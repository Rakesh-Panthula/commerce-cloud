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
import com.sncustomwebservices.skipfield.SkipReturnRequestFieldValueSetter;
import com.sncustomwebservices.skipfield.SkipReturnRequestListFieldValueSetter;

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
	@Resource(name = "skipReturnRequestListFieldValueSetter")
	private SkipReturnRequestListFieldValueSetter skipReturnRequestListFieldValueSetter;
	@Resource(name = "skipReturnRequestFieldValueSetter")
	private SkipReturnRequestFieldValueSetter skipReturnRequestFieldValueSetter;

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@CacheControl(directive = CacheControlDirective.PRIVATE)
	@GetMapping(produces = MediaType.APPLICATION_JSON)
	@ResponseBody
	@Operation(operationId = "getReturnRequests", summary = "Retrieves the return request history of the customer.", description = "Retrieves the order history for all return requests associated with a customer.")
	@ApiBaseSiteIdAndUserIdParam
	public ReturnRequestListWsDTO getReturnRequests(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		skipReturnRequestListFieldValueSetter.setValue(fields);
		return orderReturnsHelper.searchOrderReturnRequests(currentPage, pageSize, sort, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@GetMapping(value = "/{returnRequestCode}", produces = MediaType.APPLICATION_JSON)
	@CacheControl(directive = CacheControlDirective.PRIVATE)
	@ResponseBody
	@Operation(operationId = "getReturnRequest", summary = "Retrieves the details of a return request.", description = "Retrieves the details of a return request. To get entryGroup information, set fields value as follows: fields=order(entryGroups(BASIC)), fields=order(entryGroups(DEFAULT)), or fields=order(entryGroups(FULL)).")
	@ApiBaseSiteIdAndUserIdParam
	public ReturnRequestWsDTO getReturnRequest(
			@Parameter(description = "Order returns request code.", required = true) @PathVariable final String returnRequestCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		skipReturnRequestFieldValueSetter.setValue(fields);
		return orderReturnsHelper.getOrderReturnRequest(returnRequestCode, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@Operation(operationId = "updateReturnRequest", summary = "Updates the order return request using the specified code.", description = "Updates the order return request. Only cancellation of the request is supported by setting the attribute status to CANCELLING. Cancellation of the return request cannot be reverted")
	@ResponseStatus(HttpStatus.OK)
	@PatchMapping(value = "/{returnRequestCode}", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public void updateReturnRequest(
			@Parameter(description = "Order returns request code.", required = true) @PathVariable final String returnRequestCode,
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
	@Operation(operationId = "createReturnRequest", summary = "Creates an order return request.",
			description = "Creates an order return request. An order can be completely or partially returned."
					+ " For a complete return, add all order entry numbers and quantities in the request body."
					+ " For a partial return, only add the order entry numbers and quantities of the selected products.")
	@ApiBaseSiteIdAndUserIdParam
	public ReturnRequestWsDTO createReturnRequest(
			@Parameter(description = "Return request input list for the current order.", required = true) @RequestBody final ReturnRequestEntryInputListWsDTO returnRequestEntryInputList,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		skipReturnRequestFieldValueSetter.setValue(fields);
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
