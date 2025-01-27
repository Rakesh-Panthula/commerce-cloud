/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.helper;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commerceservices.enums.SearchQueryContext;
import de.hybris.platform.commerceservices.search.facetdata.ProductCategorySearchPageData;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.FilterQueryOperator;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchFilterQueryData;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchQueryData;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductCategorySearchPageWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductSearchPageWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import com.sncustomwebservices.util.ws.SearchQueryCodec;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;


@Component
public class ProductsHelper extends AbstractHelper
{
	private static final int NEXT_FILTER = 2;
	private static final String IDENTIFIER_FIELD_KEY = "code";

	@Resource(name = "productSearchFacade")
	private ProductSearchFacade<ProductData> productSearchFacade;
	@Resource(name = "cwsSearchQueryCodec")
	private SearchQueryCodec<SolrSearchQueryData> searchQueryCodec;
	@Resource(name = "solrSearchStateConverter")
	private Converter<SolrSearchQueryData, SearchStateData> solrSearchStateConverter;

	/**
	 * @deprecated since 6.6. Please use {@link #searchProducts(String, int, int, String, String, String)} instead.
	 */
	@Deprecated(since = "6.6", forRemoval = true)
	public ProductSearchPageWsDTO searchProducts(final String query, final int currentPage, final int pageSize, final String sort,
			final String fields)
	{
		final ProductSearchPageData<SearchStateData, ProductData> sourceResult = searchProducts(query, currentPage, pageSize, sort);
		if (sourceResult instanceof ProductCategorySearchPageData)
		{
			return getDataMapper().map(sourceResult, ProductCategorySearchPageWsDTO.class, fields);
		}

		return getDataMapper().map(sourceResult, ProductSearchPageWsDTO.class, fields);
	}

	public ProductSearchPageData<SearchStateData, ProductData> searchProducts(final String query, final int currentPage,
			final int pageSize, final String sort)
	{
		final SolrSearchQueryData searchQueryData = searchQueryCodec.decodeQuery(query);
		final PageableData pageable = createPageableData(currentPage, pageSize, sort);

		return productSearchFacade.textSearch(solrSearchStateConverter.convert(searchQueryData), pageable);
	}

	public ProductSearchPageWsDTO searchProducts(final String filters, final String query, final int currentPage, final int pageSize, final String sort,
			final String fields, final String searchQueryContext)
	{
		if (StringUtils.isEmpty(filters))
		{
			return searchProducts(query, currentPage, pageSize, sort, fields, searchQueryContext);
		}

		final SearchQueryContext context = decodeContext(searchQueryContext);

		final ProductSearchPageData<SearchStateData, ProductData> sourceResult = searchProducts(filters, query, currentPage, pageSize, sort,
				context);
		if (sourceResult instanceof ProductCategorySearchPageData)
		{
			return getDataMapper().map(sourceResult, ProductCategorySearchPageWsDTO.class, fields);
		}

		return getDataMapper().map(sourceResult, ProductSearchPageWsDTO.class, fields);
	}

	/**
	 * @deprecated since 2211.28. Please use {@link ProductsHelper#searchProducts(String, String, int, int, String, String, String)}
	 *             instead.
	 */
	@Deprecated(since = "2211.28", forRemoval = true)
	public ProductSearchPageWsDTO searchProducts(final String query, final int currentPage, final int pageSize, final String sort,
												 final String fields, final String searchQueryContext)
	{
		final SearchQueryContext context = decodeContext(searchQueryContext);

		final ProductSearchPageData<SearchStateData, ProductData> sourceResult = searchProducts(query, currentPage, pageSize, sort,
				context);
		if (sourceResult instanceof ProductCategorySearchPageData)
		{
			return getDataMapper().map(sourceResult, ProductCategorySearchPageWsDTO.class, fields);
		}

		return getDataMapper().map(sourceResult, ProductSearchPageWsDTO.class, fields);
	}

