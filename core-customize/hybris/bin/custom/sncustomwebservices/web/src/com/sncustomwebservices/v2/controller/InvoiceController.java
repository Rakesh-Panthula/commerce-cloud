/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.invoice.InvoiceFacade;
import de.hybris.platform.commercefacades.invoice.data.SAPInvoiceData;
import de.hybris.platform.commercewebservicescommons.dto.order.SAPInvoicesWsDTO;
import de.hybris.platform.core.servicelayer.data.SearchPageData;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.pagination.WebPaginationUtils;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;



@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orders")
@Tag(name = "Invoices")
public class InvoiceController extends BaseCommerceController
{
	@Resource(name = "sapInvoiceFacade")
	private InvoiceFacade sapInvoiceFacade;

	@Resource(name = "webPaginationUtils")
	private WebPaginationUtils webPaginationUtils;

	@Secured(
	{ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@GetMapping(value = "/{code}/invoices")
	@CacheControl(directive = CacheControlDirective.PUBLIC, maxAge = 120)
	@ResponseBody
	@Operation(operationId = "getUserOrderInvoices", summary = "Get invoices for an order.", description = "Returns invoices based on a specific order code. The response contains list of invoice information.")
	@ApiBaseSiteIdAndUserIdParam
	@ApiResponse(responseCode = "200", description = "List of invoice of an order")
	public SAPInvoicesWsDTO getUserOrderInvoices(@Parameter(description = "Order Code", required = true)
	@PathVariable
	final String code, @Parameter(description = "The current result page requested.")
	@RequestParam(defaultValue = DEFAULT_CURRENT_PAGE)
	final int currentPage, @Parameter(description = "The number of results returned per page.")
	@RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
	final int pageSize, @Parameter(description = "Sorting method applied to the return results.")
	@RequestParam(required = false)
	final String sort, @ApiFieldsParam
	@RequestParam(defaultValue = DEFAULT_FIELD_SET)
	final String fields)


	{
		final SearchPageData<SAPInvoiceData> searchPageDataInput = webPaginationUtils.buildSearchPageData(sort, currentPage,
				pageSize, true);
		final SearchPageData<SAPInvoiceData> invoices = sapInvoiceFacade.getInvoices(sanitize(code), searchPageDataInput,
				addPaginationField(fields));
		return getDataMapper().map(invoices, SAPInvoicesWsDTO.class, fields);

	}

	@Secured(
	{ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@GetMapping(value = "/{code}/invoices/{invoiceId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	@Operation(operationId = "getUserOrderInvoiceBinary", summary = "Get binary invoice of an order", description = "Get invoice of an order in encoded byte array")
	@ApiBaseSiteIdAndUserIdParam
	public ResponseEntity<byte[]> getUserOrderInvoiceBinary(@Parameter(description = "Order Code", required = true)
	@PathVariable
	final String code, @Parameter(description = "Invoice ID", required = true)
	@PathVariable
	final String invoiceId, @Parameter(description = "External system identifier where the invoice resides.", example = "S4SALES")
	@RequestParam(required = false)
	final String externalSystemId)
	{
		final byte[] byteArray = sapInvoiceFacade.getInvoiceBinary(sanitize(code), sanitize(invoiceId), sanitize(externalSystemId));
		if (byteArray.length <= 0)
		{
			throw new NotFoundException("Invoice with id = %s of order %s is not found.".formatted(invoiceId, code),
					"The invoice with the given id does not have any data", invoiceId);
		}
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "octet-stream"));

		headers.setContentDispositionFormData("attachment", invoiceId + ".pdf");
		headers.setContentLength(byteArray.length);
		return new ResponseEntity<>(byteArray, headers, HttpStatus.OK);

	}


}
