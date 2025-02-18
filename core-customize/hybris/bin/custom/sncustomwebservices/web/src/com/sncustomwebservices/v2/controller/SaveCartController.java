/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.SaveCartFacade;
import de.hybris.platform.commercefacades.order.data.CommerceSaveCartParameterData;
import de.hybris.platform.commercefacades.order.data.CommerceSaveCartResultData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceSaveCartException;
import de.hybris.platform.commercewebservicescommons.dto.order.SaveCartResultWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.skipfield.SkipSaveCartResultFieldValueSetter;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
	@Resource(name = "skipSaveCartResultFieldValueSetter")
	private SkipSaveCartResultFieldValueSetter skipSaveCartResultFieldValueSetter;

	@RequestMapping(value = "/{cartId}/save", method = RequestMethod.PATCH)
	@ResponseBody
	@Operation(operationId = "doSaveCart", summary = "Updates a cart to save it.", description = "Updates a cart to explicitly save it. Adds the name and description of the saved cart if specified.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doSaveCart(
			@Parameter(description = "Cart identifier: cart code for logged-in user, cart GUID for anonymous user, or 'current' for the last modified cart.", required = true) @PathVariable final String cartId,
			@Parameter(description = "Name of the saved cart.") @RequestParam(value = "saveCartName", required = false) final String saveCartName,
			@Parameter(description = "Description of the saved cart.") @RequestParam(value = "saveCartDescription", required = false) final String saveCartDescription,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		skipSaveCartResultFieldValueSetter.setValue(fields);
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);
		parameters.setName(saveCartName);
		parameters.setDescription(saveCartDescription);

		final CommerceSaveCartResultData result = saveCartFacade.saveCart(parameters);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@PatchMapping(value = "/{cartId}/restoresavedcart")
	@ResponseBody
	@Operation(operationId = "doUpdateSavedCart", summary = "Restores a saved cart.", description = "Restores the data of a saved cart.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doUpdateSavedCart(
			@Parameter(description = "Cart identifier: cart code for logged-in user, cart GUID for anonymous user, or 'current' for the last modified cart.", required = true) @PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException, CommerceCartModificationException
	{
		skipSaveCartResultFieldValueSetter.setValue(fields);
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);
		parameters.setEnableHooks(true);
		saveCartFacade.restoreSavedCart(parameters);
		getCartFacade().validateCartData();

		final CommerceSaveCartResultData result = new CommerceSaveCartResultData();
		result.setSavedCartData(getSessionCart());
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@RequestMapping(value = "/{cartId}/flagForDeletion", method = RequestMethod.PATCH)
	@ResponseBody
	@Operation(operationId = "doUpdateFlagForDeletion", summary = "Updates the cart by flagging it for deletion.", description =
			"Updates the cart without corresponding saved cart attributes by flagging it for deletion. The cart is not "
					+ "deleted from the database, but without the saved cart properties, the cart will be handled by the cart removal job just like any other cart.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doUpdateFlagForDeletion(
			@Parameter(description = "Cart identifier: cart code for logged-in user, cart GUID for anonymous user, or 'current' for the last modified cart.", required = true) @PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		skipSaveCartResultFieldValueSetter.setValue(fields);
		final CommerceSaveCartResultData result = saveCartFacade.flagForDeletion(cartId);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@GetMapping(value = "/{cartId}/savedcart")
	@ResponseBody
	@Operation(operationId = "getSavedCart", summary = "Retrieves the saved cart.", description = "Retrieves the saved cart for an authenticated customer using the cart identifier.  To get entryGroup information, set fields value as follows: fields=savedCartData(entryGroups(BASIC)), fields=savedCartData(entryGroups(DEFAULT)), or fields=savedCartData(entryGroups(FULL)).")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO getSavedCart(
			@Parameter(description = "Cart identifier: cart code for logged-in user, cart GUID for anonymous user, or 'current' for the last modified cart.", required = true) @PathVariable final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		skipSaveCartResultFieldValueSetter.setValue(fields);
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);

		final CommerceSaveCartResultData result = saveCartFacade.getCartForCodeAndCurrentUser(parameters);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}

	@PostMapping(value = "/{cartId}/clonesavedcart")
	@ResponseBody
	@Operation(operationId = "doCartClone", summary = "Creates a clone of a saved cart.", description = "Creates a clone of a saved cart. Customers can provide a name and a description for the cloned cart even though they aren't mandatory parameters.")
	@ApiBaseSiteIdAndUserIdParam
	public SaveCartResultWsDTO doCartClone(
			@Parameter(description = "Cart identifier: cart code for logged-in user, cart GUID for anonymous user, or 'current' for the last modified cart.", required = true) @PathVariable final String cartId,
			@Parameter(description = "Name of the cloned cart.") @RequestParam(value = "name", required = false) final String name,
			@Parameter(description = "Description of the cloned cart.") @RequestParam(value = "description", required = false) final String description,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceSaveCartException
	{
		skipSaveCartResultFieldValueSetter.setValue(fields);
		final CommerceSaveCartParameterData parameters = new CommerceSaveCartParameterData();
		parameters.setCartId(cartId);
		parameters.setName(name);
		parameters.setDescription(description);

		final CommerceSaveCartResultData result = saveCartFacade.cloneSavedCart(parameters);
		return getDataMapper().map(result, SaveCartResultWsDTO.class, fields);
	}
}
