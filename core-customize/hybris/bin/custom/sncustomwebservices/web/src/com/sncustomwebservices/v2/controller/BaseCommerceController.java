/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.voucher.VoucherFacade;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.security.BruteForceAttackHandler;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartAddressException;
import de.hybris.platform.converters.ConfigurablePopulator;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.validators.EnumValueValidator;
import com.sncustomwebservices.exceptions.InvalidPaymentInfoException;
import com.sncustomwebservices.exceptions.NoCheckoutCartException;
import com.sncustomwebservices.exceptions.UnsupportedDeliveryModeException;
import com.sncustomwebservices.populator.options.PaymentInfoOption;
import com.sncustomwebservices.validation.data.CartVoucherValidationData;
import com.sncustomwebservices.validation.data.CartVoucherValidationDataList;
import com.sncustomwebservices.validator.CartVoucherValidator;
import com.sncustomwebservices.validator.PlaceOrderCartValidator;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static com.sncustomwebservices.constants.YcommercewebservicesConstants.ENUM_VALUES_SEPARATOR;


public class BaseCommerceController extends BaseController
{
	protected static final String API_COMPATIBILITY_B2C_CHANNELS = "api.compatibility.b2c.channels";
	protected static final String ENTRY = "entry";
	private static final Logger LOG = LoggerFactory.getLogger(BaseCommerceController.class);

	@Resource(name = "commerceWebServicesCartFacade2")
	private CartFacade cartFacade;
	@Resource(name = "checkoutFacade")
	private CheckoutFacade checkoutFacade;
	@Resource(name = "voucherFacade")
	private VoucherFacade voucherFacade;
	@Resource(name = "deliveryAddressValidator")
	private Validator deliveryAddressValidator;
	@Resource(name = "httpRequestAddressDataPopulator")
	private Populator<HttpServletRequest, AddressData> httpRequestAddressDataPopulator;
	@Resource(name = "addressValidator")
	private Validator addressValidator;
	@Resource(name = "addressDTOValidator")
	private Validator addressDTOValidator;
	@Resource(name = "userFacade")
	private UserFacade userFacade;
	@Resource(name = "ccPaymentInfoValidator")
	private Validator ccPaymentInfoValidator;
	@Resource(name = "paymentDetailsDTOValidator")
	private Validator paymentDetailsDTOValidator;
	@Resource(name = "httpRequestPaymentInfoPopulator")
	private ConfigurablePopulator<HttpServletRequest, CCPaymentInfoData, PaymentInfoOption> httpRequestPaymentInfoPopulator;
	@Resource(name = "placeOrderCartValidator")
	private PlaceOrderCartValidator placeOrderCartValidator;
	@Resource(name = "orderStatusValueValidator")
	private EnumValueValidator orderStatusValueValidator;
	@Resource(name = "cartVoucherValidator")
	private CartVoucherValidator cartVoucherValidator;
	@Resource(name = "bruteForceAttackHandler")
	private BruteForceAttackHandler bruteForceAttackHandler;

	protected AddressData createAddressInternal(final HttpServletRequest request)
	{
		final AddressData addressData = new AddressData();
		httpRequestAddressDataPopulator.populate(request, addressData);

		validate(addressData, "addressData", addressValidator);

		return createAddressInternal(addressData);
	}

	protected AddressData createAddressInternal(final AddressData addressData)
	{
		addressData.setShippingAddress(true);
		addressData.setVisibleInAddressBook(true);
		userFacade.addAddress(addressData);
		if (addressData.isDefaultAddress())
		{
			userFacade.setDefaultAddress(addressData);
		}
		return addressData;
	}

	protected CartData setCartDeliveryAddressInternal(final String addressId)
	{
		LOG.debug("setCartDeliveryAddressInternal: {}", logParam("addressId", addressId));
		final AddressData address = new AddressData();
		address.setId(addressId);
		final Errors errors = new BeanPropertyBindingResult(address, "addressData");
		deliveryAddressValidator.validate(address, errors);
		if (errors.hasErrors())
		{
			throw new CartAddressException("Address given by id " + sanitize(addressId) + " is not valid",
					CartAddressException.NOT_VALID, addressId);
		}
		if (checkoutFacade.setDeliveryAddress(address))
		{
			return getSessionCart();
		}
		throw new CartAddressException(
				"Address given by id " + sanitize(addressId) + " cannot be set as delivery address in this cart",
				CartAddressException.CANNOT_SET, addressId);
	}

