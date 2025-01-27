/**
 *
 */
package com.sncustomwebservices.dao.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateIfSingleResult;
import static java.lang.String.format;

import de.hybris.platform.catalog.CatalogService;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.catalog.model.SyncItemJobModel;
import de.hybris.platform.catalog.synchronization.CatalogSynchronizationService;
import de.hybris.platform.catalog.synchronization.SyncConfig;
import de.hybris.platform.cmsfacades.data.MediaData;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.cronjob.enums.JobLogLevel;
import de.hybris.platform.search.restriction.SearchRestrictionService;
import de.hybris.platform.servicelayer.media.impl.MediaDao;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.util.ServicesUtil;

import java.util.ArrayList;
import java.util.List;

import com.sncustomwebservices.dao.CustomProductDao;

/**
 * @author Anand.Mund
 *
 */
public class CustomProductDaoImpl implements CustomProductDao {

	private static final String OFFLINE_VERSION = "Staged";
	private static final String ONLINE_VERSION = "Online";

	private FlexibleSearchService flexibleSearchService;
	private ModelService modelService;
	private de.hybris.platform.product.daos.ProductDao productImplDao;
	private MediaDao mediaDao;
	private CatalogService catalogService;
	private CatalogSynchronizationService service;
	private CatalogVersionService catalogVersionService;
	private SessionService sessionService;
	private SearchRestrictionService searchRestrictionService;

	@Override
	public List<ProductModel> getProductsByName(final String productName) {

		final String FIND_PRODUCT_BY_PRODUCTNAME = "select {" + ProductModel.PK + "} from {" + ProductModel._TYPECODE + "} "
				+ "where {" + ProductModel.CATALOGVERSION + "}='8796093547097' and {" + ProductModel.APPROVALSTATUS
				+ "}= '8796093120603' " + "and {" + ProductModel.NAME + "} like '%" + productName + "%'";

		final SearchResult<ProductModel> searchResult = getFlexibleSearchService().search(FIND_PRODUCT_BY_PRODUCTNAME);
		final List<ProductModel> productModel = searchResult.getResult();
		return productModel;
	}

	@Override
	public void updateImageForProduct(final String productCode, final MediaData mediaData)	{

		ServicesUtil.validateParameterNotNull(productCode, "Empty Product Code");
		ServicesUtil.validateParameterNotNull(mediaData.getCode(), "Empty ImageCode");

		final List<MediaModel> mediaModel = getMediaDao().findMediaByCode(mediaData.getCode());
		final CatalogVersionModel source = getStagedCatalogVersion(mediaData.getCatalogId(), mediaData.getCatalogVersion());
		final CatalogVersionModel target = getCatalogVersionService().getCatalogVersion(mediaData.getCatalogId(), ONLINE_VERSION);
		final List<ProductModel> productModel = getStagedProductModel(source, productCode);

		validateIfSingleResult(productModel, format("Product with code '%s' not found!", productCode),
				format("Product code '%s' is not unique, %d products found!", productCode, Integer.valueOf(productModel.size())));

		for (final ProductModel product : productModel)	{

			product.setPicture(mediaModel.get(0));
			saveProductInStagedVersion(product);
		}

		syncProductModel(productModel, source, target);
	}

	protected String createJobIdentifier(final String catalogId){
		return "sync " + catalogId + ":" + OFFLINE_VERSION + "->" + ONLINE_VERSION;
	}

	protected CatalogVersionModel getStagedCatalogVersion(final String catalogId, final String catalogVersion){
		return getSessionService().executeInLocalView(new SessionExecutionBody()	{
			@Override
			public Object execute(){
				try{
					getSearchRestrictionService().disableSearchRestrictions();
					return getCatalogVersionService().getCatalogVersion(catalogId, catalogVersion);
				}finally	{
					getSearchRestrictionService().enableSearchRestrictions();
				}
			}
		});
	}

	protected List<ProductModel> getStagedProductModel(final CatalogVersionModel model, final String productCode){
		return getSessionService().executeInLocalView(new SessionExecutionBody()	{
			@Override
			public Object execute(){
				try{
					getSearchRestrictionService().disableSearchRestrictions();
					return getProductImplDao().findProductsByCode(model, productCode);
				}finally	{
					getSearchRestrictionService().enableSearchRestrictions();
				}
			}
		});
	}

