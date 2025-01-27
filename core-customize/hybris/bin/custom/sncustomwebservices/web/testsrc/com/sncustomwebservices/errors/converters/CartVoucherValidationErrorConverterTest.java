/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.errors.converters;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.webservicescommons.dto.error.ErrorWsDTO;
import com.sncustomwebservices.validation.data.CartVoucherValidationData;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.MessageSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;


/**
 * Test suite for {@link CartVoucherValidationErrorConverter}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class CartVoucherValidationErrorConverterTest
{
	private static final String APPLIED_VOUCHER_EXPIRED = "applied.voucher.expired";
	private static final String EXPIRED_VOUCHER_CODE = "expiredVoucherCode";
	private static final String TYPE = "cartVoucherError";
	private static final String SUBJECT_TYPE = "voucher";
	private static final String REASON_INVALID = "expired";

	@Mock
	private I18NService i18NService;
	@Mock
	private MessageSource messageSource;

	private CartVoucherValidationData validationData;

	private CartVoucherValidationErrorConverter converter;

	@Before
	public void setUp()
	{
		converter = new CartVoucherValidationErrorConverter(i18NService, messageSource);

		validationData = new CartVoucherValidationData();
		validationData.setSubject(EXPIRED_VOUCHER_CODE);

		given(messageSource.getMessage(eq(APPLIED_VOUCHER_EXPIRED), any(Object[].class), anyString(), nullable(Locale.class)))
				.willReturn(APPLIED_VOUCHER_EXPIRED);
	}

	@Test
	public void testPopulate()
	{
		final ErrorWsDTO errorWsDTO = new ErrorWsDTO();
		converter.populate(validationData, errorWsDTO);

		Assert.assertEquals(TYPE, errorWsDTO.getType());
		Assert.assertEquals(SUBJECT_TYPE, errorWsDTO.getSubjectType());
		Assert.assertEquals(EXPIRED_VOUCHER_CODE, errorWsDTO.getSubject());
		Assert.assertEquals(REASON_INVALID, errorWsDTO.getReason());
		Assert.assertEquals(APPLIED_VOUCHER_EXPIRED, errorWsDTO.getMessage());
	}
}
