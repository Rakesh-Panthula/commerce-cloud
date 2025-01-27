/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;


import de.hybris.platform.commercewebservicescommons.dto.order.CartWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.SaveCartResultWsDTO;

import org.junit.Test;
import org.mockito.InjectMocks;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SkipSaveCartResultFieldValueSetterTest extends BaseSkipFieldTest
{
	@InjectMocks
	private SkipSaveCartResultFieldValueSetter skipSaveCartResultFieldValueSetter;

	@Test
	public void testSetValue()
	{
		final SaveCartResultWsDTO resultWsDTO = new SaveCartResultWsDTO();
		resultWsDTO.setSavedCartData(new CartWsDTO());
		when(dataMapper.map(anyObject(), eq(SaveCartResultWsDTO.class), anyString())).thenReturn(resultWsDTO);
		skipSaveCartResultFieldValueSetter.setValue(FIELD_ENTRIES);
		verify(sessionService).setAttribute(SKIP_ORDER_ENTRYGROUPS, true);
	}
}
