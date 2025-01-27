/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.data.AddToCartParams;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationStatus;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ProductWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartEntryGroupException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import com.sncustomwebservices.validator.StockValidator;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Unit test for {@link CartEntryGroupsController}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class CartEntryGroupsControllerTest
{
	private static final String BASE_SITE = "myBaseSite";
	private static final String PRODUCT_CODE = "MY_PRODUCT_CODE";
	private static final long QUANTITY = 1L;
	private static final int GROUP_NUMBER = 1;
	private static final String FIELDS = "MY_FIELDS";
	private final CartModificationData data = new CartModificationData();
	private final CartModificationWsDTO wsDTO = new CartModificationWsDTO();

	@Mock
	private DataMapper dataMapper;
	@Mock
	private CartFacade cartFacade;
	@Mock
	private Validator addToCartEntryGroupValidator;
	@Mock
	private Validator greaterThanZeroValidator;
	@Mock
	private StockValidator stockValidator;
	@InjectMocks
	private CartEntryGroupsController controller;
	@Captor
	private ArgumentCaptor<AddToCartParams> addToCartParamsCaptor;

	@Before
	public void setUp()
	{
		final CartData cart = new CartData();
		lenient().when(cartFacade.getSessionCart()).thenReturn(cart);
		when(dataMapper.map(data, CartModificationWsDTO.class, FIELDS)).thenReturn(wsDTO);
	}

	@Test
	public void testAddToCartEntryGroup() throws CommerceCartModificationException
	{
		when(cartFacade.addToCart(any(AddToCartParams.class))).thenReturn(data);

		final ProductWsDTO product = new ProductWsDTO();
		product.setCode(PRODUCT_CODE);
		final OrderEntryWsDTO entry = new OrderEntryWsDTO();
		entry.setProduct(product);
		entry.setQuantity(QUANTITY);

		final CartModificationWsDTO cartModificationWsDTO = controller.addToCartEntryGroup(BASE_SITE, GROUP_NUMBER, entry, FIELDS);

		verify(dataMapper).map(data, CartModificationWsDTO.class, FIELDS);
		verify(addToCartEntryGroupValidator).validate(any(), any());
		verify(greaterThanZeroValidator).validate(any(), any());
		verify(stockValidator).validate(BASE_SITE, PRODUCT_CODE, null);

		verify(cartFacade).addToCart(addToCartParamsCaptor.capture());

		assertThat(cartModificationWsDTO).isSameAs(wsDTO);
		assertThat(addToCartParamsCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("storeId", BASE_SITE)
				.hasFieldOrPropertyWithValue("productCode", PRODUCT_CODE).hasFieldOrPropertyWithValue("quantity", QUANTITY)
				.hasFieldOrPropertyWithValue("entryGroupNumbers", Set.of(GROUP_NUMBER));
	}

	@Test
	public void testAddToCartEntryGroupWithNullQuantity() throws CommerceCartModificationException
	{
		when(cartFacade.addToCart(any(AddToCartParams.class))).thenReturn(data);

		final ProductWsDTO product = new ProductWsDTO();
		product.setCode(PRODUCT_CODE);
		final OrderEntryWsDTO entry = new OrderEntryWsDTO();
		entry.setProduct(product);
		entry.setQuantity(null);

		final CartModificationWsDTO cartModificationWsDTO = controller.addToCartEntryGroup(BASE_SITE, GROUP_NUMBER, entry, FIELDS);

		verify(dataMapper).map(data, CartModificationWsDTO.class, FIELDS);
		verify(addToCartEntryGroupValidator).validate(any(), any());
		verify(greaterThanZeroValidator).validate(any(), any());
		verify(stockValidator).validate(BASE_SITE, PRODUCT_CODE, null);

		verify(cartFacade).addToCart(addToCartParamsCaptor.capture());

		assertThat(cartModificationWsDTO).isSameAs(wsDTO);
		assertThat(addToCartParamsCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("storeId", BASE_SITE)
				.hasFieldOrPropertyWithValue("productCode", PRODUCT_CODE).hasFieldOrPropertyWithValue("quantity", 1L)
				.hasFieldOrPropertyWithValue("entryGroupNumbers", Set.of(GROUP_NUMBER));
	}

	@Test
	public void testRemoveEntryGroup() throws CommerceCartModificationException
	{
		when(cartFacade.removeEntryGroup(GROUP_NUMBER)).thenReturn(data);

		controller.removeEntryGroup(GROUP_NUMBER);

		verify(greaterThanZeroValidator).validate(any(), any());
		verify(cartFacade).removeEntryGroup(GROUP_NUMBER);
	}

	@Test
	public void testRemoveNotExistingEntryGroupShouldThrowException() throws CommerceCartModificationException
	{
		final CartModificationData data = new CartModificationData();
		data.setStatusCode(CommerceCartModificationStatus.INVALID_ENTRY_GROUP_NUMBER);

		when(cartFacade.removeEntryGroup(GROUP_NUMBER)).thenReturn(data);

		// when
		final Throwable raisedException = catchThrowable(() -> controller.removeEntryGroup(GROUP_NUMBER));

		// then
		assertThat(raisedException).isInstanceOf(CartEntryGroupException.class).hasMessageContaining("Entry group not found");

		verify(greaterThanZeroValidator).validate(any(), any());
	}
}
