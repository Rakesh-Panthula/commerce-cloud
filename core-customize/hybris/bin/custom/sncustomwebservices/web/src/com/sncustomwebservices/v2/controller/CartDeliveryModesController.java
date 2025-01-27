/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.data.DeliveryModesData;
import de.hybris.platform.commercewebservicescommons.dto.order.DeliveryModeListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.DeliveryModeWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.exceptions.UnsupportedDeliveryModeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@Tag(name = "Cart Delivery Modes")
public class CartDeliveryModesController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(CartDeliveryModesController.class);

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@GetMapping(value = "/{cartId}/deliverymode")
	@ResponseBody
	@Operation(operationId = "getCartDeliveryMode", summary = "Retrieves the delivery mode for the cart.", description = "Retrieves information about the delivery mode selected for the cart, including the delivery mode code, name, cost, and so on.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public DeliveryModeWsDTO getCartDeliveryMode(
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("getCartDeliveryMode");
		return getDataMapper().map(getSessionCart().getDeliveryMode(), DeliveryModeWsDTO.class, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/deliverymode")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "replaceCartDeliveryMode", summary = "Updates the delivery mode for the cart.", description = "Updates the delivery mode details for the cart based on the specified delivery mode identifier.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void replaceCartDeliveryMode(
			@Parameter(description = "Delivery mode identifier (code)", required = true) @RequestParam(required = true) final String deliveryModeId)
			throws UnsupportedDeliveryModeException
	{
		setCartDeliveryModeInternal(deliveryModeId);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@DeleteMapping(value = "/{cartId}/deliverymode")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeCartDeliveryMode", summary = "Deletes the delivery mode from the cart.", description = "")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCartDeliveryMode()
	{
		LOG.debug("removeCartDeliveryMode");
		if (!getCheckoutFacade().removeDeliveryMode())
		{
			throw new CartException("Cannot reset delivery mode!", CartException.CANNOT_RESET_DELIVERYMODE);
		}
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@GetMapping(value = "/{cartId}/deliverymodes")
	@ResponseBody
	@Operation(operationId = "getCartDeliveryModes", summary = "Retrieves the delivery modes for the current store and delivery address.", description =
			"Retrieves the delivery modes defined for the current base store and the cart delivery address. If the cart does not have a delivery address, an empty list will be returned.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public DeliveryModeListWsDTO getCartDeliveryModes(
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("getCartDeliveryModes");
		final DeliveryModesData deliveryModesData = new DeliveryModesData();
		deliveryModesData.setDeliveryModes(getCheckoutFacade().getSupportedDeliveryModes());

		return getDataMapper().map(deliveryModesData, DeliveryModeListWsDTO.class, fields);
	}
}
