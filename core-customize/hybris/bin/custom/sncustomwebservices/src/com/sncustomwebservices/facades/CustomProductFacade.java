/**
 *
 */
package com.sncustomwebservices.facades;

import de.hybris.platform.cmsfacades.data.MediaData;
import de.hybris.platform.commercefacades.product.data.ProductData;

import java.util.List;

/**
 * @author Anand.Mund
 *
 */
public interface CustomProductFacade {

	List<ProductData> getProductsByName(String productName);

	void updateImageForProduct(String productCode, MediaData mediaData);

}
