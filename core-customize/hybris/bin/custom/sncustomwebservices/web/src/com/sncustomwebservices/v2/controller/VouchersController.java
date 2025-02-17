/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.voucher.VoucherFacade;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commercewebservicescommons.dto.voucher.VoucherWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


@Controller
@RequestMapping(value = "/{baseSiteId}/vouchers")
@Tag(name = "Vouchers")
public class VouchersController extends BaseController
{
	@Resource(name = "voucherFacade")
	private VoucherFacade voucherFacade;

	@Secured("ROLE_TRUSTED_CLIENT")
	@RequestMapping(value = "/{code}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getVoucher", summary = "Get a voucher based on code.", description = "Returns details of a single voucher that is specified by its voucher identification code.")
	@ApiBaseSiteIdParam
	public VoucherWsDTO getVoucher(
			@Parameter(description = "Voucher identifier (code)", required = true) @PathVariable final String code,
			@ApiFieldsParam(defaultValue = BASIC_FIELD_SET) @RequestParam(defaultValue = BASIC_FIELD_SET) final String fields)
			throws VoucherOperationException
	{
		return getDataMapper().map(voucherFacade.getVoucher(code), VoucherWsDTO.class, fields);
	}
}
