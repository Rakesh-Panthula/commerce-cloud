/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.platform.commercefacades.product.data.StockData;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.LowStockException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.ProductLowStockException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.StockSystemException;
import com.sncustomwebservices.stock.CommerceStockFacade;

import static de.hybris.platform.basecommerce.enums.StockLevelStatus.OUTOFSTOCK;
import static de.hybris.platform.commercewebservicescommons.errors.exceptions.LowStockException.NO_STOCK;
import static de.hybris.platform.webservicescommons.util.YSanitizer.sanitize;


/**
 * Implementation of online stock validator that validates online product stock availability for the given site and entry number
 */
public class DefaultStockValidator extends DefaultStockSystemValidator implements StockValidator
{
	/**
	 * Creates an instance of the validator
	 *
	 * @param commerceStockFacade Commerce stock facade to be used in the validation
	 */
	public DefaultStockValidator(final CommerceStockFacade commerceStockFacade)
	{
		super(commerceStockFacade);
	}

	/**
	 * Validates the online product stock availability for the given site and entry number.<p>
	 * Throws {@link StockSystemException} in case stock system is unavailable.<p>
	 * Throws {@link LowStockException} in case entry stock is insufficient, includes information about entry number as an error subject.
	 * Throws {@link ProductLowStockException} in case product stock is insufficient.
	 *
	 * @param baseSiteId  Id of base site
	 * @param productCode Code of the product
	 * @param entryNumber Number of entry in the cart
	 */
	@Override
	public void validate(final String baseSiteId, final String productCode, final Long entryNumber)
	{
		validate(baseSiteId);

		final StockData stock = getCommerceStockFacade().getStockDataForProductAndBaseSite(productCode, baseSiteId);
		if (stock != null && stock.getStockLevelStatus().equals(OUTOFSTOCK))
		{
			if (entryNumber != null)
			{
				throw new LowStockException("Product [" + sanitize(productCode) + "] cannot be shipped - out of stock online",
						NO_STOCK, entryNumber.toString());
			}
			else
			{
				throw new ProductLowStockException("Product [" + sanitize(productCode) + "] cannot be shipped - out of stock online",
						NO_STOCK, productCode);
			}
		}
	}
}
