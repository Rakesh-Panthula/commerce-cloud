/**
 *
 */
package com.sncustomwebservices.dao.impl;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.List;

import com.sncustomwebservices.dao.CustomBrandDao;

/**
 * @author Anand.Mund
 *
 */
public class CustomBrandDaoImpl implements CustomBrandDao {

	private FlexibleSearchService flexibleSearchService;

	private static final String GET_BRAND_QUERY = "select {pk},{code},{manufacturername} from {product} where {catalogversion}='8796093547097' "
			+ "and {approvalstatus}= '8796093120603' and {manufacturername} is not null";

	@Override
	public List<ProductModel> getBrand() {

		final SearchResult<ProductModel> searchResult = getFlexibleSearchService().search(GET_BRAND_QUERY);
		final List<ProductModel> brandList = searchResult.getResult();

		return brandList;
	}

	/**
	 * @return the flexibleSearchService
	 */
	public FlexibleSearchService getFlexibleSearchService()	{
		return flexibleSearchService;
	}

	/**
	 * @param flexibleSearchService the flexibleSearchService to set
	 */
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
		this.flexibleSearchService = flexibleSearchService;
	}

}
