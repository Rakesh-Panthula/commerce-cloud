/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.SaveCartFacade;
import de.hybris.platform.commercefacades.order.data.CommerceSaveCartParameterData;
import de.hybris.platform.commercefacades.order.data.CommerceSaveCartResultData;
import de.hybris.platform.commerceservices.order.CommerceSaveCartException;
import de.hybris.platform.commercewebservicescommons.dto.order.SaveCartResultWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Controller for saved cart related requests such as saving a cart or retrieving/restoring/... a saved cart
 */
@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@Tag(name = "Save Cart")
public class SaveCartController extends BaseCommerceController
{
	@Resource(name = "saveCartFacade")
	private SaveCartFacade saveCartFacade;

	@RequestMapping(value = "/{cartId}/save", method = RequestMethod.PATCH)
	@ResponseBody
	@Operation(operationId = "doSaveCart", summary = "Explicitly saves a cart.", description = "Explicitly saves a cart and sets the name and description for the saved cart if given.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doSaveCart(
			@Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @PathVariable final String cartId,
			@Parameter(description = "The name that should be applied to the saved cart.") @RequestParam(value = "saveCartName", required = false) final String saveCartName,
			@Parameter(description = "The description that should be applied to the saved cart.") @RequestParam(value = "saveCartDescription", required = false) final String saveCartDescription,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);
		parameters.setName(saveCartName);
		parameters.setDescription(saveCartDescription);

		final CommerceSaveCartResultData result = saveCartFacade.saveCart(parameters);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@RequestMapping(value = "/{cartId}/restoresavedcart", method = RequestMethod.PATCH)
	@ResponseBody
	@Operation(operationId = "doUpdateSavedCart", summary = "Restore a saved cart.", description = "Restore the data of a saved cart.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doUpdateSavedCart(
			@Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);
		parameters.setEnableHooks(true);
		saveCartFacade.restoreSavedCart(parameters);

		final CommerceSaveCartResultData result = new CommerceSaveCartResultData();
		result.setSavedCartData(getSessionCart());
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@RequestMapping(value = "/{cartId}/flagForDeletion", method = RequestMethod.PATCH)
	@ResponseBody
	@Operation(operationId = "doUpdateFlagForDeletion", summary = "Flag a cart for deletion.", description =
			"Flags a cart for deletion (the cart doesn't have corresponding save cart attributes anymore). The cart is not "
					+ "actually deleted from the database. But with the removal of the saved cart attributes, this cart will be taken care of by the cart removal job just like any other cart.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doUpdateFlagForDeletion(
			@Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		final CommerceSaveCartResultData result = saveCartFacade.flagForDeletion(cartId);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@RequestMapping(value = "/{cartId}/savedcart", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getSavedCart", summary = "Get a saved cart.", description = "Returns a saved cart for an authenticated user. The cart is identified using the \"cartId\" parameter.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO getSavedCart(
			@Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);

		final CommerceSaveCartResultData result = saveCartFacade.getCartForCodeAndCurrentUser(parameters);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@RequestMapping(value = "/{cartId}/clonesavedcart", method = RequestMethod.POST)
	@ResponseBody
	@Operation(operationId = "doCartClone", summary = "Explicitly clones a cart.", description = "Explicitly clones a cart and sets the name and description for the cloned cart if given.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doCartClone(
			@Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @PathVariable final String cartId,
			@Parameter(description = "The name that should be applied to the cloned cart.") @RequestParam(value = "name", required = false) final String name,
			@Parameter(description = "The description that should be applied to the cloned cart.") @RequestParam(value = "description", required = false) final String description,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);
		parameters.setName(name);
		parameters.setDescription(description);

		final CommerceSaveCartResultData result = saveCartFacade.cloneSavedCart(parameters);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}
}
