/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestWsDTO;

import java.util.List;

import org.junit.Test;
import org.mockito.InjectMocks;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SkipReturnRequestListFieldValueSetterTest extends BaseSkipFieldTest
{
	@InjectMocks
	private SkipReturnRequestListFieldValueSetter skipReturnRequestListFieldValueSetter;

	@Test
	public void testSetValue()
	{
		final ReturnRequestListWsDTO listWsDTO = new ReturnRequestListWsDTO();
		final ReturnRequestWsDTO requestWsDTO = new ReturnRequestWsDTO();
		requestWsDTO.setOrder(new OrderWsDTO());
		listWsDTO.setReturnRequests(List.of(requestWsDTO));
		when(dataMapper.map(anyObject(),eq(ReturnRequestListWsDTO.class), anyString())).thenReturn(listWsDTO);
		skipReturnRequestListFieldValueSetter.setValue(FIELD_ENTRIES);
		verify(sessionService).setAttribute(SKIP_ORDER_ENTRYGROUPS, true);
	}

}
