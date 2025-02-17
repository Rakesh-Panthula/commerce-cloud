/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.storelocator.data.PointOfServiceData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartEntryException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.order.data.OrderEntryDataList;
import com.sncustomwebservices.validator.StockPOSValidator;
import com.sncustomwebservices.validator.StockValidator;

import javax.annotation.Resource;

import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Tag(name = "Cart Entries")
public class CartEntriesController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(CartEntriesController.class);

	private static final long DEFAULT_PRODUCT_QUANTITY = 1;

	@Resource(name = "orderEntryCreateValidator")
	private Validator orderEntryCreateValidator;
	@Resource(name = "orderEntryUpdateValidator")
	private Validator orderEntryUpdateValidator;
	@Resource(name = "orderEntryReplaceValidator")
	private Validator orderEntryReplaceValidator;
	@Resource(name = "stockValidator")
	private StockValidator stockValidator;
	@Resource(name = "stockPOSValidator")
	private StockPOSValidator stockPOSValidator;

	protected static CartModificationData mergeCartModificationData(final CartModificationData cmd1,
			final CartModificationData cmd2)
	{
		if ((cmd1 == null) && (cmd2 == null))
		{
			return new CartModificationData();
		}
		if (cmd1 == null)
		{
			return cmd2;
		}
		if (cmd2 == null)
		{
			return cmd1;
		}
		final CartModificationData cmd = new CartModificationData();
		cmd.setDeliveryModeChanged(
				Boolean.TRUE.equals(cmd1.getDeliveryModeChanged()) || Boolean.TRUE.equals(cmd2.getDeliveryModeChanged()));
		cmd.setEntry(cmd2.getEntry());
		cmd.setQuantity(cmd2.getQuantity());
		cmd.setQuantityAdded(cmd1.getQuantityAdded() + cmd2.getQuantityAdded());
		cmd.setStatusCode(cmd2.getStatusCode());
		return cmd;
	}

	protected static OrderEntryData getCartEntryForNumber(final CartData cart, final long number)
	{
		final Integer requestedEntryNumber = (int) number;
		return CollectionUtils.emptyIfNull(cart.getEntries()).stream()
				.filter(entry -> entry != null && requestedEntryNumber.equals(entry.getEntryNumber())).findFirst()
				.orElseThrow(() -> new CartEntryException("Entry not found", CartEntryException.NOT_FOUND, String.valueOf(number)));
	}

	protected static OrderEntryData getCartEntry(final CartData cart, final String productCode, final String pickupStore)
	{
		final Predicate<OrderEntryData> productsEqualFilter = orderEntryData -> orderEntryData != null
				&& orderEntryData.getProduct() != null && orderEntryData.getProduct().getCode() != null //
				&& orderEntryData.getProduct().getCode().equals(productCode);

		final Predicate<OrderEntryData> noStoresFilter = orderEntryData -> pickupStore == null
				&& orderEntryData.getDeliveryPointOfService() == null;

		final Predicate<OrderEntryData> storesEqualFilter = orderEntryData -> pickupStore != null
				&& orderEntryData.getDeliveryPointOfService() != null && pickupStore.equals(
				orderEntryData.getDeliveryPointOfService().getName());

		return cart.getEntries().stream() //
				.filter(productsEqualFilter.and(noStoresFilter.or(storesEqualFilter))).findFirst() //
				.orElse(null);
	}

	protected static void validateForAmbiguousPositions(final CartData currentCart, final OrderEntryData currentEntry,
			final String newPickupStore)
	{
		final OrderEntryData entryToBeModified = getCartEntry(currentCart, currentEntry.getProduct().getCode(), newPickupStore);
		if (entryToBeModified != null && !entryToBeModified.getEntryNumber().equals(currentEntry.getEntryNumber()))
		{
			throw new CartEntryException("Ambiguous cart entries! Entry number " + currentEntry.getEntryNumber()
					+ " after change would be the same as entry " + entryToBeModified.getEntryNumber(),
					CartEntryException.AMBIGIOUS_ENTRY, entryToBeModified.getEntryNumber().toString());
		}
	}

	protected static void validateProductCode(final OrderEntryData originalEntry, final OrderEntryWsDTO entry)
	{
		final String productCode = originalEntry.getProduct().getCode();
		final Errors errors = new BeanPropertyBindingResult(entry, ENTRY);
		if (entry.getProduct() != null && entry.getProduct().getCode() != null && !entry.getProduct().getCode().equals(productCode))
		{
			errors.reject("cartEntry.productCodeNotMatch");
			throw new WebserviceValidationException(errors);
		}
	}

	@GetMapping(value = "/{cartId}/entries")
	@ResponseBody
	@Operation(operationId = "getCartEntries", summary = "Get cart entries.", description = "Returns cart entries.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public OrderEntryListWsDTO getCartEntries(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("getCartEntries");
		final OrderEntryDataList dataList = new OrderEntryDataList();
		dataList.setOrderEntries(getSessionCart().getEntries());
		return getDataMapper().map(dataList, OrderEntryListWsDTO.class, fields);
	}

	@PostMapping(value = "/{cartId}/entries", consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@Operation(operationId = "createCartEntry", summary = "Adds a product to the cart.", description = "Adds a product to the specified cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationWsDTO createCartEntry(@PathVariable final String baseSiteId,
			@Parameter(description = "Request body parameter that contains details such as the product code (product.code), the quantity of product (quantity), and the pickup store name (deliveryPointOfService.name).\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final OrderEntryWsDTO entry,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws CommerceCartModificationException
	{
		if (entry.getQuantity() == null)
		{
			entry.setQuantity(DEFAULT_PRODUCT_QUANTITY);
		}

		validate(entry, ENTRY, orderEntryCreateValidator);

		final String pickupStore = entry.getDeliveryPointOfService() == null ? null : entry.getDeliveryPointOfService().getName();
		return addCartEntryInternal(baseSiteId, entry.getProduct().getCode(), entry.getQuantity(), pickupStore, fields);
	}

	@GetMapping(value = "/{cartId}/entries/{entryNumber}")
	@ResponseBody
	@Operation(operationId = "getCartEntry", summary = "Get the details of the cart entries.", description = "Returns the details of the cart entries.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public OrderEntryWsDTO getCartEntry(
			@Parameter(description = "The entry number. Each entry in a cart has an entry number. Cart entries are numbered in ascending order, starting with zero (0).", required = true) @PathVariable final long entryNumber,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("getCartEntry: entryNumber = {}", entryNumber);
		final OrderEntryData orderEntry = getCartEntryForNumber(getSessionCart(), entryNumber);
		return getDataMapper().map(orderEntry, OrderEntryWsDTO.class, fields);
	}

	@PutMapping(value = "/{cartId}/entries/{entryNumber}", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@Operation(operationId = "replaceCartEntry", summary = "Set quantity and store details of a cart entry.", description =
			"Updates the quantity of a single cart entry and the details of the store where the cart entry will be picked up. "
					+ "Attributes not provided in request will be defined again (set to null or default)")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationWsDTO replaceCartEntry(@PathVariable final String baseSiteId,
			@Parameter(description = "The entry number. Each entry in a cart has an entry number. Cart entries are numbered in ascending order, starting with zero (0).", required = true) @PathVariable final long entryNumber,
			@Parameter(description = "Request body parameter that contains details such as the quantity of product (quantity), and the pickup store name (deliveryPointOfService.name)\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final OrderEntryWsDTO entry,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws CommerceCartModificationException
	{
		final CartData cart = getSessionCart();
		final OrderEntryData orderEntry = getCartEntryForNumber(cart, entryNumber);
		final String pickupStore = entry.getDeliveryPointOfService() == null ? null : entry.getDeliveryPointOfService().getName();

		validateProductCode(orderEntry, entry);
		validate(entry, ENTRY, orderEntryReplaceValidator);

		return updateCartEntryInternal(baseSiteId, cart, orderEntry, entry.getQuantity(), pickupStore, fields, true);
	}

	@PatchMapping(value = "/{cartId}/entries/{entryNumber}", consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseBody
	@Operation(operationId = "updateCartEntry", summary = "Update quantity and store details of a cart entry.", description = "Updates the quantity of a single cart entry and the details of the store where the cart entry will be picked up.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationWsDTO updateCartEntry(@PathVariable final String baseSiteId,
			@Parameter(description = "The entry number. Each entry in a cart has an entry number. Cart entries are numbered in ascending order, starting with zero (0).", required = true) @PathVariable final long entryNumber,
			@Parameter(description = "Request body parameter that contains details such as the quantity of product (quantity), and the pickup store name (deliveryPointOfService.name)\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final OrderEntryWsDTO entry,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws CommerceCartModificationException
	{
		final CartData cart = getSessionCart();
		final OrderEntryData orderEntry = getCartEntryForNumber(cart, entryNumber);

		validateProductCode(orderEntry, entry);

		if (entry.getQuantity() == null)
		{
			entry.setQuantity(orderEntry.getQuantity());
		}

		validate(entry, ENTRY, orderEntryUpdateValidator);

		final String pickupStore = entry.getDeliveryPointOfService() == null ? null : entry.getDeliveryPointOfService().getName();
		return updateCartEntryInternal(baseSiteId, cart, orderEntry, entry.getQuantity(), pickupStore, fields, false);
	}

	@DeleteMapping(value = "/{cartId}/entries/{entryNumber}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeCartEntry", summary = "Deletes cart entry.", description = "Deletes cart entry by entry number.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCartEntry(
			@Parameter(description = "The entry number. Each entry in a cart has an entry number. Cart entries are numbered in ascending order, starting with zero (0).", required = true) @PathVariable final long entryNumber)
			throws CommerceCartModificationException
	{
		LOG.debug("removeCartEntry: entryNumber = {}", entryNumber);
		final CartData cart = getSessionCart();
		getCartEntryForNumber(cart, entryNumber);
		getCartFacade().updateCartEntry(entryNumber, 0);
	}

	protected CartModificationWsDTO addCartEntryInternal(final String baseSiteId, final String code, final long qty,
			final String pickupStore, final String fields) throws CommerceCartModificationException
	{
		final CartModificationData cartModificationData;
		if (StringUtils.isNotEmpty(pickupStore))
		{
			stockPOSValidator.validate(baseSiteId, code, pickupStore, null);
			cartModificationData = getCartFacade().addToCart(code, qty, pickupStore);
		}
		else
		{
			stockValidator.validate(baseSiteId, code, null);
			cartModificationData = getCartFacade().addToCart(code, qty);
		}
		return getDataMapper().map(cartModificationData, CartModificationWsDTO.class, fields);
	}

	protected CartModificationWsDTO updateCartEntryInternal(final String baseSiteId, final CartData cart,
			final OrderEntryData orderEntry, final Long qty, final String pickupStore, final String fields, final boolean putMode)
			throws CommerceCartModificationException
	{
		final long entryNumber = orderEntry.getEntryNumber().longValue();
		final String productCode = orderEntry.getProduct().getCode();
		final PointOfServiceData currentPointOfService = orderEntry.getDeliveryPointOfService();

		CartModificationData cartModificationData1 = null;
		CartModificationData cartModificationData2 = null;

		if (!StringUtils.isEmpty(pickupStore))
		{
			if (currentPointOfService == null || !currentPointOfService.getName().equals(pickupStore))
			{
				//was 'shipping mode' or store is changed
				validateForAmbiguousPositions(cart, orderEntry, pickupStore);
				stockPOSValidator.validate(baseSiteId, productCode, pickupStore, entryNumber);
				cartModificationData1 = getCartFacade().updateCartEntry(entryNumber, pickupStore);
			}
		}
		else if (putMode && currentPointOfService != null)
		{
			//was 'pickup in store', now switch to 'shipping mode'
			validateForAmbiguousPositions(cart, orderEntry, pickupStore);
			stockValidator.validate(baseSiteId, productCode, entryNumber);
			cartModificationData1 = getCartFacade().updateCartEntry(entryNumber, pickupStore);
		}

		if (qty != null)
		{
			cartModificationData2 = getCartFacade().updateCartEntry(entryNumber, qty);
		}

		return getDataMapper().map(mergeCartModificationData(cartModificationData1, cartModificationData2),
				CartModificationWsDTO.class, fields);
	}
}
