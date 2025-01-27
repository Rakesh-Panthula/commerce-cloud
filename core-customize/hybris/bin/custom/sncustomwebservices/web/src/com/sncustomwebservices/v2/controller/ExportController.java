/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.product.ProductExportFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ProductResultData;
import de.hybris.platform.commercewebservicescommons.dto.product.ProductListWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.formatters.WsDateFormatter;
import com.sncustomwebservices.product.data.ProductDataList;

import javax.annotation.Resource;

import java.util.Date;
import java.util.EnumSet;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import static org.apache.commons.lang3.StringUtils.isEmpty;


/**
 * Web Services Controller to expose the functionality of the
 * {@link de.hybris.platform.commercefacades.product.ProductFacade} and SearchFacade.
 */
@Controller
@RequestMapping(value = "/{baseSiteId}/export/products")
@Tag(name = "Export")
public class ExportController extends BaseController
{
	private static final EnumSet<ProductOption> OPTIONS = EnumSet.allOf(ProductOption.class);
	private static final String DEFAULT_PAGE_VALUE = "0";
	private static final String MAX_INTEGER = "20";

	@Resource(name = "cwsProductExportFacade")
	private ProductExportFacade productExportFacade;
	@Resource(name = "wsDateFormatter")
	private WsDateFormatter wsDateFormatter;

	@Secured("ROLE_TRUSTED_CLIENT")
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getExportedProducts", summary = "Retrieves a list of exported products.", description = "Retrieves all of the products or only the products that were modified after the time in the timestamp parameter.")
	@ApiBaseSiteIdParam
	public ProductListWsDTO getExportedProducts(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_PAGE_VALUE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = MAX_INTEGER) final int pageSize,
			@Parameter(description = "Only products from this catalog are returned. The catalog must be provided along with the version.") @RequestParam(required = false) final String catalog,
			@Parameter(description = "Only products from this catalog version are returned. The catalog version must be provided along with the catalog.") @RequestParam(required = false) final String version,
			@Parameter(description = "Only products that are more recent than the given parameter are returned. The value should be in ISO-8601 format: 2018-01-09T16:28:45+0000.") @RequestParam(required = false) final String timestamp,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		if (isEmpty(catalog) && !isEmpty(version))
		{
			throw new RequestParameterException("Both 'catalog' and 'version' parameters have to be provided or ignored.",
					RequestParameterException.MISSING, "catalog");
		}

		if (isEmpty(version) && !isEmpty(catalog))
		{
			throw new RequestParameterException("Both 'catalog' and 'version' parameters have to be provided or ignored.",
					RequestParameterException.MISSING, "version");
		}

		if (isEmpty(timestamp))
		{
			return fullExport(fields, currentPage, pageSize, catalog, version);
		}
		else
		{
			return incrementalExport(fields, currentPage, pageSize, catalog, version, timestamp);
		}
	}

	protected ProductListWsDTO incrementalExport(final String fields, final int currentPage, final int pageSize,
			final String catalog, final String version, final String timestamp)
	{
		final Date timestampDate;
		try
		{
			timestampDate = wsDateFormatter.toDate(timestamp);
		}
		catch (final IllegalArgumentException e)
		{
			throw new RequestParameterException("Wrong time format. The only accepted format is ISO-8601.",
					RequestParameterException.INVALID, "timestamp", e);
		}

		final ProductResultData modifiedProducts = productExportFacade.getOnlyModifiedProductsForOptions(catalog, version,
				timestampDate, OPTIONS, currentPage, pageSize);

		return getDataMapper().map(convertResultset(currentPage, pageSize, catalog, version, modifiedProducts),
				ProductListWsDTO.class, fields);
	}

	protected ProductListWsDTO fullExport(final String fields, final int currentPage, final int pageSize, final String catalog,
			final String version)
	{
		final ProductResultData products = productExportFacade.getAllProductsForOptions(catalog, version, OPTIONS, currentPage,
				pageSize);

		return getDataMapper().map(convertResultset(currentPage, pageSize, catalog, version, products), ProductListWsDTO.class,
				fields);
	}

	protected ProductDataList convertResultset(final int page, final int pageSize, final String catalog, final String version,
			final ProductResultData modifiedProducts)
	{
		final ProductDataList result = new ProductDataList();
		result.setProducts(modifiedProducts.getProducts());
		if (pageSize > 0)
		{
			result.setTotalPageCount(((modifiedProducts.getTotalCount() % pageSize) == 0) ?
					(modifiedProducts.getTotalCount() / pageSize) :
					((modifiedProducts.getTotalCount() / pageSize) + 1));
		}
		result.setCurrentPage(page);
		result.setTotalProductCount(modifiedProducts.getTotalCount());
		result.setCatalog(catalog);
		result.setVersion(version);
		return result;
	}
}
