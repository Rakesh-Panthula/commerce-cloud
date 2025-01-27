/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.CheckoutPaymentFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import javax.annotation.Resource;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Cart Addresses")
public class CartAddressesController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(CartAddressesController.class);

	private static final String ADDRESS_MAPPING = "firstName,lastName,titleCode,phone,cellphone,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress";

	private static final String OBJECT_NAME_ADDRESS = "address";

	@Resource(name = "sapCheckoutPaymentFacade")
	private CheckoutPaymentFacade checkoutPaymentFacade;

	@Resource(name = "sapBillingAddressValidator")
	private Validator billingAddressValidator;

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_GUEST", "ROLE_TRUSTED_CLIENT" })
	@PostMapping(value = "/{cartId}/addresses/delivery", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "createCartDeliveryAddress", summary = "Creates a delivery address for the cart.", description = "")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public AddressWsDTO createCartDeliveryAddress(@Parameter(description =
			"Request body parameter that contains details such as the customer's first name (firstName), the customer's last name (lastName), the customer's title (titleCode), the customer's phone (phone), "
					+ "the country (country.isocode), the first part of the address (line1), the second part of the address (line2), the town (town), the postal code (postalCode), and the region (region.isocode).\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final AddressWsDTO address,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("createCartDeliveryAddress");
		validate(address, OBJECT_NAME_ADDRESS, getAddressDTOValidator());
		AddressData addressData = getDataMapper().map(address, AddressData.class, ADDRESS_MAPPING);
		addressData = createAddressInternal(addressData);
		setCartDeliveryAddressInternal(addressData.getId());
		return getDataMapper().map(addressData, AddressWsDTO.class, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/addresses/delivery")
	@ResponseStatus(HttpStatus.OK)
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@Operation(operationId = "replaceCartDeliveryAddress", summary = "Updates the delivery address for a cart.", description = "The updated address country must be a valid delivery country for the current base store.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void replaceCartDeliveryAddress(
			@Parameter(description = "Address identifier", required = true) @RequestParam final String addressId)
	{
		setCartDeliveryAddressInternal(addressId);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@DeleteMapping(value = "/{cartId}/addresses/delivery")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeCartDeliveryAddress", summary = "Deletes the delivery address.", description = "Deletes the delivery address associated with a cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCartDeliveryAddress()
	{
		LOG.debug("removeCartDeliveryAddress");
		if (!getCheckoutFacade().removeDeliveryAddress())
		{
			throw new CartException("Cannot reset address!", CartException.CANNOT_RESET_ADDRESS);
		}
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/addresses/billing", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "updateBillingAddress", summary = "Create or update the billing address of the cart.", description = "Creates or updates the billing address of the cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void updateBillingAddress(
			@Parameter(description = "Address object.", required = true) @RequestBody final AddressWsDTO addressWsDTO)
	{
		this.validateBillingAddress(addressWsDTO);
		final AddressData addressData = getDataMapper().map(addressWsDTO, AddressData.class);
		checkoutPaymentFacade.setPaymentAddress(addressData);
	}

	protected void validateBillingAddress(final AddressWsDTO address)
	{
		final Errors errors = new BeanPropertyBindingResult(address, "billingAddress");
		billingAddressValidator.validate(address, errors);
		if (errors.hasErrors())
		{
			throw new WebserviceValidationException(errors);
		}
	}
}
