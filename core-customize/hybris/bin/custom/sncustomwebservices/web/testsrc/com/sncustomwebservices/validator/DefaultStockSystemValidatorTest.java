/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.StockSystemException;
import com.sncustomwebservices.stock.CommerceStockFacade;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


/**
 * Unit test for {@link DefaultStockSystemValidator}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStockSystemValidatorTest
{
	private static final String BASE_SITE_ID = "testSite";

	@Mock
	private CommerceStockFacade commerceStockFacade;
	private DefaultStockSystemValidator validator;

	@Before
	public void setUp()
	{
		when(commerceStockFacade.isStockSystemEnabled(anyString())).thenReturn(true);
		this.validator = new DefaultStockSystemValidator(commerceStockFacade);
	}

	@Test
	public void testValidate()
	{
		validator.validate(BASE_SITE_ID);
	}

	@Test
	public void testValidateWhenSystemNotEnabled()
	{
		when(commerceStockFacade.isStockSystemEnabled(anyString())).thenReturn(false);

		final StockSystemException actualException = assertThrows(StockSystemException.class,
				() -> validator.validate(BASE_SITE_ID));

		assertThat(actualException).hasMessage("Stock system is not enabled on this site")
				.hasFieldOrPropertyWithValue("reason", StockSystemException.NOT_ENABLED)
				.hasFieldOrPropertyWithValue("subject", BASE_SITE_ID);
	}
}
