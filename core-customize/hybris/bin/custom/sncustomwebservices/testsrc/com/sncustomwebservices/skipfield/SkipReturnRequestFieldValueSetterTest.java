/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestWsDTO;

import org.junit.Test;
import org.mockito.InjectMocks;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SkipReturnRequestFieldValueSetterTest extends BaseSkipFieldTest
{
	@InjectMocks
	private SkipReturnRequestFieldValueSetter skipReturnRequestFieldValueSetter;

	@Test
	public void testSetValue()
	{
		final ReturnRequestWsDTO requestWsDTO = new ReturnRequestWsDTO();
		requestWsDTO.setOrder(new OrderWsDTO());
		when(dataMapper.map(anyObject(), eq(ReturnRequestWsDTO.class), anyString())).thenReturn(requestWsDTO);
		skipReturnRequestFieldValueSetter.setValue(FIELD_ENTRIES);
		verify(sessionService).setAttribute(SKIP_ORDER_ENTRYGROUPS, true);
	}

}
