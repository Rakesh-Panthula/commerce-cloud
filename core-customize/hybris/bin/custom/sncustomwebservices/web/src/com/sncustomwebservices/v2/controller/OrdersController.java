/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.basecommerce.enums.CancelReason;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.OrderHistoriesData;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.dto.order.CancellationRequestEntryInputListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CancellationRequestEntryInputWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderHistoryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.PaymentAuthorizationException;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.ordermanagementfacades.cancellation.data.OrderCancelEntryData;
import de.hybris.platform.ordermanagementfacades.cancellation.data.OrderCancelRequestData;
import de.hybris.platform.ordermanagementfacades.order.OmsOrderFacade;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.util.Config;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.exceptions.NoCheckoutCartException;
import com.sncustomwebservices.requestfrom.RequestFromValueSetter;
import com.sncustomwebservices.strategies.OrderCodeIdentificationStrategy;
import com.sncustomwebservices.v2.helper.OrdersHelper;
import com.sncustomwebservices.skipfield.SkipOrderFieldValueSetter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import static de.hybris.platform.commercefacades.order.constants.OrderOccControllerRequestFromConstants.ORDERS_CONTROLLER;
import static java.util.stream.Collectors.toList;



/**
 * Web Service Controller for the ORDERS resource. Most methods check orders of the user. Methods require authentication
 * and are restricted to https channel.
 */


