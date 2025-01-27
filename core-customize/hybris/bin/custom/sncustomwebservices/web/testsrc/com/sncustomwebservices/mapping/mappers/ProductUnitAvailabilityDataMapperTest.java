/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.mapping.mappers;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.enums.StockLevelStatus;
import de.hybris.platform.commercefacades.product.data.SapUnitAvailabilityData;
import de.hybris.platform.commercewebservicescommons.dto.availability.SapUnitAvailabilityWsDTO;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class ProductUnitAvailabilityDataMapperTest
{
	@InjectMocks
	private ProductUnitAvailabilityDataMapper mapper;

	private SapUnitAvailabilityData sapUnitAvailabilityData;

	private SapUnitAvailabilityWsDTO sapUnitAvailabilityWsDTO;

	@Before
	public void init()
	{
		sapUnitAvailabilityData = new SapUnitAvailabilityData();
		sapUnitAvailabilityData.setUnit("PC");
		sapUnitAvailabilityWsDTO = new SapUnitAvailabilityWsDTO();
	}

	@Test
	public void test_convert_should_return_IN_STOCK()
	{
		sapUnitAvailabilityData.setStatus(StockLevelStatus.INSTOCK);
		sapUnitAvailabilityData.setQuantity(10l);

		mapper.mapAtoB(sapUnitAvailabilityData, sapUnitAvailabilityWsDTO, null);

		Assert.assertEquals("IN_STOCK", sapUnitAvailabilityWsDTO.getStatus());
		Assert.assertEquals("PC", sapUnitAvailabilityWsDTO.getUnit());
		Assert.assertEquals(10l, sapUnitAvailabilityWsDTO.getQuantity().longValue());
	}

	@Test
	public void test_convert_should_return_LOW_STOCK()
	{
		sapUnitAvailabilityData.setStatus(StockLevelStatus.LOWSTOCK);
		sapUnitAvailabilityData.setQuantity(1l);

		mapper.mapAtoB(sapUnitAvailabilityData, sapUnitAvailabilityWsDTO, null);

		Assert.assertEquals("LOW_STOCK", sapUnitAvailabilityWsDTO.getStatus());
		Assert.assertEquals("PC", sapUnitAvailabilityWsDTO.getUnit());
		Assert.assertEquals(1l, sapUnitAvailabilityWsDTO.getQuantity().longValue());
	}

	@Test
	public void test_convert_should_return_OUT_OF_STOCK()
	{
		sapUnitAvailabilityData.setStatus(StockLevelStatus.OUTOFSTOCK);
		sapUnitAvailabilityData.setQuantity(0l);

		mapper.mapAtoB(sapUnitAvailabilityData, sapUnitAvailabilityWsDTO, null);

		Assert.assertEquals("OUT_OF_STOCK", sapUnitAvailabilityWsDTO.getStatus());
		Assert.assertEquals("PC", sapUnitAvailabilityWsDTO.getUnit());
		Assert.assertEquals(0l, sapUnitAvailabilityWsDTO.getQuantity().longValue());
	}
}
