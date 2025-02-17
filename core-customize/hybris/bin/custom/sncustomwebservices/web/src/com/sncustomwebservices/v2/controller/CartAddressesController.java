/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
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


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Cart Addresses")
public class CartAddressesController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(CartAddressesController.class);

	private static final String ADDRESS_MAPPING = "firstName,lastName,titleCode,phone,cellphone,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress";
	private static final String OBJECT_NAME_ADDRESS = "address";

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_GUEST", "ROLE_TRUSTED_CLIENT" })
	@PostMapping(value = "/{cartId}/addresses/delivery", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "createCartDeliveryAddress", summary = "Creates a delivery address for the cart.", description = "Creates an address and assigns it to the cart as the delivery address.")
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
	@Operation(operationId = "replaceCartDeliveryAddress", summary = "Sets a delivery address for the cart.", description = "Sets a delivery address for the cart. The address country must be placed among the delivery countries of the current base store.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void replaceCartDeliveryAddress(
			@Parameter(description = "Address identifier", required = true) @RequestParam final String addressId)
	{
		setCartDeliveryAddressInternal(addressId);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@DeleteMapping(value = "/{cartId}/addresses/delivery")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeCartDeliveryAddress", summary = "Deletes the delivery address from the cart.", description = "Deletes the delivery address from the specified cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCartDeliveryAddress()
	{
		LOG.debug("removeCartDeliveryAddress");
		if (!getCheckoutFacade().removeDeliveryAddress())
		{
			throw new CartException("Cannot reset address!", CartException.CANNOT_RESET_ADDRESS);
		}
	}
}
