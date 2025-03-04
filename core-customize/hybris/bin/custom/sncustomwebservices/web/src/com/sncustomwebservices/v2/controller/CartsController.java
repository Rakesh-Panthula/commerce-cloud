/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.order.CartAccessCodeFacade;
import de.hybris.platform.commercefacades.order.SaveCartFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.accesscode.exceptions.AccessCodeException;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.commerceservices.order.CommerceCartMergingException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartRestorationException;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commercewebservicescommons.dto.accessCode.SapAccessCodeWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.SAPGuestUserRequestWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.cart.impl.CommerceWebServicesCartFacade;
import com.sncustomwebservices.order.data.CartDataList;
import com.sncustomwebservices.requestfrom.RequestFromValueSetter;
import com.sncustomwebservices.skipfield.SkipCartFieldValueSetter;
import com.sncustomwebservices.skipfield.SkipCartListFieldValueSetter;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static de.hybris.platform.commercefacades.order.constants.OrderOccControllerRequestFromConstants.CARTS_CONTROLLER;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Carts")
public class CartsController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(CartsController.class);

	private static final String COUPON_STATUS_CODE = "couponNotValid";
	private static final String VOUCHER_STATUS_CODE = "voucherNotValid";
	public static final String GUEST_USER_MAPPER_CONFIG = "sapGuestUserEmail";
	private static final String GUEST_USER_NAME = "guest";

	@Resource(name = "customerFacade")
	private CustomerFacade customerFacade;
	@Resource(name = "saveCartFacade")
	private SaveCartFacade saveCartFacade;
	@Resource(name = "skipCartFieldValueSetter")
	private SkipCartFieldValueSetter skipCartFieldValueSetter;
	@Resource(name = "skipCartListFieldValueSetter")
	private SkipCartListFieldValueSetter skipCartListFieldValueSetter;
	@Resource(name = "guestUserDTOValidator")
	private Validator guestUserDTOValidator;
	@Resource(name = "cartAccessCodeFacade")
	CartAccessCodeFacade cartAccessCodeFacade;
	@Resource(name = "requestFromValueSetter")
	private RequestFromValueSetter requestFromValueSetter;

	@GetMapping
	@ResponseBody
	@Operation(operationId = "getCarts", summary = "Retrieves the carts of a customer.", description = "Retrieves a list of all the carts associated with a customer.")
	@ApiBaseSiteIdAndUserIdParam
	public CartListWsDTO getCarts(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
			@Parameter(description = "If the value is true, only saved carts are returned.") @RequestParam(defaultValue = "false") final boolean savedCartsOnly,
			@Parameter(description = "Pagination for savedCartsOnly. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page if the savedCartsOnly parameter is set to true. Default value: 20.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results if the savedCartsOnly parameter is set to true.") @RequestParam(required = false) final String sort)
	{
		if (getUserFacade().isAnonymousUser())
		{
			throw new AccessDeniedException("Access is denied");
		}

		final CartDataList cartDataList = new CartDataList();

		skipCartListFieldValueSetter.setValue(fields);
		requestFromValueSetter.setRequestFrom(CARTS_CONTROLLER);

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
	@Operation(operationId = "getCart", summary = "Retrieves a cart.", description = "Retrieves a cart using the cart identifier. To get entryGroup information, set fields value as follows: fields=entryGroups(BASIC), fields=entryGroups(DEFAULT), or fields=entryGroups(FULL).")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartWsDTO getCart(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{

		skipCartFieldValueSetter.setValue(fields);
		requestFromValueSetter.setRequestFrom(CARTS_CONTROLLER);
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
			@Parameter(description = "The cart GUID that will be merged with the anonymous cart.") @RequestParam(required = false) final String toMergeCartGuid,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("createCart");
		skipCartFieldValueSetter.setValue(fields);
		requestFromValueSetter.setRequestFrom(CARTS_CONTROLLER);
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
	@Operation(operationId = "removeCart", summary = "Deletes the cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCart()
	{
		getCartFacade().removeSessionCart();
	}

	/**
	 * @deprecated since 2211.28, please use {@link #setEmail(SAPGuestUserRequestWsDTO)} instead
	 */
	@Deprecated(since = "2211.28", forRemoval = true)
	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/email")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "replaceCartGuestUser", summary = "Assigns an email address to the cart.", description = "Assigns an email to the cart. This endpoint is deprecated in the 2211.28 update and its deletion is planned. Please use '/setEmail' instead.")
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

		customerFacade.createGuestUserForAnonymousCheckout(email, GUEST_USER_NAME);
	}

	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/setEmail", consumes = APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(operationId = "setGuestUserEmail", summary = "Assigns an email address to the cart.", description = "Assigns an email to the cart. This endpoint is added in the 2211.28 update.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void setEmail(@RequestBody SAPGuestUserRequestWsDTO guest) throws DuplicateUidException
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("replaceCartGuestUser: email={}", sanitize(guest.getEmail()));
		}
		if (!EmailValidator.getInstance().isValid(guest.getEmail()))
		{
			throw new RequestParameterException("Email [" + sanitize(guest.getEmail()) + "] is not a valid e-mail address!",
					RequestParameterException.INVALID, "login");
		}

		customerFacade.createGuestUserForAnonymousCheckout(guest.getEmail(), GUEST_USER_NAME);
	}

	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@PostMapping(value = "/{cartId}/guestuser", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@ApiResponses(value =
	{ @ApiResponse(responseCode = "201", description = "Created"),
	  @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))),
	  @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))),
	  @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))),
	  @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))) })
	@Operation(operationId = "createCartGuestUser", summary = "Creates a guest user for the cart.", description = "Creates a guest user, and assigns the user to the cart. This api is specifically designed to create a guest user for a cart that only has an anonymous user assigned."
			+ "\n\n" + "Note: API will deny access if the cart already has a normal user or a guest user assigned")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public SAPGuestUserRequestWsDTO createCartGuestUser(
			@Parameter(description = "Optional attributes needed to create a guest user, such as email address, which will be used during the normal anonymous guest checkout process ") @RequestBody(required = false) final SAPGuestUserRequestWsDTO guestUser,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws DuplicateUidException
	{
		CustomerData guestCustomerData = null;
		if (guestUser != null)
		{
			validate(guestUser, "guestUser", guestUserDTOValidator);
			guestCustomerData = getDataMapper().map(guestUser, CustomerData.class, GUEST_USER_MAPPER_CONFIG);
		}
		guestCustomerData = customerFacade.createGuestUserForCheckout(guestCustomerData, GUEST_USER_NAME);
		return getDataMapper().map(guestCustomerData, SAPGuestUserRequestWsDTO.class, fields);
	}

	@Secured({ "ROLE_GUEST" })
	@PatchMapping(value = "/{cartId}/guestuser", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@ApiResponses(value =
	{ @ApiResponse(responseCode = "200", description = "OK"),
	  @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))),
	  @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))),
	  @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))),
	  @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = ErrorListWsDTO.class))) })
	@Operation(operationId = "updateCurrentUserProfile", summary = "Updates profile for current cart guest user.", description = "Updates profile for current cart guest user. This API is specifically designed to update profile for guest user,"
			+ " so it can only be accessed after a guest user has been created for the cart. Only attributes provided in the request body will be changed." + "\n\n"
			+ "Note: API will deny access if the cart already has a normal user or no user assigned")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public SAPGuestUserRequestWsDTO updateCurrentUserProfile(
			@Parameter(description = "Attributes needed to update for guest user, such as email address ", required = true) @RequestBody final SAPGuestUserRequestWsDTO guestUser,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
        	validate(guestUser, "guestUser", guestUserDTOValidator);
		final CustomerData guestCustomerData = getCartFacade().getCurrentCartCustomer();
		LOG.debug("updateGuestUser: userId={}", guestCustomerData.getUid());
		getDataMapper().map(guestUser, guestCustomerData, GUEST_USER_MAPPER_CONFIG, false);
		return getDataMapper().map(customerFacade.updateGuestUserProfile(guestCustomerData), SAPGuestUserRequestWsDTO.class, fields);
	}

	@PostMapping(path = "/{cartId}/validate")
	@ResponseBody
	@Operation(operationId = "validateCart", summary = "Validates the cart.", description = "Validates the cart and returns the cart data during checkout and quotation.")
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

	/**
	 * Generate access code for cartId in the url
	 * User id is user identifier or 'current' for currently authenticated user, 'anonymous' for anonymous user when user id
	 * is anonymous, cart id is cart.guid
	 *
	 * @return access code for cartId
	 * @throws AccessCodeException sign cart id failed exception
	 */
	@PostMapping(value = "/{cartId}/accessCode", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCartIdAccessCode", summary = "Generates an access code for the cart.", description = "Generates an access code for the specified cart. An access code is composed by a payload part and a signature part.")
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public SapAccessCodeWsDTO getCartIdAccessCode() throws AccessCodeException
	{
		final SapAccessCodeWsDTO accessCodeWsDTO = new SapAccessCodeWsDTO();
		accessCodeWsDTO.setAccessCode(this.cartAccessCodeFacade.generateCartIdAccessCode());
		return accessCodeWsDTO;
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