	protected CartData setCartDeliveryModeInternal(final String deliveryModeId) throws UnsupportedDeliveryModeException
	{
		LOG.debug("setCartDeliveryModeInternal: {}", logParam("deliveryModeId", deliveryModeId));
		if (checkoutFacade.setDeliveryMode(deliveryModeId))
		{
			return getSessionCart();
		}
		throw new UnsupportedDeliveryModeException(deliveryModeId);
	}

	protected CartData applyVoucherForCartInternal(final String voucherId)
			throws NoCheckoutCartException, VoucherOperationException
	{
		LOG.debug("apply voucher: {}", logParam("voucherId", voucherId));
		if (!checkoutFacade.hasCheckoutCart())
		{
			throw new NoCheckoutCartException("Cannot apply voucher. There was no checkout cart created yet!");
		}

		voucherFacade.applyVoucher(voucherId);
		return getSessionCart();
	}

	protected CartData applyVoucherForCartInternal(final String voucherId, final HttpServletRequest request)
			throws NoCheckoutCartException, VoucherOperationException
	{
		final String ipAddress = request.getRemoteAddr();
		if (bruteForceAttackHandler.registerAttempt(ipAddress + "_voucher"))
		{
			throw new VoucherOperationException("You have entered too many voucher codes. Please try again later.");
		}

		return applyVoucherForCartInternal(voucherId);
	}

	protected CartData addPaymentDetailsInternal(final CCPaymentInfoData paymentInfoData) throws InvalidPaymentInfoException
	{
		final boolean emptySavedPaymentInfos = userFacade.getCCPaymentInfos(true).isEmpty();
		final CCPaymentInfoData createdPaymentInfoData = checkoutFacade.createPaymentSubscription(paymentInfoData);

		if (createdPaymentInfoData == null)
		{
			throw new InvalidPaymentInfoException("null");
		}

		if (createdPaymentInfoData.isSaved() && (paymentInfoData.isDefaultPaymentInfo() || emptySavedPaymentInfos))
		{
			userFacade.setDefaultPaymentInfo(createdPaymentInfoData);
		}

		if (checkoutFacade.setPaymentDetails(createdPaymentInfoData.getId()))
		{
			return getSessionCart();
		}
		throw new InvalidPaymentInfoException(createdPaymentInfoData.getId());
	}

	protected CartData setPaymentDetailsInternal(final String paymentDetailsId) throws InvalidPaymentInfoException
	{
		LOG.debug("setPaymentDetailsInternal: {}", logParam("paymentDetailsId", paymentDetailsId));
		if (checkoutFacade.setPaymentDetails(paymentDetailsId))
		{
			return getSessionCart();
		}
		throw new InvalidPaymentInfoException(paymentDetailsId);
	}

	protected void validateCartForPlaceOrder() throws NoCheckoutCartException, InvalidCartException
	{
		if (!checkoutFacade.hasCheckoutCart())
		{
			throw new NoCheckoutCartException("Cannot place order. There was no checkout cart created yet!");
		}

		final CartData cartData = getSessionCart();

		final List<CartVoucherValidationData> validateDataList = cartVoucherValidator.validate(cartData.getAppliedVouchers());
		if (CollectionUtils.isNotEmpty(validateDataList))
		{
			final CartVoucherValidationDataList cartVoucherValidationDataList = new CartVoucherValidationDataList();
			cartVoucherValidationDataList.setCartVoucherValidationDataList(validateDataList);
			throw new WebserviceValidationException(cartVoucherValidationDataList);
		}

		final Errors errors = new BeanPropertyBindingResult(cartData, "sessionCart");
		placeOrderCartValidator.validate(cartData, errors);
		if (errors.hasErrors())
		{
			throw new WebserviceValidationException(errors);
		}

		try
		{
			final List<CartModificationData> modificationList = cartFacade.validateCartData();
			if (modificationList != null && !modificationList.isEmpty())
			{
				final CartModificationDataList cartModificationDataList = new CartModificationDataList();
				cartModificationDataList.setCartModificationList(modificationList);
				throw new WebserviceValidationException(cartModificationDataList);
			}
		}
		catch (final CommerceCartModificationException e)
		{
			throw new InvalidCartException(e);
		}
	}

