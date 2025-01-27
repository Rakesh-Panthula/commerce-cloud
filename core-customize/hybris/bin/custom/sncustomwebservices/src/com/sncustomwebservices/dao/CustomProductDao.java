/**
 *
 */
package com.sncustomwebservices.dao;

import de.hybris.platform.cmsfacades.data.MediaData;
import de.hybris.platform.core.model.product.ProductModel;

import java.util.List;

/**
 * @author Anand.Mund
 *
 */
public interface CustomProductDao {

	List<ProductModel> getProductsByName(String productName);

	void updateImageForProduct(String productCode, MediaData mediaData);

}
