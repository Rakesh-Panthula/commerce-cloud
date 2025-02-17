/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.exceptions.InvalidPaymentInfoException;
import com.sncustomwebservices.exceptions.NoCheckoutCartException;
import com.sncustomwebservices.exceptions.UnsupportedRequestException;
import com.sncustomwebservices.request.support.impl.PaymentProviderRequestSupportedStrategy;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Cart Payments")
public class CartPaymentsController extends BaseCommerceController
{
	private static final String PAYMENT_MAPPING = "accountHolderName,cardNumber,cardType,cardTypeData(code),expiryMonth,expiryYear,issueNumber,startMonth,startYear,subscriptionId,defaultPaymentInfo,saved,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress,phone)";

	@Resource(name = "paymentProviderRequestSupportedStrategy")
	private PaymentProviderRequestSupportedStrategy paymentProviderRequestSupportedStrategy;

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PostMapping(value = "/{cartId}/paymentdetails", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "createCartPaymentDetails", summary = "Defines and assigns details of a new credit card payment to the cart.", description = "Defines the details of a new credit card, and assigns this payment option to the cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public PaymentDetailsWsDTO createCartPaymentDetails(@Parameter(description =
			"Request body parameter that contains details such as the name on the card (accountHolderName), the card number (cardNumber), the card type (cardType.code), "
					+ "the month of the expiry date (expiryMonth), the year of the expiry date (expiryYear), whether the payment details should be saved (saved), whether the payment details "
					+ "should be set as default (defaultPaymentInfo), and the billing address (billingAddress.firstName, billingAddress.lastName, billingAddress.titleCode, billingAddress.country.isocode, "
					+ "billingAddress.line1, billingAddress.line2, billingAddress.town, billingAddress.postalCode, billingAddress.region.isocode)\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final PaymentDetailsWsDTO paymentDetails,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws InvalidPaymentInfoException, NoCheckoutCartException, UnsupportedRequestException
	{
		paymentProviderRequestSupportedStrategy.checkIfRequestSupported("addPaymentDetails");
		validatePayment(paymentDetails);
		CCPaymentInfoData paymentInfoData = getDataMapper().map(paymentDetails, CCPaymentInfoData.class, PAYMENT_MAPPING);
		paymentInfoData = addPaymentDetailsInternal(paymentInfoData).getPaymentInfo();
		return getDataMapper().map(paymentInfoData, PaymentDetailsWsDTO.class, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/paymentdetails")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "replaceCartPaymentDetails", summary = "Sets credit card payment details for the cart.", description = "Sets credit card payment details for the specified cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void replaceCartPaymentDetails(
			@Parameter(description = "Payment details identifier.", required = true) @RequestParam final String paymentDetailsId)
			throws InvalidPaymentInfoException
	{
		setPaymentDetailsInternal(paymentDetailsId);
	}

	protected void validatePayment(final PaymentDetailsWsDTO paymentDetails) throws NoCheckoutCartException
	{
		if (!getCheckoutFacade().hasCheckoutCart())
		{
			throw new NoCheckoutCartException("Cannot add PaymentInfo. There was no checkout cart created yet!");
		}
		validate(paymentDetails, "paymentDetails", getPaymentDetailsDTOValidator());
	}
}
