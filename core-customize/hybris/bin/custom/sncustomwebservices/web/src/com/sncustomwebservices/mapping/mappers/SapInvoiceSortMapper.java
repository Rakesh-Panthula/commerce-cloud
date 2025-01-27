/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.mapping.mappers;

import de.hybris.platform.commercewebservicescommons.dto.order.SAPInvoiceWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.SAPInvoicesWsDTO;
import de.hybris.platform.core.servicelayer.data.SearchPageData;
import de.hybris.platform.webservicescommons.dto.SortWsDTO;
import de.hybris.platform.webservicescommons.mapping.config.FieldMapper;
import de.hybris.platform.webservicescommons.mapping.mappers.AbstractCustomMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ma.glasnost.orika.MappingContext;


public class SapInvoiceSortMapper extends AbstractCustomMapper<SearchPageData, SAPInvoicesWsDTO>
{
	protected static final String SAP_INVOICE_FIELD_MAPPER_BEAN = "sapInvoiceFieldMapper";
	protected static final String FILED_INVOICES = "invoices";
	protected static final String FILED_RESULTS = "results";
	protected static final String SORT_SPLITTER = ":";

	private ApplicationContext applicationContext;


	@Override
	public void mapAtoB(final SearchPageData a, final SAPInvoicesWsDTO b, final MappingContext context)
	{
		context.beginMappingField(FILED_RESULTS, getAType(), a, FILED_INVOICES, getBType(), b);
		try
		{
			if (shouldMap(a, b, context) && a.getResults() != null)
			{
				b.setInvoices(mapperFacade.mapAsList(a.getResults(), SAPInvoiceWsDTO.class, context));
			}
		}
		finally
		{
			context.endMappingField();
		}

		if (b != null && b.getSorts() != null)
		{
			final Map<String, String> filedMappings = getSapInvoiceFiledMappings();
			final List<SortWsDTO> updatedSorts = b.getSorts().stream()
					.map(sort -> {
						final String mappedCode = filedMappings.get(sort.getCode());
						if (Objects.nonNull(mappedCode) && !mappedCode.isEmpty())
						{
							sort.setCode(mappedCode);
						}
						return sort;
					}).toList();
			b.setSorts(updatedSorts);
		}
	}


	public final String mapSort(final String sort)
	{
		if (StringUtils.isBlank(sort))
		{
			return StringUtils.EMPTY;
		}
		final String[] sortInput = sort.split(SORT_SPLITTER);

		final Map<String, String> filedMappings = getSapInvoiceFiledMappings();
		final String mappedSortCode = filedMappings.entrySet().stream().filter(entry -> sortInput[0].equals(entry.getValue()))
				.map(Map.Entry::getKey).findFirst().orElse(sortInput[0]);

		if (mappedSortCode != null)
		{
			if (sortInput.length > 1)
			{
				return String.join(SORT_SPLITTER, mappedSortCode, sortInput[1]);
			}
			else
			{
				return mappedSortCode;
			}
		}

		return StringUtils.EMPTY;
	}


	protected Map<String, String> getSapInvoiceFiledMappings()
	{
		final FieldMapper sapInvoiceListFieldMapper = getApplicationContext().getBean(SAP_INVOICE_FIELD_MAPPER_BEAN,
				FieldMapper.class);
		return sapInvoiceListFieldMapper.getFieldMapping();
	}



	protected ApplicationContext getApplicationContext()
	{
		return applicationContext;
	}

	@Autowired
	public void setApplicationContext(final ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}


}
