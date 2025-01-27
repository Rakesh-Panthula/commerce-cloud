/**
 *
 */
package com.sncustomwebservices.service.impl;

import de.hybris.platform.cmsfacades.data.MediaData;
import de.hybris.platform.core.model.product.ProductModel;

import java.util.List;

import com.sncustomwebservices.dao.CustomProductDao;
import com.sncustomwebservices.service.CustomProductService;

/**
 * @author Anand.Mund
 *
 */
public class CustomProductServiceImpl implements CustomProductService {

	private CustomProductDao customProductDao;

	@Override
	public List<ProductModel> getProductsByName(final String productName){
		return getCustomProductDao().getProductsByName(productName);
	}
	
	@Override
	public void updateImageForProduct(final String productCode, final MediaData mediaData)	{
		getCustomProductDao().updateImageForProduct(productCode, mediaData);
	}

	/**
	 * @return the customProductDao
	 */
	public CustomProductDao getCustomProductDao(){
		return customProductDao;
	}

	/**
	 * @param customProductDao the customProductDao to set
	 */
	public void setCustomProductDao(final CustomProductDao customProductDao){
		this.customProductDao = customProductDao;
	}



}
