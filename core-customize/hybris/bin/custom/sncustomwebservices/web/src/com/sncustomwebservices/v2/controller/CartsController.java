/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.order.SaveCartFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.commerceservices.order.CommerceCartMergingException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartRestorationException;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commercewebservicescommons.dto.order.CartListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.cart.impl.CommerceWebServicesCartFacade;
import com.sncustomwebservices.order.data.CartDataList;
import com.sncustomwebservices.validation.data.CartVoucherValidationData;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@Tag(name = "Carts")
public class CartsController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(CartsController.class);

	private static final String COUPON_STATUS_CODE = "couponNotValid";
	private static final String VOUCHER_STATUS_CODE = "voucherNotValid";

	@Resource(name = "customerFacade")
	private CustomerFacade customerFacade;
	@Resource(name = "saveCartFacade")
	private SaveCartFacade saveCartFacade;

	@GetMapping
	@ResponseBody
	@Operation(operationId = "getCarts", summary = "Get all customer carts.", description = "Lists all customer carts.")
	@ApiBaseSiteIdAndUserIdParam
	public CartListWsDTO getCarts(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
			@Parameter(description = "Optional parameter. If the parameter is provided and its value is true, only saved carts are returned.") @RequestParam(defaultValue = "false") final boolean savedCartsOnly,
			@Parameter(description = "Optional pagination parameter in case of savedCartsOnly == true. Default value 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Optional {@link PaginationData} parameter in case of savedCartsOnly == true. Default value 20.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Optional sort criterion in case of savedCartsOnly == true. No default value.") @RequestParam(required = false) final String sort)
	{
		if (getUserFacade().isAnonymousUser())
		{
			throw new AccessDeniedException("Access is denied");
		}

		final CartDataList cartDataList = new CartDataList();

		final PageableData pageableData = new PageableData();
		pageableData.setCurrentPage(currentPage);
		pageableData.setPageSize(pageSize);
		pageableData.setSort(sort);
		final List<CartData> allCarts = new ArrayList<>(
				saveCartFacade.getSavedCartsForCurrentUser(pageableData, null).getResults());
		if (!savedCartsOnly)
		{
			allCarts.addAll(getCartFacade().getCartsForCurrentUser());
		}
		cartDataList.setCarts(allCarts);

		return getDataMapper().map(cartDataList, CartListWsDTO.class, fields);
	}

	@GetMapping(value = "/{cartId}")
	@ResponseBody
	@Operation(operationId = "getCart", summary = "Get a cart with a given identifier.", description = "Returns the cart with a given identifier.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartWsDTO getCart(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		// CartMatchingFilter sets current cart based on cartId, so we can return cart from the session
		return getDataMapper().map(getSessionCart(), CartWsDTO.class, fields);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "createCart", summary = "Creates or restore a cart for a user.", description = "Creates a new cart or restores an anonymous cart as a user's cart (if an old Cart Id is given in the request).")
	@ApiBaseSiteIdAndUserIdParam
	public CartWsDTO createCart(
			@Parameter(description = "Anonymous cart GUID.") @RequestParam(required = false) final String oldCartId,
			@Parameter(description = "The GUID of the user's cart that will be merged with the anonymous cart.") @RequestParam(required = false) final String toMergeCartGuid,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("createCart");
		if (StringUtils.isNotEmpty(oldCartId))
		{
			restoreAnonymousCartAndMerge(oldCartId, toMergeCartGuid);
		}
		else
		{
			restoreSavedCart(toMergeCartGuid);
		}
		return getDataMapper().map(getSessionCart(), CartWsDTO.class, fields);
	}

	protected void restoreAnonymousCartAndMerge(final String oldCartId, final String toMergeCartGuid)
	{
		if (getUserFacade().isAnonymousUser())
		{
			throw new CartException("Anonymous user is not allowed to copy cart!");
		}
		if (!isCartAnonymous(oldCartId))
		{
			throw new CartException("Cart is not anonymous", CartException.CANNOT_RESTORE, oldCartId);
		}
		if (StringUtils.isNotEmpty(toMergeCartGuid) && !isUserCart(toMergeCartGuid))
		{
			throw new CartException("Cart is not current user's cart", CartException.CANNOT_RESTORE, toMergeCartGuid);
		}

		final String evaluatedToMergeCartGuid = StringUtils.isNotEmpty(toMergeCartGuid) ?
				toMergeCartGuid :
				getSessionCart().getGuid();
		try
		{
			getCartFacade().restoreAnonymousCartAndMerge(oldCartId, evaluatedToMergeCartGuid);
		}
		catch (final CommerceCartMergingException e)
		{
			throw new CartException("Couldn't merge carts", CartException.CANNOT_MERGE, e);
		}
		catch (final CommerceCartRestorationException e)
		{
			throw new CartException("Couldn't restore cart", CartException.CANNOT_RESTORE, e);
		}
	}

	protected void restoreSavedCart(final String toMergeCartGuid)
	{
		if (StringUtils.isNotEmpty(toMergeCartGuid))
		{
			if (!isUserCart(toMergeCartGuid))
			{
				throw new CartException("Cart is not current user's cart", CartException.CANNOT_RESTORE, toMergeCartGuid);
			}
			try
			{
				getCartFacade().restoreSavedCart(toMergeCartGuid);
			}
			catch (final CommerceCartRestorationException e)
			{
				throw new CartException("Couldn't restore cart", CartException.CANNOT_RESTORE, toMergeCartGuid, e);
			}
		}
	}

	@DeleteMapping(value = "/{cartId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeCart", summary = "Deletes a cart with a given cart id.", description = "Deletes a cart with a specified cart id.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCart()
	{
		getCartFacade().removeSessionCart();
	}

	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/email")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "replaceCartGuestUser", summary = "Assigns an email to the cart.", description = "Assigns an email to the cart. This step is required to make a guest checkout.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void replaceCartGuestUser(
			@Parameter(description = "Email of the guest user. It will be used during the checkout process.", required = true) @RequestParam final String email)
			throws DuplicateUidException
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("replaceCartGuestUser: email={}", sanitize(email));
		}
		if (!EmailValidator.getInstance().isValid(email))
		{
			throw new RequestParameterException("Email [" + sanitize(email) + "] is not a valid e-mail address!",
					RequestParameterException.INVALID, "login");
		}

		customerFacade.createGuestUserForAnonymousCheckout(email, "guest");
	}

	@PostMapping(path = "/{cartId}/validate")
	@ResponseBody
	@Operation(operationId = "validateCart", summary = "Validates the cart", description = "Runs a cart validation and returns the result.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationListWsDTO validateCart(
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws CommerceCartModificationException
	{
		LOG.debug("validateCart");
		final CartModificationDataList cartModificationDataList = new CartModificationDataList();

		final List<CartVoucherValidationData> invalidVouchers = getCartVoucherValidator().validate(
				getSessionCart().getAppliedVouchers());
		// when a voucher is invalid validateCartData removes it from a cart
		final List<CartModificationData> cartValidationResults = getCartFacade().validateCartData();

		cartModificationDataList.setCartModificationList(replaceVouchersValidationResults(cartValidationResults, invalidVouchers));
		return getDataMapper().map(cartModificationDataList, CartModificationListWsDTO.class, fields);
	}

	protected boolean isUserCart(final String toMergeCartGuid)
	{
		if (getCartFacade() instanceof CommerceWebServicesCartFacade)
		{
			final CommerceWebServicesCartFacade commerceWebServicesCartFacade = (CommerceWebServicesCartFacade) getCartFacade();
			return commerceWebServicesCartFacade.isCurrentUserCart(toMergeCartGuid);
		}
		return true;
	}

	protected boolean isCartAnonymous(final String cartGuid)
	{
		if (getCartFacade() instanceof CommerceWebServicesCartFacade)
		{
			final CommerceWebServicesCartFacade commerceWebServicesCartFacade = (CommerceWebServicesCartFacade) getCartFacade();
			return commerceWebServicesCartFacade.isAnonymousUserCart(cartGuid);
		}
		return true;
	}

	protected List<CartModificationData> replaceVouchersValidationResults(final List<CartModificationData> cartModifications,
			final List<CartVoucherValidationData> inValidVouchers)
	{
		if (CollectionUtils.isEmpty(inValidVouchers))
		{
			// do not replace
			return cartModifications;
		}

		final Predicate<CartModificationData> isNotVoucherModification = modification ->
				!COUPON_STATUS_CODE.equals(modification.getStatusCode()) && !VOUCHER_STATUS_CODE.equals(modification.getStatusCode());

		return Collections.unmodifiableList(Stream.concat( //
				cartModifications.stream().filter(isNotVoucherModification), //
				inValidVouchers.stream().map(this::createCouponValidationResult) //
		).collect(Collectors.toList()));
	}

	protected CartModificationData createCouponValidationResult(final CartVoucherValidationData voucherValidationData)
	{
		final CartModificationData cartModificationData = new CartModificationData();
		cartModificationData.setStatusCode(COUPON_STATUS_CODE);
		cartModificationData.setStatusMessage(voucherValidationData.getSubject());
		return cartModificationData;
	}
}
