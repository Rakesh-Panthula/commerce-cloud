/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

/**
 * Validator of product stock availability in the point of service.
 */
public interface StockPOSValidator
{
	/**
	 * Validates product stock availability in the point of service.<p>
	 * Uses entry number in validation message.
	 * Throws sub type of {@link de.hybris.platform.webservicescommons.errors.exceptions.WebserviceException} in case the stock system is unavailable or stock is insufficient.
	 *
	 * @param baseSiteId  Site identifier
	 * @param productCode Code of the product
	 * @param storeName   Name of the point of service
	 * @param entryNumber Number of entry in case of entry validation
	 */
	void validate(final String baseSiteId, final String productCode, final String storeName, final Long entryNumber);
}
