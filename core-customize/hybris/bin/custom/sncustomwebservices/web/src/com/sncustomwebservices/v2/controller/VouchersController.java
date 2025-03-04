/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.voucher.VoucherFacade;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commercewebservicescommons.dto.voucher.SAPVoucherOperationRequestWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.voucher.VoucherWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	/**
	 * @deprecated (since "2211.28", forRemoval = true)
	 */
	@Deprecated(since = "2211.28", forRemoval = true)
	@Secured("ROLE_TRUSTED_CLIENT")
	@RequestMapping(value = "/{code}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getVoucher", summary = "Retrieves the voucher.", description = "Retrieves the details of the voucher using the voucher identifier. This endpoint is deprecated in the 2211.28 update and its deletion is planned. Please use the POST {baseSiteId}/vouchers/code/search instead.")
	@ApiBaseSiteIdParam
	public VoucherWsDTO getVoucher(
			@Parameter(description = "Voucher identifier (code)", required = true, example = "VCHR-H8BC-Y3D5-34AL") @PathVariable final String code,
			@ApiFieldsParam(defaultValue = BASIC_FIELD_SET) @RequestParam(defaultValue = BASIC_FIELD_SET) final String fields)
			throws VoucherOperationException
	{
		return getDataMapper().map(voucherFacade.getVoucher(code), VoucherWsDTO.class, fields);
	}

	@Secured("ROLE_TRUSTED_CLIENT")
	@PostMapping("/code/search")
	@ResponseBody
	@Operation(operationId = "getVoucherByCode", summary = "Retrieves the voucher by voucher code.", description = "Retrieves the details of the voucher by voucher code. This endpoint is added in the 2211.28 update.")
	@ApiBaseSiteIdParam
	public VoucherWsDTO getVoucherByCode(@RequestBody final SAPVoucherOperationRequestWsDTO voucherOperationRequestWsDTO,
			@ApiFieldsParam(defaultValue = BASIC_FIELD_SET) @RequestParam(defaultValue = BASIC_FIELD_SET) final String fields)
			throws VoucherOperationException
	{
		final String voucherCode = voucherOperationRequestWsDTO.getVoucherCode();
		return getDataMapper().map(voucherFacade.getVoucher(voucherCode), VoucherWsDTO.class, fields);

	}
}