@Controller
@RequestMapping(value = "/{baseSiteId}")
@Tag(name = "Orders")
public class OrdersController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(OrdersController.class);
	public static final String TOGGLE_GET_ORDER_BY_CODE_OR_GUID_ENABLED= "toggle.getOrderByCodeOrGuid.enabled";

	@Resource(name = "orderFacade")
	private OrderFacade orderFacade;
	@Resource(name = "orderCodeIdentificationStrategy")
	private OrderCodeIdentificationStrategy orderCodeIdentificationStrategy;
	@Resource(name = "cartLoaderStrategy")
	private CartLoaderStrategy cartLoaderStrategy;
	@Resource(name = "ordersHelper")
	private OrdersHelper ordersHelper;
	@Resource(name = "omsOrderFacade")
	private OmsOrderFacade omsOrderFacade;
	@Resource(name = "cancellationRequestEntryInputListDTOValidator")
	private Validator cancellationRequestEntryInputListDTOValidator;
	@Resource(name = "wsCustomerFacade")
	private CustomerFacade customerFacade;
	@Resource(name = "skipOrderFieldValueSetter")
	private SkipOrderFieldValueSetter skipOrderFieldValueSetter;
	@Resource(name = "requestFromValueSetter")
	private RequestFromValueSetter requestFromValueSetter;

	@Secured("ROLE_TRUSTED_CLIENT")
	@RequestMapping(value = "/orders/{code}", method = RequestMethod.GET)
	@CacheControl(directive = CacheControlDirective.PRIVATE)
	@Cacheable(value = "orderCache", key = "T(de.hybris.platform.commercewebservicescommons.cache.CommerceCacheKeyGenerator).generateKey(false,true,'getOrder',#code,#fields)")
	@ResponseBody
	@Operation(operationId = "getOrder", summary = "Retrieves the order.", description = "Retrieves the details of the order using the Globally Unique Identifier (GUID) or the order code. To get entryGroup information, set fields value as follows: fields=entryGroups(BASIC), fields=entryGroups(DEFAULT), or fields=entryGroups(FULL).")
	@ApiBaseSiteIdParam
	public OrderWsDTO getOrder(
			@Parameter(description = "Order GUID or order code.", required = true) @PathVariable final String code,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final OrderData orderData;
		skipOrderFieldValueSetter.setValue(fields);
		requestFromValueSetter.setRequestFrom(ORDERS_CONTROLLER);
		if (orderCodeIdentificationStrategy.isID(code))
		{
			orderData = orderFacade.getOrderDetailsForGUID(code);
		}
		else
		{
			orderData = orderFacade.getOrderDetailsForCodeWithoutUser(code);
		}

		return getDataMapper().map(orderData, OrderWsDTO.class, fields);
	}


	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/users/{userId}/orders/{code}", method = RequestMethod.GET)
	@CacheControl(directive = CacheControlDirective.PRIVATE)
	@Cacheable(value = "orderCache", key = "T(de.hybris.platform.commercewebservicescommons.cache.CommerceCacheKeyGenerator).generateKey(true,true,'getOrderForUserByCode',#code,#fields)")
	@ResponseBody
	@Operation(operationId = "getUserOrders", summary = "Retrieves the order.", description = "Retrieves the details of the order. To get entryGroup information, set fields value as follows: fields=entryGroups(BASIC), fields=entryGroups(DEFAULT), or fields=entryGroups(FULL).")
	@ApiBaseSiteIdAndUserIdParam
	public OrderWsDTO getUserOrders(
			@Parameter(description = "Order GUID or order code.", required = true) @PathVariable final String code,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		skipOrderFieldValueSetter.setValue(fields);
		requestFromValueSetter.setRequestFrom(ORDERS_CONTROLLER);
		OrderData orderData;
		if(Config.getBoolean(TOGGLE_GET_ORDER_BY_CODE_OR_GUID_ENABLED, true))
		{
			// get order by code or guid
			orderData = orderFacade.getOrderDetailsForPotentialId(code);
		} else {
			// get order only by code
			orderData = orderFacade.getOrderDetailsForCode(code);
		}
		return getDataMapper().map(orderData, OrderWsDTO.class, fields);
	}



	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@CacheControl(directive = CacheControlDirective.PRIVATE)
	@RequestMapping(value = "/users/{userId}/orders", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getUserOrderHistory", summary = "Retrieves the order history of a customer.", description = "Retrieves the order history of a customer. The response may display the results across multiple pages, when applicable.")
	@ApiBaseSiteIdAndUserIdParam
	public OrderHistoryListWsDTO getUserOrderHistory(
			@Parameter(description = "Filters orders with specified status(es). For example, statuses = CANCELLED returns CANCELLED orders. To filter multiple statues, separate statues with \",\" and all orders with these statuses will be returned. " 
					+ "For example, statuses = CANCELLED,COMPLETED,READY returns all orders with statuses CANCELLED,COMPLETED, and READY.\n"
					+ "Options available are:\nCANCELLING,\nCHECKED_VALID,\nCREATED,\nOPEN,\nREADY,\n"
					+ "CHECKED_INVALID,\nON_HOLD,\nON_VALIDATION,\nPENDING_APPROVAL,\nSUSPENDED,\nCOMPLETED,\n"
					+ "PAYMENT_AUTHORIZED,\nPENDING_APPROVAL_FROM_MERCHANT,\nCANCELLED,\nPAYMENT_NOT_AUTHORIZED,\n"
					+ "PENDING_QUOTE,\nAPPROVED_QUOTE,\nPAYMENT_AMOUNT_RESERVED,\nPAYMENT_AMOUNT_NOT_RESERVED,\n"
					+ "REJECTED_QUOTE,\nAPPROVED,\nPAYMENT_CAPTURED,\nPAYMENT_NOT_CAPTURED,\nREJECTED,\n"
					+ "APPROVED_BY_MERCHANT,\nFRAUD_CHECKED,\nORDER_SPLIT,\nREJECTED_BY_MERCHANT,\nASSIGNED_TO_ADMIN,\n"
					+ "PROCESSING_ERROR,\nB2B_PROCESSING_ERROR,\nWAIT_FRAUD_MANUAL_CHECK,\nPAYMENT_NOT_VOIDED,\n"
					+ "TAX_NOT_VOIDED,\nTAX_NOT_COMMITTED,\nTAX_NOT_REQUOTED") @RequestParam(required = false) final String statuses,
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "The number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletResponse response)
	{
		validateStatusesEnumValue(statuses);

		final OrderHistoryListWsDTO orderHistoryList = ordersHelper.searchOrderHistory(statuses, currentPage, pageSize, sort,
				addPaginationField(fields));

		setTotalCountHeader(response, orderHistoryList.getPagination());

		return orderHistoryList;
	}


	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/users/{userId}/orders", method = RequestMethod.HEAD)
	@ResponseBody
	@Operation(operationId = "countUserOrders", summary = "Retrieves the total number of orders for a customer.", description = "Retrieves the total number of orders for a customer in the response header, under the \"x-total-count\" attribute.")
	@ApiBaseSiteIdAndUserIdParam
	public void countUserOrders(
			@Parameter(description = "Filters only certain order statuses. For example, statuses=CANCELLED,CHECKED_VALID would only return orders with status CANCELLED or CHECKED_VALID.") @RequestParam(required = false) final String statuses,
			final HttpServletResponse response)
	{
		final OrderHistoriesData orderHistoriesData = ordersHelper.searchOrderHistory(statuses, 0, 1, null);

		setTotalCountHeader(response, orderHistoriesData.getPagination());
	}


	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/users/{userId}/orders", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@Operation(operationId = "placeOrder", summary = "Creates an order.", description = "Creates an order and returns the order details. The response contains all the order data.")
	@ApiBaseSiteIdAndUserIdParam
	public OrderWsDTO placeOrder(
			@Parameter(description = "Cart code for logged in user, cart GUID for guest checkout", required = true) @RequestParam final String cartId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
	{
		LOG.info("placeOrder");
		skipOrderFieldValueSetter.setValue(fields);
		requestFromValueSetter.setRequestFrom(ORDERS_CONTROLLER);
		cartLoaderStrategy.loadCart(cartId);

		validateCartForPlaceOrder();

		//authorize
		if (!getCheckoutFacade().authorizePayment(null))
		{
			throw new PaymentAuthorizationException();
		}

		//placeorder
		final OrderData orderData = getCheckoutFacade().placeOrder();
		return getDataMapper().map(orderData, OrderWsDTO.class, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@PostMapping(value = "/users/{userId}/orders/{code}/cancellation", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "doCancelOrder", summary = "Cancels an order.", description = "Cancels an order, completely or partially. For a complete cancellation, add all the order entry numbers and quantities in the request body."
			+ " For partial cancellation, only add the order entry numbers and quantities to be cancelled.")
	@ApiBaseSiteIdAndUserIdParam
	public void doCancelOrder(@Parameter(description = "Order code", required = true) @PathVariable final String code,
			@Parameter(description = "Cancellation request input list for the current order.", required = true) @RequestBody final CancellationRequestEntryInputListWsDTO cancellationRequestEntryInputList)
	{
		validate(cancellationRequestEntryInputList, "cancellationRequestEntryInputList",
				cancellationRequestEntryInputListDTOValidator);
		validateUserForOrder(code);

		omsOrderFacade.createRequestOrderCancel(prepareCancellationRequestData(code, cancellationRequestEntryInputList));
	}

	/**
	 * Validates if the current user has access to the order
	 *
	 * @param code the order code
	 * @throws NotFoundException if current user has no access to the order
	 */
	protected void validateUserForOrder(final String code)
	{
		try
		{
			orderFacade.getOrderDetailsForCode(code);
		}
		catch (final UnknownIdentifierException ex)
		{
			LOG.warn("Order not found for the current user in current BaseStore", ex);
			throw new NotFoundException("Resource not found");
		}
	}

	/**
	 * It prepares the {@link OrderCancelRequestData} object by
	 * taking the order code and a map of order entry and cancel quantity and sets the user
	 *
	 * @param code                              which we want to request to cancel
	 * @param cancellationRequestEntryInputList map of order entry and cancel quantity
	 * @return Populated {@link OrderCancelRequestData}
	 */
	protected OrderCancelRequestData prepareCancellationRequestData(final String code,
			final CancellationRequestEntryInputListWsDTO cancellationRequestEntryInputList)
	{
		final OrderCancelRequestData cancellationRequest = new OrderCancelRequestData();

		final List<OrderCancelEntryData> cancellationEntries = cancellationRequestEntryInputList.getCancellationRequestEntryInputs()
				.stream().map(this::mapToOrderCancelEntryData).collect(toList());

		cancellationRequest.setUserId(customerFacade.getCurrentCustomerUid());
		cancellationRequest.setOrderCode(code);
		cancellationRequest.setEntries(cancellationEntries);

		return cancellationRequest;
	}

	protected OrderCancelEntryData mapToOrderCancelEntryData(final CancellationRequestEntryInputWsDTO entryInput)
	{
		final OrderCancelEntryData cancelEntry = new OrderCancelEntryData();

		cancelEntry.setOrderEntryNumber(entryInput.getOrderEntryNumber());
		cancelEntry.setCancelQuantity(entryInput.getQuantity());
		cancelEntry.setCancelReason(CancelReason.CUSTOMERREQUEST);

		return cancelEntry;
	}

}
