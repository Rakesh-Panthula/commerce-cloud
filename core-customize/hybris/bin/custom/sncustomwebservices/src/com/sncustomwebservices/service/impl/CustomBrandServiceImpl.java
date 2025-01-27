/**
 * 
 */
package com.sncustomwebservices.service.impl;

import de.hybris.platform.core.model.product.ProductModel;

import java.util.List;

import com.sncustomwebservices.dao.CustomBrandDao;
import com.sncustomwebservices.service.CustomBrandService;

/**
 * @author Anand.Mund
 *
 */
public class CustomBrandServiceImpl implements CustomBrandService {
	
	private CustomBrandDao brandDao;

	@Override
	public List<ProductModel> getBrand() {
		
		return getBrandDao().getBrand();
	}

	/**
	 * @return the brandDao
	 */
	public CustomBrandDao getBrandDao(){
		return brandDao;
	}

	/**
	 * @param brandDao the brandDao to set
	 */
	public void setBrandDao(final CustomBrandDao brandDao){
		this.brandDao = brandDao;
	}
	
}
