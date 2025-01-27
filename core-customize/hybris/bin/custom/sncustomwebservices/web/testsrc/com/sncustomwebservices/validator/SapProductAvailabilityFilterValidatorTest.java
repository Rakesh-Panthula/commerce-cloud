/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.product.data.SapProductAvailabilityQueryContext;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class SapProductAvailabilityFilterValidatorTest
{
	@InjectMocks
	private SapProductAvailabilityFilterValidator validator;

	@Test
	public void test_getProductAvailability_should_return_exception_when_filters_is_null()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters(null);
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals("The request isn’t allowed. The 'filters' field can’t be empty. Please provide a valid value.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_return_exception_when_filters_is_emtpy()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters("");
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals("The request isn’t allowed. The 'filters' field can’t be empty. Please provide a valid value.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_return_exception_when_filters_is_blank()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters("   ");
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals("The request isn’t allowed. The 'filters' field can’t be empty. Please provide a valid value.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_return_exception_when_filters_is_not_in_correct_pattern_unit_not_provided()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters("productA");
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals(
					"The request isn’t allowed because the provided value 'productA' doesn't adhere to the required format. Please provide product codes and their respective units in the 'filter' field using this format - productCodeA:unitCodeA,unitCodeB;productCodeB:unitCodeA,unitCodeB. For example: 3318057_A:EA,PC;4112097_B:EA.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_return_exception_when_filters_is_not_in_correct_pattern_unit_is_null()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters("productA:");
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals(
					"The request isn’t allowed because the provided value 'productA:' doesn't adhere to the required format. Please provide product codes and their respective units in the 'filter' field using this format - productCodeA:unitCodeA,unitCodeB;productCodeB:unitCodeA,unitCodeB. For example: 3318057_A:EA,PC;4112097_B:EA.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_return_exception_when_filters_is_not_in_correct_pattern_unit_is_blank()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters("productA:  ");
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals(
					"The request isn’t allowed because the provided value 'productA:  ' doesn't adhere to the required format. Please provide product codes and their respective units in the 'filter' field using this format - productCodeA:unitCodeA,unitCodeB;productCodeB:unitCodeA,unitCodeB. For example: 3318057_A:EA,PC;4112097_B:EA.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_return_exception_when_filters_is_not_in_correct_pattern_productCode_is_blank()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters("   :PC");
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals(
					"The request isn’t allowed because the provided value '   :PC' doesn't adhere to the required format. Please provide product codes and their respective units in the 'filter' field using this format - productCodeA:unitCodeA,unitCodeB;productCodeB:unitCodeA,unitCodeB. For example: 3318057_A:EA,PC;4112097_B:EA.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_get_ProductAvailability_should_return_exception_when_filters_is_not_correct_unit_is_blank()
	{
		boolean error = false;
		try
		{
			validator.decodeAndValidateFilters("productA:PC, ,EA");
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals(
					"The request isn’t allowed because the provided value 'productA:PC, ,EA' doesn't adhere to the required format. Please provide product codes and their respective units in the 'filter' field using this format - productCodeA:unitCodeA,unitCodeB;productCodeB:unitCodeA,unitCodeB. For example: 3318057_A:EA,PC;4112097_B:EA.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_return_exception_when_quantity_of_products_is_51()
	{
		boolean error = false;
		try
		{
			String filters = generateProductWithUnitWithGivenQuantity(51);
			validator.decodeAndValidateFilters(filters);
		}
		catch (RequestParameterException e)
		{
			Assert.assertEquals("The request isn’t allowed. The maximum number of product types to be retrieved is 50.",
					e.getMessage());
			error = true;
		}
		Assert.assertTrue(error);
	}

	@Test
	public void test_getProductAvailability_should_ignore_unit_when_same_unit_is_provided_more_than_one_time()
	{
		String filters = "productA:PC;productB:PC;productA:PC";

		SapProductAvailabilityQueryContext queryContext = validator.decodeAndValidateFilters(filters);

		Assert.assertNotNull(queryContext);
		Assert.assertNotNull(queryContext.getProducts());
		Assert.assertEquals(2, queryContext.getProducts().size());

		Assert.assertEquals("productA", queryContext.getProducts().get(0).getProductCode());
		Assert.assertEquals(1, queryContext.getProducts().get(0).getUnits().size());
		Assert.assertEquals("PC", queryContext.getProducts().get(0).getUnits().get(0));

		Assert.assertEquals("productB", queryContext.getProducts().get(1).getProductCode());
		Assert.assertEquals(1, queryContext.getProducts().get(1).getUnits().size());
		Assert.assertEquals("PC", queryContext.getProducts().get(1).getUnits().get(0));
	}

	@Test
	public void test_getProductAvailability_should_generate_context_when_quantity_of_products_is_50_and_is_valid_pattern()
	{
		String filters = generateProductWithUnitWithGivenQuantity(50);

		SapProductAvailabilityQueryContext queryContext = validator.decodeAndValidateFilters(filters);

		Assert.assertNotNull(queryContext);
		Assert.assertNotNull(queryContext.getProducts());
		Assert.assertEquals(50, queryContext.getProducts().size());

		for (int i = 0; i < 50; i++)
		{
			Assert.assertEquals("product" + i, queryContext.getProducts().get(i).getProductCode());
			Assert.assertEquals(1, queryContext.getProducts().get(i).getUnits().size());
			Assert.assertEquals("PC", queryContext.getProducts().get(i).getUnits().get(0));
		}
	}


	@Test
	public void test_getProductAvailability_should_call_facade_when_filter_is_in_pattern_and_has_duplicate_products()
	{
		String filters = generateProductWithUnitWithGivenQuantity(1);
		filters = filters + "product0:BX";

		SapProductAvailabilityQueryContext queryContext = validator.decodeAndValidateFilters(filters);

		Assert.assertNotNull(queryContext);
		Assert.assertNotNull(queryContext.getProducts());
		Assert.assertEquals(1, queryContext.getProducts().size());

		Assert.assertEquals("product0", queryContext.getProducts().get(0).getProductCode());
		Assert.assertEquals(2, queryContext.getProducts().get(0).getUnits().size());
		Assert.assertEquals("PC", queryContext.getProducts().get(0).getUnits().get(0));
		Assert.assertEquals("BX", queryContext.getProducts().get(0).getUnits().get(1));
	}


	private String generateProductWithUnitWithGivenQuantity(int quantity)
	{
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < quantity; i++)
		{
			stringBuffer.append("product" + i);
			stringBuffer.append(":PC;");
		}
		return stringBuffer.toString();
	}
}
