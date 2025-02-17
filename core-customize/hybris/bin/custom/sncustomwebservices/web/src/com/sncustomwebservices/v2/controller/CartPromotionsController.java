/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.product.data.PromotionResultData;
import de.hybris.platform.commercefacades.promotion.CommercePromotionRestrictionFacade;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commerceservices.promotion.CommercePromotionRestrictionException;
import de.hybris.platform.commercewebservicescommons.dto.product.PromotionResultListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.voucher.VoucherListWsDTO;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.exceptions.NoCheckoutCartException;
import com.sncustomwebservices.product.data.PromotionResultDataList;
import com.sncustomwebservices.voucher.data.VoucherDataList;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@Tag(name = "Cart Promotions")
public class CartPromotionsController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(CartPromotionsController.class);

	@Resource(name = "commercePromotionRestrictionFacade")
	private CommercePromotionRestrictionFacade commercePromotionRestrictionFacade;

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@GetMapping(value = "/{cartId}/promotions")
	@ResponseBody
	@Operation(operationId = "getCartPromotions", summary = "Get information about promotions applied on cart.", description =
			"Returns information about the promotions applied on the cart. "
					+ "Requests pertaining to promotions have been developed for the previous version of promotions and vouchers, and as a result, some of them "
					+ "are currently not compatible with the new promotions engine.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public PromotionResultListWsDTO getCartPromotions(
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("getCartPromotions");
		final List<PromotionResultData> appliedPromotions = new ArrayList<>();
		final List<PromotionResultData> orderPromotions = getSessionCart().getAppliedOrderPromotions();
		final List<PromotionResultData> productPromotions = getSessionCart().getAppliedProductPromotions();
		appliedPromotions.addAll(orderPromotions);
		appliedPromotions.addAll(productPromotions);

		final PromotionResultDataList dataList = new PromotionResultDataList();
		dataList.setPromotions(appliedPromotions);
		return getDataMapper().map(dataList, PromotionResultListWsDTO.class, fields);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@GetMapping(value = "/{cartId}/promotions/{promotionId}")
	@ResponseBody
	@Operation(operationId = "getCartPromotion", summary = "Get information about promotion applied on cart.", description =
			"Returns information about a promotion (with a specific promotionId), that has "
					+ "been applied on the cart. Requests pertaining to promotions have been developed for the previous version of promotions and vouchers, and as a result, some "
					+ "of them are currently not compatible with the new promotions engine.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public PromotionResultListWsDTO getCartPromotion(
			@Parameter(description = "Promotion identifier (code)", required = true) @PathVariable final String promotionId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("getCartPromotion: promotionId = {}", sanitize(promotionId));
		}
		final List<PromotionResultData> appliedPromotions = new ArrayList<>();
		final List<PromotionResultData> orderPromotions = getSessionCart().getAppliedOrderPromotions();
		final List<PromotionResultData> productPromotions = getSessionCart().getAppliedProductPromotions();
		for (final PromotionResultData prd : orderPromotions)
		{
			if (prd.getPromotionData().getCode().equals(promotionId))
			{
				appliedPromotions.add(prd);
			}
		}
		for (final PromotionResultData prd : productPromotions)
		{
			if (prd.getPromotionData().getCode().equals(promotionId))
			{
				appliedPromotions.add(prd);
			}
		}

		final PromotionResultDataList dataList = new PromotionResultDataList();
		dataList.setPromotions(appliedPromotions);
		return getDataMapper().map(dataList, PromotionResultListWsDTO.class, fields);
	}

	@Secured({ "ROLE_TRUSTED_CLIENT" })
	@PostMapping(value = "/{cartId}/promotions")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "doApplyCartPromotion", summary = "Enables promotions based on the promotionsId of the cart.", description =
			"Enables a promotion for the order based on the promotionId defined for the cart. "
					+ "Requests pertaining to promotions have been developed for the previous version of promotions and vouchers, and as a result, some of them are currently not compatible "
					+ "with the new promotions engine.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void doApplyCartPromotion(
			@Parameter(description = "Promotion identifier (code)", required = true) @RequestParam(required = true) final String promotionId)
			throws CommercePromotionRestrictionException
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("doApplyCartPromotion: promotionId = {}", sanitize(promotionId));
		}
		commercePromotionRestrictionFacade.enablePromotionForCurrentCart(promotionId);
	}

	@Secured({ "ROLE_TRUSTED_CLIENT" })
	@DeleteMapping(value = "/{cartId}/promotions/{promotionId}")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeCartPromotion", summary = "Disables the promotion based on the promotionsId of the cart.", description =
			"Disables the promotion for the order based on the promotionId defined for the cart. "
					+ "Requests pertaining to promotions have been developed for the previous version of promotions and vouchers, and as a result, some of them are currently not compatible with "
					+ "the new promotions engine.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCartPromotion(
			@Parameter(description = "Promotion identifier (code)", required = true) @PathVariable final String promotionId)
			throws CommercePromotionRestrictionException
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("removeCartPromotion: promotionId = {}", sanitize(promotionId));
		}
		commercePromotionRestrictionFacade.disablePromotionForCurrentCart(promotionId);
	}

	@Secured({ "ROLE_CLIENT", "ROLE_CUSTOMERGROUP", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_GUEST" })
	@GetMapping(value = "/{cartId}/vouchers")
	@ResponseBody
	@Operation(operationId = "getCartVouchers", summary = "Get a list of vouchers applied to the cart.", description = "Returns a list of vouchers applied to the cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public VoucherListWsDTO getCartVouchers(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		LOG.debug("getVouchers");
		final VoucherDataList dataList = new VoucherDataList();
		dataList.setVouchers(getVoucherFacade().getVouchersForCart());
		return getDataMapper().map(dataList, VoucherListWsDTO.class, fields);
	}

	@Secured({ "ROLE_CLIENT", "ROLE_CUSTOMERGROUP", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_GUEST" })
	@PostMapping(value = "/{cartId}/vouchers")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "doApplyCartVoucher", summary = "Applies a voucher based on the voucherId defined for the cart.", description = "Applies a voucher based on the voucherId defined for the specified cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void doApplyCartVoucher(
			@Parameter(description = "Voucher identifier (code)", required = true) @RequestParam final String voucherId,
			final HttpServletRequest request) throws NoCheckoutCartException, VoucherOperationException
	{
		applyVoucherForCartInternal(voucherId, request);
	}

	@Secured({ "ROLE_CLIENT", "ROLE_CUSTOMERGROUP", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_GUEST" })
	@DeleteMapping(value = "/{cartId}/vouchers/{voucherId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(operationId = "removeCartVoucher", summary = "Deletes a voucher defined for the current cart.", description = "Deletes a voucher based on the voucherId defined for the current cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void removeCartVoucher(
			@Parameter(description = "Voucher identifier (code)", required = true) @PathVariable final String voucherId)
			throws NoCheckoutCartException, VoucherOperationException
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("release voucher : voucherCode = {}", sanitize(voucherId));
		}
		if (!getCheckoutFacade().hasCheckoutCart())
		{
			throw new NoCheckoutCartException("Cannot realese voucher. There was no checkout cart created yet!");
		}
		getVoucherFacade().releaseVoucher(voucherId);
	}
}
