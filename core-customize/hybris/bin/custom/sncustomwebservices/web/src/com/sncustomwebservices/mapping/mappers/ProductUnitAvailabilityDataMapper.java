/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.mapping.mappers;

import de.hybris.platform.basecommerce.enums.StockLevelStatus;
import de.hybris.platform.commercefacades.product.data.SapUnitAvailabilityData;
import de.hybris.platform.commercewebservicescommons.dto.availability.SapUnitAvailabilityWsDTO;
import de.hybris.platform.webservicescommons.mapping.mappers.AbstractCustomMapper;

import ma.glasnost.orika.MappingContext;


public class ProductUnitAvailabilityDataMapper extends AbstractCustomMapper<SapUnitAvailabilityData, SapUnitAvailabilityWsDTO>
{
	private static final String IN_STOCK = "IN_STOCK";
	private static final String LOW_STOCK = "LOW_STOCK";
	private static final String OUT_OF_STOCK = "OUT_OF_STOCK";

	@Override
	public void mapAtoB(final SapUnitAvailabilityData a, final SapUnitAvailabilityWsDTO b, final MappingContext context)
	{
		b.setUnit(a.getUnit());
		b.setQuantity(a.getQuantity());
		
		if (StockLevelStatus.INSTOCK.equals(a.getStatus()))
		{
			b.setStatus(IN_STOCK);
		}
		else if (StockLevelStatus.LOWSTOCK.equals(a.getStatus()))
		{
			b.setStatus(LOW_STOCK);
		}
		else if (StockLevelStatus.OUTOFSTOCK.equals(a.getStatus()))
		{
			b.setStatus(OUT_OF_STOCK);
		}
	}
}
