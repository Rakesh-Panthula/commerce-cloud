/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.product.availability.SapProductAvailabilityFacade;
import de.hybris.platform.commercefacades.product.data.SapAvailabilityData;
import de.hybris.platform.commercefacades.product.data.SapProductAvailabilityQueryContext;
import de.hybris.platform.commercewebservicescommons.dto.availability.SapAvailabilityWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.servicelayer.exceptions.SystemException;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import com.sncustomwebservices.validator.SapProductAvailabilityFilterValidator;

import javax.ws.rs.InternalServerErrorException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class ProductAvailabilityControllerTest
{
	@InjectMocks
	private ProductAvailabilityController controller;

	@Mock
	private SapProductAvailabilityFacade facade;

	@Mock
	private DataMapper dataMapper;

	@Mock
	private SapProductAvailabilityFilterValidator validator;


	@Test
	public void test_getProductAvailability_should_thrown_exception_when_validate_failed()
	{
		String filters = "productA:unit1,productB:unit2";
		Mockito.doThrow(RequestParameterException.class).when(validator).decodeAndValidateFilters(filters);
		boolean error = false;
		try
		{
			controller.getProductAvailability(filters);
		}
		catch (RequestParameterException e)
		{
			error = true;
		}

		Assert.assertTrue(error);
		Mockito.verify(validator, Mockito.times(1)).decodeAndValidateFilters(filters);
		Mockito.verify(facade, Mockito.times(0)).getAvailabilityForProductsAndUnits(any());
		Mockito.verify(dataMapper, Mockito.times(0)).map(any(), eq(SapAvailabilityWsDTO.class));
	}

	@Test
	public void test_getProductAvailability_should_thrown_exception_when_facade_failed()
	{
		String filters = "productA:unit1;productB:unit2";
		SapProductAvailabilityQueryContext context = mock(SapProductAvailabilityQueryContext.class);

		Mockito.when(validator.decodeAndValidateFilters(filters)).thenReturn(context);
		Mockito.doThrow(InternalServerErrorException.class).when(facade).getAvailabilityForProductsAndUnits(context);

		boolean error = false;
		try
		{
			controller.getProductAvailability(filters);
		}
		catch (InternalServerErrorException e)
		{
			error = true;
		}

		Assert.assertTrue(error);
		Mockito.verify(validator, Mockito.times(1)).decodeAndValidateFilters(filters);
		Mockito.verify(facade, Mockito.times(1)).getAvailabilityForProductsAndUnits(any());
		Mockito.verify(dataMapper, Mockito.times(0)).map(any(), eq(SapAvailabilityWsDTO.class));
	}


	@Test
	public void test_getProductAvailability_should_success_when_filters_is_valid_and_query_success()
	{
		String filters = "productA:unit1;productB:unit2";
		SapProductAvailabilityQueryContext context = mock(SapProductAvailabilityQueryContext.class);
		SapAvailabilityData sapAvailabilityData = mock(SapAvailabilityData.class);
		SapAvailabilityWsDTO sapAvailabilityWsDTO = mock(SapAvailabilityWsDTO.class);
		Mockito.when(validator.decodeAndValidateFilters(filters)).thenReturn(context);
		Mockito.when(facade.getAvailabilityForProductsAndUnits(context)).thenReturn(sapAvailabilityData);
		Mockito.when(dataMapper.map(sapAvailabilityData, SapAvailabilityWsDTO.class)).thenReturn(sapAvailabilityWsDTO);

		SapAvailabilityWsDTO result = controller.getProductAvailability(filters);

		Mockito.verify(validator, Mockito.times(1)).decodeAndValidateFilters(filters);
		Mockito.verify(facade, Mockito.times(1)).getAvailabilityForProductsAndUnits(context);
		Mockito.verify(dataMapper, Mockito.times(1)).map(sapAvailabilityData, SapAvailabilityWsDTO.class);

		Assert.assertEquals(sapAvailabilityWsDTO, result);
	}


	@Test
	public void test_handleInternalServerError()
	{
		SystemException systemException = new SystemException("error message");

		ErrorListWsDTO errorListWsDTO = controller.handleInternalServerError(systemException);

		Assert.assertNotNull(errorListWsDTO);
		Assert.assertNotNull(errorListWsDTO.getErrors());
		Assert.assertEquals(1, errorListWsDTO.getErrors().size());
		Assert.assertEquals("error message", errorListWsDTO.getErrors().get(0).getMessage());
		Assert.assertEquals("SystemError", errorListWsDTO.getErrors().get(0).getType());
	}
}