	protected CartData getSessionCart()
	{
		return cartFacade.getSessionCart();
	}

	/**
	 * Checks if given statuses are valid
	 *
	 * @param statuses
	 */
	protected void validateStatusesEnumValue(final String statuses)
	{
		if (statuses == null)
		{
			return;
		}

		final String[] statusesStrings = statuses.split(ENUM_VALUES_SEPARATOR);
		validate(statusesStrings, "", orderStatusValueValidator);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected CartFacade getCartFacade()
	{
		return cartFacade;
	}

	protected void setCartFacade(final CartFacade cartFacade)
	{
		this.cartFacade = cartFacade;
	}

	protected CheckoutFacade getCheckoutFacade()
	{
		return checkoutFacade;
	}

	protected void setCheckoutFacade(final CheckoutFacade checkoutFacade)
	{
		this.checkoutFacade = checkoutFacade;
	}

	protected VoucherFacade getVoucherFacade()
	{
		return voucherFacade;
	}

	protected void setVoucherFacade(final VoucherFacade voucherFacade)
	{
		this.voucherFacade = voucherFacade;
	}

	protected Validator getDeliveryAddressValidator()
	{
		return deliveryAddressValidator;
	}

	protected void setDeliveryAddressValidator(final Validator deliveryAddressValidator)
	{
		this.deliveryAddressValidator = deliveryAddressValidator;
	}

	protected Populator<HttpServletRequest, AddressData> getHttpRequestAddressDataPopulator()
	{
		return httpRequestAddressDataPopulator;
	}

	protected void setHttpRequestAddressDataPopulator(
			final Populator<HttpServletRequest, AddressData> httpRequestAddressDataPopulator)
	{
		this.httpRequestAddressDataPopulator = httpRequestAddressDataPopulator;
	}

	protected Validator getAddressValidator()
	{
		return addressValidator;
	}

	protected void setAddressValidator(final Validator addressValidator)
	{
		this.addressValidator = addressValidator;
	}

	protected Validator getAddressDTOValidator()
	{
		return addressDTOValidator;
	}

	protected void setAddressDTOValidator(final Validator addressDTOValidator)
	{
		this.addressDTOValidator = addressDTOValidator;
	}

	protected UserFacade getUserFacade()
	{
		return userFacade;
	}

	protected void setUserFacade(final UserFacade userFacade)
	{
		this.userFacade = userFacade;
	}

	protected Validator getCcPaymentInfoValidator()
	{
		return ccPaymentInfoValidator;
	}

	protected void setCcPaymentInfoValidator(final Validator ccPaymentInfoValidator)
	{
		this.ccPaymentInfoValidator = ccPaymentInfoValidator;
	}

	protected Validator getPaymentDetailsDTOValidator()
	{
		return paymentDetailsDTOValidator;
	}

	protected void setPaymentDetailsDTOValidator(final Validator paymentDetailsDTOValidator)
	{
		this.paymentDetailsDTOValidator = paymentDetailsDTOValidator;
	}

	protected ConfigurablePopulator<HttpServletRequest, CCPaymentInfoData, PaymentInfoOption> getHttpRequestPaymentInfoPopulator()
	{
		return httpRequestPaymentInfoPopulator;
	}

	protected void setHttpRequestPaymentInfoPopulator(
			final ConfigurablePopulator<HttpServletRequest, CCPaymentInfoData, PaymentInfoOption> httpRequestPaymentInfoPopulator)
	{
		this.httpRequestPaymentInfoPopulator = httpRequestPaymentInfoPopulator;
	}

	protected CartVoucherValidator getCartVoucherValidator()
	{
		return cartVoucherValidator;
	}
}
