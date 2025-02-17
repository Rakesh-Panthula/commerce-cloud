/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.catalog.CatalogFacade;
import de.hybris.platform.commercefacades.catalog.CatalogOption;
import de.hybris.platform.commercefacades.catalog.PageOption;
import de.hybris.platform.commercefacades.catalog.data.CatalogData;
import de.hybris.platform.commercefacades.catalog.data.CatalogVersionData;
import de.hybris.platform.commercefacades.catalog.data.CatalogsData;
import de.hybris.platform.commercefacades.catalog.data.CategoryHierarchyData;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CatalogListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CatalogVersionWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CatalogWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CategoryHierarchyWsDTO;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetBuilder;
import de.hybris.platform.webservicescommons.mapping.impl.FieldSetBuilderContext;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
@RequestMapping(value = "/{baseSiteId}/catalogs")
@Tag(name = "Catalogs")
public class CatalogsController extends BaseController
{
	private static final Set<CatalogOption> OPTIONS = EnumSet.of(CatalogOption.BASIC, CatalogOption.CATEGORIES,
			CatalogOption.SUBCATEGORIES);

	@Resource(name = "cwsCatalogFacade")
	private CatalogFacade catalogFacade;
	@Resource(name = "fieldSetBuilder")
	private FieldSetBuilder fieldSetBuilder;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getCatalogs", summary = "Get a list of catalogs", description = "Returns all catalogs with versions defined for the base store.")
	@ApiBaseSiteIdParam
	public CatalogListWsDTO getCatalogs(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final List<CatalogData> catalogDataList = catalogFacade.getAllProductCatalogsForCurrentSite(OPTIONS);
		final CatalogsData catalogsData = new CatalogsData();
		catalogsData.setCatalogs(catalogDataList);

		final FieldSetBuilderContext context = new FieldSetBuilderContext();
		context.setRecurrencyLevel(countRecurrecyLevel(catalogDataList));
		final Set<String> fieldSet = fieldSetBuilder.createFieldSet(CatalogListWsDTO.class, DataMapper.FIELD_PREFIX, fields,
				context);

		return getDataMapper().map(catalogsData, CatalogListWsDTO.class, fieldSet);
	}

	@RequestMapping(value = "/{catalogId}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getCatalog", summary = "Get a catalog", description = "Returns information about a catalog based on its ID, along with the versions defined for the current base store.")
	@ApiBaseSiteIdParam
	public CatalogWsDTO getCatalog(
			@Parameter(description = "Catalog identifier", required = true) @PathVariable final String catalogId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CatalogData catalogData = catalogFacade.getProductCatalogForCurrentSite(catalogId, OPTIONS);

		final FieldSetBuilderContext context = new FieldSetBuilderContext();
		context.setRecurrencyLevel(countRecurrencyForCatalogData(catalogData));
		final Set<String> fieldSet = fieldSetBuilder.createFieldSet(CatalogWsDTO.class, DataMapper.FIELD_PREFIX, fields, context);

		return getDataMapper().map(catalogData, CatalogWsDTO.class, fieldSet);
	}

	@RequestMapping(value = "/{catalogId}/{catalogVersionId}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getCatalogVersion", summary = "Get information about catalog version", description = "Returns information about the catalog version that exists for the current base store.")
	@ApiBaseSiteIdParam
	public CatalogVersionWsDTO getCatalogVersion(
			@Parameter(description = "Catalog identifier", required = true) @PathVariable final String catalogId,
			@Parameter(description = "Catalog version identifier", required = true) @PathVariable final String catalogVersionId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CatalogVersionData catalogVersionData = catalogFacade.getProductCatalogVersionForTheCurrentSite(catalogId,
				catalogVersionId, OPTIONS);

		final FieldSetBuilderContext context = new FieldSetBuilderContext();
		context.setRecurrencyLevel(countRecurrencyForCatalogVersionData(catalogVersionData));
		final Set<String> fieldSet = fieldSetBuilder.createFieldSet(CatalogVersionWsDTO.class, DataMapper.FIELD_PREFIX, fields,
				context);

		return getDataMapper().map(catalogVersionData, CatalogVersionWsDTO.class, fieldSet);
	}

	@RequestMapping(value = "/{catalogId}/{catalogVersionId}/categories/{categoryId}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getCategories", summary = "Get information about catagory in a catalog version", description = "Returns information about a specified category that exists in a catalog version available for the current base store.")
	@ApiBaseSiteIdParam
	public CategoryHierarchyWsDTO getCategories(
			@Parameter(description = "Catalog identifier", required = true) @PathVariable final String catalogId,
			@Parameter(description = "Catalog version identifier", required = true) @PathVariable final String catalogVersionId,
			@Parameter(description = "Category identifier", required = true) @PathVariable final String categoryId,
			@ApiFieldsParam @RequestParam(defaultValue = "DEFAULT") final String fields)
	{
		final PageOption page = PageOption.createForPageNumberAndPageSize(0, 10);
		final CategoryHierarchyData categoryHierarchyData = catalogFacade.getCategoryById(catalogId, catalogVersionId, categoryId,
				page, OPTIONS);

		final FieldSetBuilderContext context = new FieldSetBuilderContext();
		context.setRecurrencyLevel(countRecurrencyForCategoryHierarchyData(1, categoryHierarchyData));
		final Set<String> fieldSet = fieldSetBuilder.createFieldSet(CategoryHierarchyWsDTO.class, DataMapper.FIELD_PREFIX, fields,
				context);

		return getDataMapper().map(categoryHierarchyData, CategoryHierarchyWsDTO.class, fieldSet);
	}

	protected int countRecurrecyLevel(final List<CatalogData> catalogDataList)
	{
		int recurrencyLevel = 1;
		int value;
		for (final CatalogData catalog : catalogDataList)
		{
			value = countRecurrencyForCatalogData(catalog);
			if (value > recurrencyLevel)
			{
				recurrencyLevel = value;
			}
		}
		return recurrencyLevel;
	}

	protected int countRecurrencyForCatalogData(final CatalogData catalog)
	{
		int retValue = 1;
		int value;
		for (final CatalogVersionData version : catalog.getCatalogVersions())
		{
			value = countRecurrencyForCatalogVersionData(version);
			if (value > retValue)
			{
				retValue = value;
			}
		}
		return retValue;
	}

	protected int countRecurrencyForCatalogVersionData(final CatalogVersionData catalogVersion)
	{
		int retValue = 1;
		int value;
		for (final CategoryHierarchyData hierarchy : catalogVersion.getCategoriesHierarchyData())
		{
			value = countRecurrencyForCategoryHierarchyData(1, hierarchy);
			if (value > retValue)
			{
				retValue = value;
			}
		}
		return retValue;
	}

	protected int countRecurrencyForCategoryHierarchyData(final int currentValue, final CategoryHierarchyData hierarchy)
	{
		int calculatedValue = currentValue + 1;
		int subcategoryRecurrencyValue;
		for (final CategoryHierarchyData subcategory : hierarchy.getSubcategories())
		{
			subcategoryRecurrencyValue = countRecurrencyForCategoryHierarchyData(calculatedValue, subcategory);
			if (subcategoryRecurrencyValue > calculatedValue)
			{
				calculatedValue = subcategoryRecurrencyValue;
			}
		}
		return calculatedValue;
	}
}
