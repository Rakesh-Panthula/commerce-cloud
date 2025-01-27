/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.platform.commercewebservicescommons.errors.exceptions.StockSystemException;
import com.sncustomwebservices.stock.CommerceStockFacade;


/**
 * Stock system validator that allows to check whether stock system is available
 */
public class DefaultStockSystemValidator implements StockSystemValidator
{
	private final CommerceStockFacade commerceStockFacade;

	/**
	 * Creates an instance of the validator
	 *
	 * @param commerceStockFacade Commerce stock facade to be used in the validation
	 */
	public DefaultStockSystemValidator(final CommerceStockFacade commerceStockFacade)
	{
		this.commerceStockFacade = commerceStockFacade;
	}

	/**
	 * Verifies if stock system is available for given site. Throws {@link StockSystemException} in case system is unavailable.
	 *
	 * @param baseSiteId site identifier
	 */
	@Override
	public void validate(final String baseSiteId)
	{
		if (!commerceStockFacade.isStockSystemEnabled(baseSiteId))
		{
			throw new StockSystemException("Stock system is not enabled on this site", StockSystemException.NOT_ENABLED, baseSiteId);
		}
	}

	protected CommerceStockFacade getCommerceStockFacade()
	{
		return commerceStockFacade;
	}
}