	protected void saveProductInStagedVersion(final ProductModel productModel){
		getSessionService().executeInLocalView(new SessionExecutionBody(){

			@Override
			public Object execute(){
				try{
					getSearchRestrictionService().disableSearchRestrictions();
					getModelService().save(productModel);
					return productModel;
				}finally{
					getSearchRestrictionService().enableSearchRestrictions();
				}
			}
		});
	}

	protected void syncProductModel(final List<ProductModel> listProductModel, final CatalogVersionModel source,
			final CatalogVersionModel target){

		final List<ItemModel> givenItem = new ArrayList<ItemModel>();
		givenItem.addAll(listProductModel);
		final SyncItemJobModel syncItemJobModel = getService().getSyncJob(source, target,
				createJobIdentifier(source.getCatalog().getId()));
		final SyncConfig syncConfig = new SyncConfig();
		syncConfig.setSynchronous(false);
		syncConfig.setCreateSavedValues(false);
		syncConfig.setForceUpdate(false);
		syncConfig.setLogLevelDatabase(JobLogLevel.WARNING);
		syncConfig.setLogLevelFile(JobLogLevel.INFO);
		syncConfig.setLogToDatabase(false);
		syncConfig.setLogToFile(true);

		getService().performSynchronization(givenItem, syncItemJobModel, syncConfig);
	}

	/**
	 * @return the flexibleSearchService
	 */
	public FlexibleSearchService getFlexibleSearchService() {
		return flexibleSearchService;
	}

	/**
	 * @param flexibleSearchService the flexibleSearchService to set
	 */
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService) {
		this.flexibleSearchService = flexibleSearchService;
	}

	/**
	 * @return the modelService
	 */
	public ModelService getModelService()
	{
		return modelService;
	}

	/**
	 * @param modelService
	 *           the modelService to set
	 */
	public void setModelService(final ModelService modelService){
		this.modelService = modelService;
	}

	/**
	 * @return the productImplDao
	 */
	public de.hybris.platform.product.daos.ProductDao getProductImplDao(){
		return productImplDao;
	}

	/**
	 * @param productImplDao
	 *           the productImplDao to set
	 */
	public void setProductImplDao(final de.hybris.platform.product.daos.ProductDao productImplDao){
		this.productImplDao = productImplDao;
	}

	/**
	 * @return the mediaDao
	 */
	public MediaDao getMediaDao(){
		return mediaDao;
	}

	/**
	 * @param mediaDao
	 *           the mediaDao to set
	 */
	public void setMediaDao(final MediaDao mediaDao){
		this.mediaDao = mediaDao;
	}

	/**
	 * @return the catalogService
	 */
	public CatalogService getCatalogService()	{
		return catalogService;
	}

	/**
	 * @param catalogService
	 *           the catalogService to set
	 */
	public void setCatalogService(final CatalogService catalogService)	{
		this.catalogService = catalogService;
	}

	/**
	 * @return the service
	 */
	public CatalogSynchronizationService getService()	{
		return service;
	}

	/**
	 * @param service
	 *           the service to set
	 */
	public void setService(final CatalogSynchronizationService service)	{
		this.service = service;
	}

	/**
	 * @return the catalogVersionService
	 */
	public CatalogVersionService getCatalogVersionService()	{
		return catalogVersionService;
	}

	/**
	 * @param catalogVersionService
	 *           the catalogVersionService to set
	 */
	public void setCatalogVersionService(final CatalogVersionService catalogVersionService){
		this.catalogVersionService = catalogVersionService;
	}

	/**
	 * @return the sessionService
	 */
	public SessionService getSessionService()	{
		return sessionService;
	}

	/**
	 * @param sessionService
	 *           the sessionService to set
	 */
	public void setSessionService(final SessionService sessionService)	{
		this.sessionService = sessionService;
	}

	/**
	 * @return the searchRestrictionService
	 */
	public SearchRestrictionService getSearchRestrictionService()	{
		return searchRestrictionService;
	}

	/**
	 * @param searchRestrictionService
	 *           the searchRestrictionService to set
	 */
	public void setSearchRestrictionService(final SearchRestrictionService searchRestrictionService){
		this.searchRestrictionService = searchRestrictionService;
	}



}
