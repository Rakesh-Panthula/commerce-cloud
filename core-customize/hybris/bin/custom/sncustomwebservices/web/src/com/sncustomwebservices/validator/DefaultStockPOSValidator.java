/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.platform.commercefacades.product.data.StockData;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.LowStockException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.ProductLowStockException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.StockSystemException;
import com.sncustomwebservices.stock.CommerceStockFacade;

import static de.hybris.platform.basecommerce.enums.StockLevelStatus.LOWSTOCK;
import static de.hybris.platform.basecommerce.enums.StockLevelStatus.OUTOFSTOCK;
import static de.hybris.platform.commercewebservicescommons.errors.exceptions.LowStockException.LOW_STOCK;
import static de.hybris.platform.commercewebservicescommons.errors.exceptions.LowStockException.NO_STOCK;
import static de.hybris.platform.webservicescommons.util.YSanitizer.sanitize;


/**
 * Implementation of stock validator that validates stock availability for the product in the point of service.
 */
public class DefaultStockPOSValidator extends DefaultStockSystemValidator implements StockPOSValidator
{
	/**
	 * Creates an instance of the validator
	 *
	 * @param commerceStockFacade Commerce stock facade to be used in the validation
	 */
	public DefaultStockPOSValidator(final CommerceStockFacade commerceStockFacade)
	{
		super(commerceStockFacade);
	}

	/**
	 * Validates stock availability for the product in the given point of service.<p>
	 * Throws {@link StockSystemException} in case stock system is unavailable for the base site.<p>
	 * Throws {@link LowStockException} in case entry stock is insufficient, includes information about entry number as an error subject.
	 * Throws {@link ProductLowStockException} in case product stock is insufficient.
	 *
	 * @param baseSiteId  Id of base site
	 * @param productCode Code of the product
	 * @param storeName   Name of the point of service
	 * @param entryNumber Number of entry in the cart
	 */
	@Override
	public void validate(final String baseSiteId, final String productCode, final String storeName, final Long entryNumber)
	{
		validate(baseSiteId);

		final StockData stock = getCommerceStockFacade().getStockDataForProductAndPointOfService(productCode, storeName);

		if (stock != null && stock.getStockLevelStatus().equals(OUTOFSTOCK))
		{
			if (entryNumber != null)
			{
				throw new LowStockException("Product [" + sanitize(productCode) + "] is currently out of stock", NO_STOCK,
						entryNumber.toString());
			}
			else
			{
				throw new ProductLowStockException("Product [" + sanitize(productCode) + "] is currently out of stock", NO_STOCK,
						productCode);
			}
		}
		else if (stock != null && stock.getStockLevelStatus().equals(LOWSTOCK))
		{
			if (entryNumber != null)
			{
				throw new LowStockException("Not enough product in stock", LOW_STOCK, entryNumber.toString());
			}
			else
			{
				throw new ProductLowStockException("Not enough product in stock", LOW_STOCK, productCode);
			}
		}
	}
}
