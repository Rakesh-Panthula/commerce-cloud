/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.storelocator.data.PointOfServiceData;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ProductWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartEntryException;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;


/**
 * Unit test for {@link CartEntriesController}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class CartEntriesControllerTest
{
	private static final long ENTRY_NUMBER_PARAMETER = 1L;
	private static final Integer ENTRY_NUMBER = 1;
	private static final Integer UNKNOWN_ENTRY_NUMBER = 2;
	private static final String PRODUCT_CODE = "12345";
	private static final String PICKUP_STORE = "TestStore";
	private static final String UNKNOWN_PRODUCT_CODE = "UnknownProduct";
	private static final String UNKNOWN_PICKUP_STORE = "UnknownStore";

	@Mock
	private PointOfServiceData pointOfService;
	@Mock
	private ProductData product;
	@Mock
	private OrderEntryData cartEntry;
	@Mock
	private CartData cart;
	@Mock
	private OrderEntryWsDTO cartEntryWsDTO;
	@Mock
	private ProductWsDTO productWsDTO;

	protected static void assertCorrectException(final CartEntryException actualException)
	{
		assertThat(actualException).hasMessage("Entry not found")
				.hasFieldOrPropertyWithValue("reason", CartEntryException.NOT_FOUND)
				.hasFieldOrPropertyWithValue("subject", ENTRY_NUMBER.toString());
	}

	@Before
	public void setUp()
	{
		when(pointOfService.getName()).thenReturn(PICKUP_STORE);
		when(cartEntry.getDeliveryPointOfService()).thenReturn(pointOfService);
		when(product.getCode()).thenReturn(PRODUCT_CODE);
		when(cartEntry.getProduct()).thenReturn(product);
		final List<OrderEntryData> entries = Arrays.asList(null, cartEntry);
		when(cart.getEntries()).thenReturn(entries);
		when(productWsDTO.getCode()).thenReturn(PRODUCT_CODE);
		when(cartEntryWsDTO.getProduct()).thenReturn(productWsDTO);
	}

	@Test
	public void testGetCartEntryForNumberWhenNoEntryList()
	{
		final CartEntryException cartEntryException = assertThrows(CartEntryException.class,
				() -> CartEntriesController.getCartEntryForNumber(cart, ENTRY_NUMBER_PARAMETER));

		assertCorrectException(cartEntryException);
	}

	@Test
	public void testGetCartEntryForNumberWhenEmptyEntryList()
	{
		when(cart.getEntries()).thenReturn(Collections.emptyList());

		final CartEntryException cartEntryException = assertThrows(CartEntryException.class,
				() -> CartEntriesController.getCartEntryForNumber(cart, ENTRY_NUMBER_PARAMETER));

		assertCorrectException(cartEntryException);
	}

	@Test
	public void testGetCartEntryForNumberWhenInvalidEntries()
	{
		when(cartEntry.getEntryNumber()).thenReturn(UNKNOWN_ENTRY_NUMBER);

		final CartEntryException actualException = assertThrows(CartEntryException.class,
				() -> CartEntriesController.getCartEntryForNumber(cart, ENTRY_NUMBER_PARAMETER));

		assertCorrectException(actualException);
	}

	@Test
	public void testGetCartEntryForNumberWhenCorrectEntry()
	{
		when(cartEntry.getEntryNumber()).thenReturn(ENTRY_NUMBER);

		final OrderEntryData actualCartEntry = CartEntriesController.getCartEntryForNumber(cart, ENTRY_NUMBER_PARAMETER);

		assertThat(actualCartEntry).isNotNull().hasFieldOrPropertyWithValue("entryNumber", ENTRY_NUMBER);
	}
	
	@Test
	public void testGetCartEntryWhenCorrectProductAndStore()
	{
		final OrderEntryData actualEntry = CartEntriesController.getCartEntry(cart, PRODUCT_CODE, PICKUP_STORE);

		assertThat(actualEntry).isNotNull().hasFieldOrProperty("product").hasFieldOrProperty("deliveryPointOfService");
		assertThat(actualEntry.getProduct()).hasFieldOrPropertyWithValue("code", PRODUCT_CODE);
		assertThat(actualEntry.getDeliveryPointOfService()).hasFieldOrPropertyWithValue("name", PICKUP_STORE);
	}

	@Test
	public void testGetCartEntryWhenCorrectProductNoStores()
	{
		when(cartEntry.getDeliveryPointOfService()).thenReturn(null);

		final OrderEntryData actualEntry = CartEntriesController.getCartEntry(cart, PRODUCT_CODE, null);

		assertThat(actualEntry).isNotNull().hasFieldOrProperty("product");
		assertThat(actualEntry.getProduct()).hasFieldOrPropertyWithValue("code", PRODUCT_CODE);
		assertThat(actualEntry.getDeliveryPointOfService()).isNull();
	}

	@Test
	public void testGetCartEntryWhenNoEntryProduct()
	{
		when(cartEntry.getProduct()).thenReturn(null);

		final OrderEntryData actualEntry = CartEntriesController.getCartEntry(cart, PRODUCT_CODE, PICKUP_STORE);

		assertThat(actualEntry).isNull();
	}

	@Test
	public void testGetCartEntryWhenNoEntryProductCode()
	{
		when(product.getCode()).thenReturn(null);

		final OrderEntryData actualEntry = CartEntriesController.getCartEntry(cart, PRODUCT_CODE, PICKUP_STORE);

		assertThat(actualEntry).isNull();
	}

	@Test
	public void testGetCartEntryWhenNullProductCode()
	{
		final OrderEntryData actualEntry = CartEntriesController.getCartEntry(cart, null, PICKUP_STORE);

		assertThat(actualEntry).isNull();
	}

	@Test
	public void testGetCartEntryWhenUnknownProductCode()
	{
		final OrderEntryData actualEntry = CartEntriesController.getCartEntry(cart, UNKNOWN_PRODUCT_CODE, PICKUP_STORE);

		assertThat(actualEntry).isNull();
	}

	@Test
	public void testGetCartEntryWhenUnknownStore()
	{
		final OrderEntryData actualEntry = CartEntriesController.getCartEntry(cart, PRODUCT_CODE, UNKNOWN_PICKUP_STORE);

		assertThat(actualEntry).isNull();
	}

	@Test
	public void testValidateProductCode()
	{
		CartEntriesController.validateProductCode(cartEntry, cartEntryWsDTO);
	}

	@Test
	public void testValidateNullProduct()
	{
		when(cartEntryWsDTO.getProduct()).thenReturn(null);

		CartEntriesController.validateProductCode(cartEntry, cartEntryWsDTO);
	}

	@Test
	public void testValidateNullProductCode()
	{
		when(productWsDTO.getCode()).thenReturn(null);

		CartEntriesController.validateProductCode(cartEntry, cartEntryWsDTO);
	}

	@Test
	public void testValidateProductCodeWhenUnknownProduct()
	{
		when(productWsDTO.getCode()).thenReturn(UNKNOWN_PRODUCT_CODE);

		assertThrows(WebserviceValidationException.class,
				() -> CartEntriesController.validateProductCode(cartEntry, cartEntryWsDTO));
	}
}