	protected ProductSearchPageData<SearchStateData, ProductData> searchProducts(final String filters, final String query, final int currentPage,
			final int pageSize, final String sort, final SearchQueryContext searchQueryContext)
	{
		final SolrSearchQueryData searchQueryData = searchQueryCodec.decodeQuery(query);
		var filterList = decodeFilters(filters);
		searchQueryData.setFilterQueries(filterList);
		searchQueryData.setSearchQueryContext(searchQueryContext);

		final PageableData pageable = createPageableData(currentPage, pageSize, sort);

		var searchData =  productSearchFacade.textSearch(solrSearchStateConverter.convert(searchQueryData), pageable);
		adjustProductsOrderByInput(filterList, query, sort, searchData);
		return searchData;
	}

	protected void adjustProductsOrderByInput(final List<SolrSearchFilterQueryData> filterList, final String query, final String sort, final ProductSearchPageData<SearchStateData, ProductData> data)
	{
		if (!StringUtils.isEmpty(query) || !StringUtils.isEmpty(sort) || data.getPagination().getNumberOfPages() > 1)
		{
			return;
		}
		var expectedOrder = getExpectedOrder(filterList);
		if (expectedOrder.isEmpty() )
		{
			return;
		}
		final List<ProductData> orderedProducts = new ArrayList<>();

		for(var code : expectedOrder)
		{
			for(var product : data.getResults())
			{
				if (code.equals(product.getCode()))
				{
					orderedProducts.add(product);
					break;
				}
			}
		}

		data.setResults(orderedProducts);
	}

	private List<String> getExpectedOrder(List<SolrSearchFilterQueryData> filterList)
	{
		List<String> codes = new ArrayList<>();

		for(var filter : filterList)
		{
			if ((IDENTIFIER_FIELD_KEY).equals(filter.getKey()))
			{
				codes.addAll(new ArrayList<>(filter.getValues()));
				break;
			}
		}
		return codes;
	}

	/**
	 * @deprecated since 2211.28. Please use {@link ProductsHelper#searchProducts(String, String, int, int, String, String, String)}
	 *             instead.
	 */
	@Deprecated(since = "2211.28", forRemoval = true)
	protected ProductSearchPageData<SearchStateData, ProductData> searchProducts(final String query, final int currentPage,
																				 final int pageSize, final String sort, final SearchQueryContext searchQueryContext)
	{
		final SolrSearchQueryData searchQueryData = searchQueryCodec.decodeQuery(query);
		searchQueryData.setSearchQueryContext(searchQueryContext);

		final PageableData pageable = createPageableData(currentPage, pageSize, sort);

		return productSearchFacade.textSearch(solrSearchStateConverter.convert(searchQueryData), pageable);
	}

	protected SearchQueryContext decodeContext(final String searchQueryContext)
	{
		if (StringUtils.isBlank(searchQueryContext))
		{
			return null;
		}

		try
		{
			return SearchQueryContext.valueOf(searchQueryContext);
		}
		catch (final IllegalArgumentException e)
		{
			throw new RequestParameterException(searchQueryContext + " context does not exist", RequestParameterException.INVALID,
					e);
		}
	}

	protected List<SolrSearchFilterQueryData> decodeFilters(String filters) {
		if (StringUtils.isEmpty(filters))
		{
			return Lists.emptyList();
		}

		final String[] parts = filters.split(":");
		final List<SolrSearchFilterQueryData> filterQueries = new ArrayList<>();
		final Map<String, Set<String>> filterMap = new HashMap<>();
		for (int i = 0; i < parts.length; i = i + NEXT_FILTER)
		{
			final Set<String> values = filterMap.computeIfAbsent(parts[i], k -> new LinkedHashSet<>());
			if (parts.length <= i + 1)
			{
				throw new RequestParameterException("Invalid filters format. You have to provide filters as '<attributeKey1>:<attributeValue1>:...:<attributeKeyN>:<attributeValueN>'",
						RequestParameterException.INVALID);
			}
			values.addAll(Arrays.asList(parts[i + 1].split(",")));
		}
		filterMap.entrySet().stream().forEach(entry -> {
			final SolrSearchFilterQueryData filter = new SolrSearchFilterQueryData();
			filter.setKey(entry.getKey());
			filter.setValues(entry.getValue());
			filter.setOperator(FilterQueryOperator.OR);
			filterQueries.add(filter);
		});
		return filterQueries;
	}
}
