/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.platform.commercewebservicescommons.errors.exceptions.StockSystemException;


/**
 * Validator of product stock online availability
 */
public interface StockSystemValidator
{
	/**
	 * Verifies if stock system is available for given site. Throws {@link StockSystemException} in case system is unavailable.
	 *
	 * @param baseSiteId site identifier
	 */
	void validate(final String baseSiteId);
}
