/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationWsDTO;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import com.sncustomwebservices.validation.data.CartVoucherValidationData;
import com.sncustomwebservices.validator.CartVoucherValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;


/**
 * Unit test for {@link CartsController}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class CartsControllerTest
{
	private static final String REJECTED_VOUCHER_CODE_1 = "123-abc";
	private static final String REJECTED_VOUCHER_CODE_2 = "456-def";
	private static final String FIELDS = "MY_FIELDS";
	private static final String NO_STOCK = "noStock";
	private static final String COUPON_STATUS_CODE = "couponNotValid";
	private static final String VOUCHER_STATUS_CODE = "voucherNotValid";
	private final CartModificationData data = new CartModificationData();
	private final CartModificationWsDTO wsDTO = new CartModificationWsDTO();
	private final List<String> voucherList = new ArrayList<>();

	@Mock
	private DataMapper dataMapper;
	@Mock
	private CartFacade cartFacade;
	@Mock
	private CartVoucherValidator cartVoucherValidator;
	@InjectMocks
	private CartsController controller;

	@Before
	public void setUp()
	{
		final CartData cart = new CartData();
		cart.setAppliedVouchers(voucherList);
		given(cartFacade.getSessionCart()).willReturn(cart);
		lenient().when(dataMapper.map(data, CartModificationWsDTO.class, FIELDS)).thenReturn(wsDTO);
	}

	@Test(expected = CommerceCartModificationException.class)
	public void testValidateCartException() throws CommerceCartModificationException
	{
		given(cartFacade.validateCartData()).willThrow(new CommerceCartModificationException("TEST TEST TEST"));
		controller.validateCart(FieldSetLevelHelper.DEFAULT_LEVEL);
	}

	@Test
	public void testValidateCartOk() throws CommerceCartModificationException
	{
		//given
		given(cartFacade.validateCartData()).willReturn(Collections.emptyList());
		final CartModificationListWsDTO noErrorsResult = new CartModificationListWsDTO();
		noErrorsResult.setCartModifications(Collections.emptyList());
		final Predicate<CartModificationDataList> listShouldBeEmpty = list -> list.getCartModificationList() != null && list
				.getCartModificationList().isEmpty();
		given(dataMapper.map(argThat(new CartValidationArgumentMatcher(listShouldBeEmpty)), same(CartModificationListWsDTO.class),
				same(FieldSetLevelHelper.DEFAULT_LEVEL))).willReturn(noErrorsResult);
		//when
		final CartModificationListWsDTO response = controller.validateCart(FieldSetLevelHelper.DEFAULT_LEVEL);
		//then
		assertTrue("No modifications expected", response.getCartModifications().isEmpty());
	}

	@Test
	public void testValidateCartNoStock() throws CommerceCartModificationException
	{
		//given
		final CartModificationData noStock = createCartModificationData(NO_STOCK);
		given(cartFacade.validateCartData()).willReturn(Collections.singletonList(noStock));

		final CartModificationWsDTO noStockResponse = createCartModificationDTO(NO_STOCK, null);
		final CartModificationListWsDTO resultWithNoStockError = new CartModificationListWsDTO();
		resultWithNoStockError.setCartModifications(Collections.singletonList(noStockResponse));

		final Predicate<CartModificationDataList> listContainsNoStock = list -> list.getCartModificationList() != null &&//
				list.getCartModificationList().stream().allMatch(modification -> NO_STOCK.equals(modification.getStatusCode()));

		given(dataMapper.map(argThat(new CartValidationArgumentMatcher(listContainsNoStock)), same(CartModificationListWsDTO.class),
				same(FieldSetLevelHelper.DEFAULT_LEVEL))).willReturn(resultWithNoStockError);
		//when
		final CartModificationListWsDTO response = controller.validateCart(FieldSetLevelHelper.DEFAULT_LEVEL);
		//then
		assertEquals("One modification expected", 1, response.getCartModifications().size());
	}

	@Test
	public void testValidateVoucherRejected() throws CommerceCartModificationException
	{
		//given
		given(cartVoucherValidator.validate(anyList()))
				.willReturn(List.of(createCartVoucherData(REJECTED_VOUCHER_CODE_1), createCartVoucherData(REJECTED_VOUCHER_CODE_2)));

		given(cartFacade.validateCartData())
				.willReturn(List.of(createCartModificationData(VOUCHER_STATUS_CODE), createCartModificationData(COUPON_STATUS_CODE)));

		final CartModificationWsDTO firstValidationVoucherResponse = createCartModificationDTO(VOUCHER_STATUS_CODE,
				REJECTED_VOUCHER_CODE_1);
		final CartModificationWsDTO secondValidationVoucherResponse = createCartModificationDTO(COUPON_STATUS_CODE, null);
		final CartModificationListWsDTO validationResult = new CartModificationListWsDTO();
		validationResult.setCartModifications(List.of(firstValidationVoucherResponse, secondValidationVoucherResponse));

		final Predicate<CartModificationDataList> listWithVouchers = list -> list.getCartModificationList() != null && list
				.getCartModificationList().stream()
				.allMatch(modification -> COUPON_STATUS_CODE.equals(modification.getStatusCode()) &&//
						StringUtils.isNotBlank(modification.getStatusMessage()));

		given(dataMapper.map(argThat(new CartValidationArgumentMatcher(listWithVouchers)), same(CartModificationListWsDTO.class),
				same(FieldSetLevelHelper.DEFAULT_LEVEL))).willReturn(validationResult);
		//when
		final CartModificationListWsDTO response = controller.validateCart(FieldSetLevelHelper.DEFAULT_LEVEL);
		//then
		assertEquals("Two vouchers expected", 2, response.getCartModifications().size());
	}

	private CartModificationWsDTO createCartModificationDTO(final String statusCode, final String statusMessage)
	{
		final CartModificationWsDTO dto = new CartModificationWsDTO();
		dto.setStatusCode(statusCode);
		dto.setStatusMessage(statusMessage);
		return dto;
	}

	private CartModificationData createCartModificationData(final String statusCode)
	{
		final CartModificationData data = new CartModificationData();
		data.setStatusCode(statusCode);
		return data;
	}

	private CartVoucherValidationData createCartVoucherData(final String subject)
	{
		final CartVoucherValidationData data = new CartVoucherValidationData();
		data.setSubject(subject);
		return data;
	}

	private static class CartValidationArgumentMatcher implements ArgumentMatcher<CartModificationDataList>
	{
		private final Predicate<CartModificationDataList> filter;

		public CartValidationArgumentMatcher(final Predicate<CartModificationDataList> allMatchFilter)
		{
			this.filter = allMatchFilter;
		}

		@Override
		public boolean matches(final CartModificationDataList argument)
		{
			return filter.test(argument);
		}
	}
}
