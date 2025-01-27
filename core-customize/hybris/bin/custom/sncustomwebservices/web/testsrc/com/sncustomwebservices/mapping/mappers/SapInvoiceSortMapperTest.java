/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.mapping.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.invoice.data.SAPInvoiceData;
import de.hybris.platform.commercewebservicescommons.dto.order.SAPInvoiceWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.SAPInvoicesWsDTO;
import de.hybris.platform.core.servicelayer.data.SearchPageData;
import de.hybris.platform.webservicescommons.dto.SortWsDTO;
import de.hybris.platform.webservicescommons.mapping.FieldSelectionStrategy;
import de.hybris.platform.webservicescommons.mapping.config.FieldMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MappingContext;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class SapInvoiceSortMapperTest
{
	private static final String NOT_MAPPED_FIELD = "invalidField:asc";
	private static final String SAP_INVOICE_FIELD_MAPPER_BEAN = "sapInvoiceFieldMapper";
	private static final String INPUT_FIELD_ASC = "mappedField:asc";
	private static final String MAPPED_FIELD = "mappedField";
	private static final String ORIGINAL_FIELD = "originalField";
	private static final String ORIGINAL_FIELD_ASC = "originalField:asc";
	private static final String INVOICE_ID = "1";


	@Spy
	@InjectMocks
	private SapInvoiceSortMapper sapInvoiceSortMapper;

	@Mock
   private FieldMapper fieldMapper;
	@Mock
	private ApplicationContext applicationContext;
	@Mock
	private MappingContext context;
	@Mock
	FieldSelectionStrategy fieldSelectionStrategy;
	@Mock
	private MapperFacade mapperFacade;

	@Before
	public void initialize()
	{
		sapInvoiceSortMapper.setApplicationContext(applicationContext);
		when(fieldSelectionStrategy.shouldMap(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
	}

	@Test
	public void testMapSortWithBlankInput()
	{
		final String result = sapInvoiceSortMapper.mapSort(StringUtils.EMPTY);
		assertEquals(StringUtils.EMPTY, result);
	}

	@Test
	public void testMapSortWithValidInputAndMapping()
	{
		final Map<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put(ORIGINAL_FIELD, MAPPED_FIELD);
		sapInvoiceSortMapper.setApplicationContext(mockedApplicationContext(fieldMapper));
		when(fieldMapper.getFieldMapping()).thenReturn(fieldMappings);

		final String result = sapInvoiceSortMapper.mapSort(INPUT_FIELD_ASC);

		assertEquals(ORIGINAL_FIELD_ASC, result);
	}

	@Test
	public void testMapSortWithValidInputAndNoMapping()
	{
		final Map<String, String> fieldMappings = new HashMap<>();
		sapInvoiceSortMapper.setApplicationContext(mockedApplicationContext(fieldMapper));
		when(fieldMapper.getFieldMapping()).thenReturn(fieldMappings);

		final String input = INPUT_FIELD_ASC;
		final String result = sapInvoiceSortMapper.mapSort(input);

		assertEquals(INPUT_FIELD_ASC, result);
	}

	@Test
	public void testMapSortWithValidInputAndNoSortingDirection()
	{
		final Map<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put(ORIGINAL_FIELD, MAPPED_FIELD);
		sapInvoiceSortMapper.setApplicationContext(mockedApplicationContext(fieldMapper));
		when(fieldMapper.getFieldMapping()).thenReturn(fieldMappings);

		final String input = MAPPED_FIELD;
		final String result = sapInvoiceSortMapper.mapSort(input);

		assertEquals(ORIGINAL_FIELD, result);
	}

	@Test
	public void testMapSortWithValidInputAndMappingButNoSortInput()
	{
		final Map<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put(ORIGINAL_FIELD, MAPPED_FIELD);
		sapInvoiceSortMapper.setApplicationContext(mockedApplicationContext(fieldMapper));
		when(fieldMapper.getFieldMapping()).thenReturn(fieldMappings);


		final String input = NOT_MAPPED_FIELD;
		final String result = sapInvoiceSortMapper.mapSort(input);

		assertEquals(NOT_MAPPED_FIELD, result);
	}

	@Test
	public void getSapInvoiceFiledMappingsShouldReturnMappings()
	{
		final Map<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put(MAPPED_FIELD, ORIGINAL_FIELD);
		sapInvoiceSortMapper.setApplicationContext(mockedApplicationContext(fieldMapper));
		when(fieldMapper.getFieldMapping()).thenReturn(fieldMappings);

		final Map<String, String> result = sapInvoiceSortMapper.getSapInvoiceFiledMappings();
		assertEquals(fieldMappings, result);
	}


	@Test
	public void mapAtoBShouldMapInvoices()
	{
		final SAPInvoiceData sAPInvoiceData = new SAPInvoiceData();
		sAPInvoiceData.setInvoiceId(INVOICE_ID);
		final SearchPageData searchPageData = new SearchPageData();
		searchPageData.setResults(Collections.singletonList(sAPInvoiceData));
		final SAPInvoicesWsDTO sapInvoicesWsDTO = new SAPInvoicesWsDTO();
		final SAPInvoiceWsDTO invoiceWsDTO = new SAPInvoiceWsDTO();
		invoiceWsDTO.setInvoiceId(INVOICE_ID);
		sapInvoicesWsDTO.setInvoices(Collections.singletonList(invoiceWsDTO));
		when(mapperFacade.mapAsList(Mockito.anyList(), Mockito.any(), Mockito.any(MappingContext.class)))
				.thenReturn(Collections.singletonList(invoiceWsDTO));
		sapInvoiceSortMapper.mapAtoB(searchPageData, sapInvoicesWsDTO, context);

		assertNotNull(sapInvoicesWsDTO.getInvoices());
		assertEquals(INVOICE_ID, sapInvoicesWsDTO.getInvoices().get(0).getInvoiceId());
	}

	@Test
	public void mapAtoBShouldUpdateSorts()
	{
		final SAPInvoicesWsDTO sapInvoicesWsDTO = new SAPInvoicesWsDTO();
		final SortWsDTO sortWsDTO = new SortWsDTO();
		sortWsDTO.setCode(MAPPED_FIELD);
		sapInvoicesWsDTO.setSorts(Arrays.asList(sortWsDTO));
		final SearchPageData searchPageData = new SearchPageData();
		searchPageData.setResults(null);
		final Map<String, String> fieldMappings = new HashMap<>();
		fieldMappings.put(MAPPED_FIELD, ORIGINAL_FIELD);

		sapInvoiceSortMapper.setApplicationContext(mockedApplicationContext(fieldMapper));
		when(fieldMapper.getFieldMapping()).thenReturn(fieldMappings);

		sapInvoiceSortMapper.mapAtoB(searchPageData, sapInvoicesWsDTO, context);
		assertNotNull(sapInvoicesWsDTO.getSorts());
		assertEquals(ORIGINAL_FIELD, sapInvoicesWsDTO.getSorts().get(0).getCode());

	}

	private ApplicationContext mockedApplicationContext(final FieldMapper fieldMapper)
	{
      when(applicationContext.getBean(SAP_INVOICE_FIELD_MAPPER_BEAN, FieldMapper.class)).thenReturn(fieldMapper);
      return applicationContext;
	}

}
