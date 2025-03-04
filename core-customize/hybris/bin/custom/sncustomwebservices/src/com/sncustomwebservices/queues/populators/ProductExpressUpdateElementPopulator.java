/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.queues.populators;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import com.sncustomwebservices.queues.data.ProductExpressUpdateElementData;

import org.springframework.util.Assert;


/**
 * Class populate information from ProductModel to ProductExpressUpdateElementData
 */
public class ProductExpressUpdateElementPopulator implements Populator<ProductModel, ProductExpressUpdateElementData>
{
	@Override
	public void populate(final ProductModel source, final ProductExpressUpdateElementData target)
			throws ConversionException
	{
		Assert.notNull(source, "Parameter source cannot be null.");
		Assert.notNull(target, "Parameter target cannot be null.");

		target.setCode(source.getCode());
		if (source.getCatalogVersion() != null)
		{
			target.setCatalogVersion(source.getCatalogVersion().getVersion());
			if (source.getCatalogVersion().getCatalog() != null)
			{
				target.setCatalogId(source.getCatalogVersion().getCatalog().getId());
			}
		}
	}
}
