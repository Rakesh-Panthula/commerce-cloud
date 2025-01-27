/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

/**
 * Validator of product stock online availability
 */
public interface StockValidator
{
	/**
	 * Validates product stock online availability for the given site and entry number.<p>
	 * Throws sub type of {@link de.hybris.platform.webservicescommons.errors.exceptions.WebserviceException} in case the stock system is unavailable or stock is insufficient.
	 *
	 * @param baseSiteId  Id of base site
	 * @param productCode Code of the product
	 * @param entryNumber Number of entry in case of entry validation
	 */
	void validate(final String baseSiteId, final String productCode, final Long entryNumber);
}
