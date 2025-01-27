/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.voucher.VoucherFacade;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commerceservices.security.BruteForceAttackHandler;
import de.hybris.platform.commercewebservicescommons.dto.order.SAPVoucherRequestWsDTO;
import com.sncustomwebservices.exceptions.NoCheckoutCartException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Unit test for {@link CartPromotionsController}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class CartPromotionsControllerTest
{
	private static final String DEFAULT_VOUCHER_CODE = "defaultTestVoucherCode";
	private static final String DEFAULT_REMOTE_ADDRESS = "10.0.0.1:8080";

	@Mock
	private BruteForceAttackHandler bruteForceAttackHandler;
	@Mock
	private CheckoutFacade checkoutFacade;
	@Mock
	private VoucherFacade voucherFacade;
	@Mock
	private CartFacade cartFacade;
	@Mock
	private HttpServletRequest request;

	@Captor
	private ArgumentCaptor<String> applyVoucherParamsCaptor;

	@InjectMocks
	private CartPromotionsController cartPromotionsController;

	@Before
	public void setUp()
	{
		final CartData cart = new CartData();
		when(cartFacade.getSessionCart()).thenReturn(cart);

		when(request.getRemoteAddr()).thenReturn(DEFAULT_REMOTE_ADDRESS);
		when(checkoutFacade.hasCheckoutCart()).thenReturn(true);
	}

	@Test
	public void testDoApplyCartVoucherWhenBruteForceAttackShouldThrowException() throws NoCheckoutCartException
	{
		when(bruteForceAttackHandler.registerAttempt(anyString())).thenReturn(true);

		try
		{
			cartPromotionsController.doApplyCartVoucher(DEFAULT_VOUCHER_CODE, request);
		}
		catch (VoucherOperationException e)
		{
			Assert.assertTrue(e.getMessage().contains("You have entered too many voucher codes"));
			return;
		}

		fail("Should catch VoucherOperationException");
	}

	@Test
	public void testDoApplyCartVoucherWhenNotBruteForceAttackShouldApplyVoucher()
			throws VoucherOperationException, NoCheckoutCartException
	{
		when(bruteForceAttackHandler.registerAttempt(anyString())).thenReturn(false);

		cartPromotionsController.doApplyCartVoucher(DEFAULT_VOUCHER_CODE, request);

		verify(voucherFacade).applyVoucher(applyVoucherParamsCaptor.capture());

		assertEquals(DEFAULT_VOUCHER_CODE, applyVoucherParamsCaptor.getValue());
	}
	@Test
	public void testApplyVoucher() throws VoucherOperationException, NoCheckoutCartException
	{
		SAPVoucherRequestWsDTO voucher = new SAPVoucherRequestWsDTO();
		voucher.setVoucherId("voucherId");
		when(request.getRemoteAddr()).thenReturn(DEFAULT_REMOTE_ADDRESS);
		when(checkoutFacade.hasCheckoutCart()).thenReturn(true);
		HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request);
		cartPromotionsController.applyCartVoucher(voucher, requestWrapper);
		verify(voucherFacade).applyVoucher(voucher.getVoucherId());
		verify(cartFacade).getSessionCart();
	}

	@Test
	public void testRemoveVoucher() throws VoucherOperationException, NoCheckoutCartException
	{
		SAPVoucherRequestWsDTO voucher = new SAPVoucherRequestWsDTO();
		voucher.setVoucherId("voucherId");
		cartPromotionsController.doCartVoucherRemoval(voucher);
		verify(voucherFacade).releaseVoucher(voucher.getVoucherId());
	}

	@Test
	public void testRemoveVoucherInLogDebugMode() throws VoucherOperationException, NoCheckoutCartException
	{
		SAPVoucherRequestWsDTO voucher = new SAPVoucherRequestWsDTO();
		voucher.setVoucherId("voucherId");
		Logger.getLogger(CartPromotionsController.class).setLevel(Level.DEBUG);
		cartPromotionsController.doCartVoucherRemoval(voucher);
		verify(voucherFacade).releaseVoucher(voucher.getVoucherId());
	}

	@Test(expected = NoCheckoutCartException.class)
	public void testRemoveVoucherShouldThrowNoCheckoutCartException() throws VoucherOperationException, NoCheckoutCartException
	{
		SAPVoucherRequestWsDTO voucher = new SAPVoucherRequestWsDTO();
		voucher.setVoucherId("voucherId");
		when(checkoutFacade.hasCheckoutCart()).thenReturn(false);
		cartPromotionsController.doCartVoucherRemoval(voucher);
		verify(voucherFacade, times(0)).releaseVoucher(voucher.getVoucherId());
	}
}
