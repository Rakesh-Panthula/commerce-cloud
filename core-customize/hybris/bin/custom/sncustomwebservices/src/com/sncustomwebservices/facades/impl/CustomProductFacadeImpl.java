/**
 *
 */
package com.sncustomwebservices.facades.impl;

import de.hybris.platform.cmsfacades.data.MediaData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import java.util.LinkedList;
import java.util.List;

import com.sncustomwebservices.facades.CustomProductFacade;
import com.sncustomwebservices.service.CustomProductService;

/**
 * @author Anand.Mund
 *
 */
public class CustomProductFacadeImpl implements CustomProductFacade {

	private CustomProductService productService;
	private Converter<ProductModel, ProductData> productConverter;

	@Override
	public List<ProductData> getProductsByName(final String productName)	{

		final List<ProductModel> productModel = getProductService().getProductsByName(productName);
		final List<ProductData> productData = new LinkedList<ProductData>();

		for (final ProductModel model : productModel) {
			final ProductData product = getProductConverter().convert(model);
			productData.add(product);
		}
		return productData;
	}

	@Override
	public void updateImageForProduct(final String productCode, final MediaData mediaData)	{
		getProductService().updateImageForProduct(productCode, mediaData);
	}



	/**
	 * @return the productService
	 */
	public CustomProductService getProductService()	{
		return productService;
	}

	/**
	 * @param productService the productService to set
	 */
	public void setProductService(final CustomProductService productService)	{
		this.productService = productService;
	}

	/**
	 * @return the productConverter
	 */
	public Converter<ProductModel, ProductData> getProductConverter()	{
		return productConverter;
	}

	/**
	 * @param productConverter the productConverter to set
	 */
	public void setProductConverter(final Converter<ProductModel, ProductData> productConverter)	{
		this.productConverter = productConverter;
	}

}
