/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.data.AddToCartParams;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationStatus;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartEntryGroupException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.validator.StockValidator;

import javax.annotation.Resource;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@Tag(name = "Cart Entry Groups")
public class CartEntryGroupsController extends BaseCommerceController
{
	private static final long DEFAULT_PRODUCT_QUANTITY = 1;

	@Resource(name = "greaterThanZeroValidator")
	private Validator greaterThanZeroValidator;
	@Resource(name = "addToCartEntryGroupValidator")
	private Validator addToCartEntryGroupValidator;
	@Resource(name = "stockValidator")
	private StockValidator stockValidator;

	@PostMapping(value = "/{cartId}/entrygroups/{entryGroupNumber}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Operation(operationId = "addToCartEntryGroup", summary = "Add a product to a cart entry group.", description = "Adds a product to a cart entry group.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationWsDTO addToCartEntryGroup(@PathVariable final String baseSiteId,
			@Parameter(description = "Each entry group in a cart has a specific entry group number. Entry group numbers are integers starting at one. They are defined in ascending order.", required = true) @PathVariable final Integer entryGroupNumber,
			@Parameter(description = "Request body parameter that contains details such as the product code (product.code) and the quantity of product (quantity).", required = true) @RequestBody final OrderEntryWsDTO entry,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws CommerceCartModificationException
	{
		if (entry.getQuantity() == null)
		{
			entry.setQuantity(DEFAULT_PRODUCT_QUANTITY);
		}

		validate(entry, ENTRY, addToCartEntryGroupValidator);
		validate(entryGroupNumber, "entryGroupNumber", greaterThanZeroValidator);
		stockValidator.validate(baseSiteId, entry.getProduct().getCode(), null);

		final AddToCartParams params = new AddToCartParams();
		params.setStoreId(baseSiteId);
		params.setProductCode(entry.getProduct().getCode());
		params.setQuantity(entry.getQuantity());
		params.setEntryGroupNumbers(Set.of(entryGroupNumber));
		final CartModificationData cartModificationData = getCartFacade().addToCart(params);

		return getDataMapper().map(cartModificationData, CartModificationWsDTO.class, fields);
	}

	@DeleteMapping(value = "/{cartId}/entrygroups/{entryGroupNumber}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeCartEntryGroup", summary = "Delete an entry group.", description = "Removes an entry group from an associated cart. The entry group is identified by an entryGroupNumber. The cart is identified by the cartId.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeEntryGroup(
			@Parameter(description = "Each entry group in a cart has a specific entry group number. Entry group numbers are integers starting at one. They are defined in ascending order.", required = true) @PathVariable final int entryGroupNumber)
			throws CommerceCartModificationException
	{
		validate(entryGroupNumber, "entryGroupNumber", greaterThanZeroValidator);

		final CartModificationData result = getCartFacade().removeEntryGroup(entryGroupNumber);

		if (StringUtils.equals(CommerceCartModificationStatus.INVALID_ENTRY_GROUP_NUMBER, result.getStatusCode()))
		{
			throw new CartEntryGroupException("Entry group not found", CartEntryGroupException.NOT_FOUND,
					String.valueOf(entryGroupNumber));
		}
	}
}
