/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.platform.commercefacades.product.data.SapProductAvailabilityQueryContext;
import de.hybris.platform.commercefacades.product.data.SapProductAvailabilityQueryData;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;


@Component(value = "sapProductAvailabilityFilterValidator")
public class SapProductAvailabilityFilterValidator
{
	private static final String COMMA = ",";

	private static final String SEMICOLON = ";";

	private static final String COLON = ":";

	private static final Integer MAX_SUPPORTED_PRODUCT_QUANTITY = 50;

	private static final String PRODUCT_EXCEED_MAXIMUM_QUANTITY = "The request isn’t allowed. The maximum number of product types to be retrieved is 50.";

	private static final String FILTERS_IS_MANDATORY = "The request isn’t allowed. The 'filters' field can’t be empty. Please provide a valid value.";

	private static final String FILTERS_IS_INVALID = "The request isn’t allowed because the provided value '%s' doesn't adhere to the required format. Please provide product codes and their respective units in the 'filter' field using this format - productCodeA:unitCodeA,unitCodeB;productCodeB:unitCodeA,unitCodeB. For example: 3318057_A:EA,PC;4112097_B:EA.";

	private static final String FILTERS_FIELD = "filters";

	private static final Integer PART_NUMBER = 2;

	/**
	 * decode the filter and generate the query context
	 *
	 * @param filters the pattern of the filters should be productA:Unit1,Unit2,...,UnitX;productB:Unit1,Unit2,...,UnitY;....;productM:Unit1,Unit2,...,UnitZ
	 *                the max quantity of the product supported in one filter is 50.
	 * @return
	 */
	public SapProductAvailabilityQueryContext decodeAndValidateFilters(final String filters)
	{
		if (StringUtils.isBlank(filters))
		{
			throw new RequestParameterException(FILTERS_IS_MANDATORY, RequestParameterException.MISSING, FILTERS_FIELD);
		}

		String[] productAndUnitsStrs = StringUtils.split(filters, SEMICOLON);

		Map<String, SapProductAvailabilityQueryData> productAndQueryDataMap = new HashMap<>();

		SapProductAvailabilityQueryContext queryContext = new SapProductAvailabilityQueryContext();
		List<SapProductAvailabilityQueryData> queryDataList = new ArrayList<>();
		Arrays.stream(productAndUnitsStrs).forEach(
				productAndUnitsStr -> decodeSingeProductWithUnits(productAndUnitsStr, productAndQueryDataMap, queryDataList));

		if (productAndQueryDataMap.size() > MAX_SUPPORTED_PRODUCT_QUANTITY)
		{
			throw new RequestParameterException(PRODUCT_EXCEED_MAXIMUM_QUANTITY, RequestParameterException.INVALID, FILTERS_FIELD);
		}
		queryContext.setProducts(queryDataList);
		return queryContext;
	}

	/**
	 * decode one product with units, like "productA:Unit1,Unit2,...,UnitN"
	 *
	 * @param productAndUnitsStr
	 * @param productAndQueryDataMap
	 * @param queryDataList
	 */
	private void decodeSingeProductWithUnits(final String productAndUnitsStr,
			final Map<String, SapProductAvailabilityQueryData> productAndQueryDataMap,
			final List<SapProductAvailabilityQueryData> queryDataList)
	{
		final String[] productAndUnits = StringUtils.split(productAndUnitsStr, COLON);
		if (productAndUnits.length != PART_NUMBER)
		{
			throw new RequestParameterException(String.format(FILTERS_IS_INVALID, productAndUnitsStr),
					RequestParameterException.INVALID, FILTERS_FIELD);
		}

		String productCode = StringUtils.trimToNull(productAndUnits[0]);
		String unitsStr = StringUtils.trimToNull(productAndUnits[1]);
		if (productCode == null || unitsStr == null)
		{
			throw new RequestParameterException(String.format(FILTERS_IS_INVALID, productAndUnitsStr),
					RequestParameterException.INVALID, FILTERS_FIELD);
		}

		if (productAndQueryDataMap.containsKey(productCode))
		{
			decodeUnits(unitsStr, productAndUnitsStr, productAndQueryDataMap.get(productCode));
		}
		else
		{
			SapProductAvailabilityQueryData queryData = new SapProductAvailabilityQueryData();
			queryData.setProductCode(productCode);
			queryData.setUnits(new ArrayList<>());
			decodeUnits(unitsStr, productAndUnitsStr, queryData);
			queryDataList.add(queryData);
			productAndQueryDataMap.put(productCode, queryData);
		}
	}

	/**
	 * decode a list of units pattern as "unit1,unit2,...,unitN"
	 *
	 * @param unitsStr
	 * @param productAndUnitsStr
	 * @param queryData
	 */
	private void decodeUnits(final String unitsStr, final String productAndUnitsStr,
			final SapProductAvailabilityQueryData queryData)
	{
		String[] units = StringUtils.split(unitsStr, COMMA);
		Arrays.stream(units).forEach(unit -> {
			if (StringUtils.isBlank(unit))
			{
				throw new RequestParameterException(String.format(FILTERS_IS_INVALID, productAndUnitsStr),
						RequestParameterException.INVALID, FILTERS_FIELD);
			}
			if (!queryData.getUnits().contains(unit))
			{
				queryData.getUnits().add(StringUtils.trim(unit));
			}
		});
	}
}
