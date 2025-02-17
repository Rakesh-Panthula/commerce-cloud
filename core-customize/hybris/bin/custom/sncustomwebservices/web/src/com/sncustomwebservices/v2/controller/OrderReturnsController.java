/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestEntryInputListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestModificationWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ReturnRequestStatusWsDTOType;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.v2.helper.OrderReturnsHelper;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orderReturns")
@Tag(name = "Return Requests")
public class OrderReturnsController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(OrderReturnsController.class);

	@Resource(name = "orderReturnsHelper")
	private OrderReturnsHelper orderReturnsHelper;

	@Resource(name = "returnRequestEntryInputListDTOValidator")
	private Validator returnRequestEntryInputListDTOValidator;

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@CacheControl(directive = CacheControlDirective.PUBLIC, maxAge = 120)
	@GetMapping(produces = MediaType.APPLICATION_JSON)
	@ResponseBody
	@Operation(operationId = "getReturnRequests", summary = "Gets the user's return requests history", description = "Returns order return request data associated with a specified user for a specified base store.")
	@ApiBaseSiteIdAndUserIdParam
	public ReturnRequestListWsDTO getReturnRequests(
			@Parameter(description = "The current result page requested.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "The number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return orderReturnsHelper.searchOrderReturnRequests(currentPage, pageSize, sort, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@GetMapping(value = "/{returnRequestCode}", produces = MediaType.APPLICATION_JSON)
	@CacheControl(directive = CacheControlDirective.PUBLIC, maxAge = 120)
	@ResponseBody
	@Operation(operationId = "getReturnRequest", summary = "Get the details of a return request.", description = "Returns specific order return request details based on a specific return request code. The response contains detailed order return request information.")
	@ApiBaseSiteIdAndUserIdParam
	public ReturnRequestWsDTO getReturnRequest(
			@Parameter(description = "Order return request code", required = true) @PathVariable final String returnRequestCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return orderReturnsHelper.getOrderReturnRequest(returnRequestCode, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@Operation(operationId = "updateReturnRequest", summary = "Updates the order return request.", description = "Updates the order return request. Only cancellation of the request is supported by setting the attribute status to CANCELLING. Cancellation of the return request cannot be reverted")
	@ResponseStatus(HttpStatus.OK)
	@PatchMapping(value = "/{returnRequestCode}", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public void updateReturnRequest(
			@Parameter(description = "Order return request code", required = true) @PathVariable final String returnRequestCode,
			@Parameter(description = "Return request modification object.", required = true) @RequestBody final ReturnRequestModificationWsDTO returnRequestModification)
	{
		if (returnRequestModification.getStatus() == ReturnRequestStatusWsDTOType.CANCELLING)
		{
			orderReturnsHelper.cancelOrderReturnRequest(returnRequestCode);
		}
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@PostMapping(produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ResponseStatus(value = HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "createReturnRequest", summary = "Create an order return request.", description = "Creates an order return request.")
	@ApiBaseSiteIdAndUserIdParam
	public ReturnRequestWsDTO createReturnRequest(
			@Parameter(description = "Return request input list for the current order.", required = true) @RequestBody final ReturnRequestEntryInputListWsDTO returnRequestEntryInputList,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		validate(returnRequestEntryInputList, "returnRequestEntryInputList", returnRequestEntryInputListDTOValidator);
		return orderReturnsHelper.createOrderReturnRequest(returnRequestEntryInputList, fields);
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	@ResponseBody
	@ExceptionHandler({ UnknownIdentifierException.class })
	public ErrorListWsDTO handleNotFoundExceptions(final Exception ex)
	{
		LOG.debug("Unknown identifier error", ex);
		return handleErrorInternal(NotFoundException.class.getSimpleName(), ex.getMessage());
	}
}
