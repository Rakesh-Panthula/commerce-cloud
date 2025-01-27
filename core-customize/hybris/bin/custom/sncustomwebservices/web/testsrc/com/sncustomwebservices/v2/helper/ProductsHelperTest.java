/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.helper;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PaginationData;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchFilterQueryData;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Test;

public class ProductsHelperTest {
    @Test
    public void testDecodeFiltersWhenValidFilters()
    {
        ProductsHelper productsHelper = new ProductsHelper();

        String filters = "code:123,456:branName:sony:code:456,789";
        var filterQueries = productsHelper.decodeFilters(filters);

        assertEquals(2, filterQueries.size());
        assertEquals("code", filterQueries.get(0).getKey());
        assertTrue(filterQueries.get(0).getValues().contains("123"));
        assertTrue(filterQueries.get(0).getValues().contains("456"));
        assertTrue(filterQueries.get(0).getValues().contains("789"));
        assertEquals(3, filterQueries.get(0).getValues().size());
        assertEquals("branName", filterQueries.get(1).getKey());
        assertTrue(filterQueries.get(1).getValues().contains("sony"));
        assertEquals(1, filterQueries.get(1).getValues().size());
    }

    @Test
    public void testDecodeFiltersWhenEmptyFilters()
    {
        ProductsHelper productsHelper = new ProductsHelper();
        assertTrue(productsHelper.decodeFilters("").isEmpty());
    }

    @Test
    public void testDecodeFiltersWhenInvalidFilters()
    {
        ProductsHelper productsHelper = new ProductsHelper();

        assertThrows(RequestParameterException.class, () -> {
            productsHelper.decodeFilters("code:");
        });
        assertThrows(RequestParameterException.class, () -> {
            productsHelper.decodeFilters("id::816323");
        });
    }

    @Test
    public void testAdjustProductsOrderByInputWhenNeedAdjust()
    {
        ProductsHelper productsHelper = new ProductsHelper();
        var data = mockProductSearchPageData();

        productsHelper.adjustProductsOrderByInput(mockFilters(), null, null, data);

        assertEquals(2, data.getResults().size());
        assertEquals("2", data.getResults().get(0).getCode());
    }

    @Test
    public void testAdjustProductsOrderByInputWhenNoEmptyQuery()
    {
        ProductsHelper productsHelper = new ProductsHelper();
        var data = mockProductSearchPageData();

        productsHelper.adjustProductsOrderByInput(mockFilters(), "camel", null, data);

        assertEquals(2, data.getResults().size());
        assertEquals("1", data.getResults().get(0).getCode());
    }

    @Test
    public void testAdjustProductsOrderByInputWhenNoEmptySort()
    {
        ProductsHelper productsHelper = new ProductsHelper();
        var data = mockProductSearchPageData();

        productsHelper.adjustProductsOrderByInput(mockFilters(), null, "price-asc", data);

        assertEquals(2, data.getResults().size());
        assertEquals("1", data.getResults().get(0).getCode());
    }

    @Test
    public void testAdjustProductsOrderByInputWhenMoreThanOnePage()
    {
        ProductsHelper productsHelper = new ProductsHelper();
        var data = mockProductSearchPageData();
        data.getPagination().setNumberOfPages(2);

        productsHelper.adjustProductsOrderByInput(mockFilters(), null, null, data);

        assertEquals(2, data.getResults().size());
        assertEquals("1", data.getResults().get(0).getCode());
    }

    private List<SolrSearchFilterQueryData> mockFilters()
    {
        SolrSearchFilterQueryData filter = new SolrSearchFilterQueryData();
        filter.setKey("code");
        LinkedHashSet values = new LinkedHashSet();
        values.add("2");
        values.add("1");
        filter.setValues(values);
        return List.of(filter);
    }

    private ProductSearchPageData<SearchStateData, ProductData> mockProductSearchPageData()
    {
        ProductSearchPageData<SearchStateData, ProductData> data = new ProductSearchPageData<>();
        PaginationData pagination = new PaginationData();
        pagination.setNumberOfPages(1);
        data.setPagination(pagination);
        ProductData p1 = new ProductData();
        p1.setCode("1");
        ProductData p2 = new ProductData();
        p2.setCode("2");
        data.setResults(List.of(p1, p2));
        return data;
    }
}
