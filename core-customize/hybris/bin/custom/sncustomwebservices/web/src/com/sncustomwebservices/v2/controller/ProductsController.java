/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.acceleratorfacades.order.data.PriceRangeData;
import de.hybris.platform.catalog.enums.ProductReferenceTypeEnum;
import de.hybris.platform.commercefacades.catalog.CatalogFacade;
import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ImageData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.product.data.ProductReferenceData;
import de.hybris.platform.commercefacades.product.data.ProductReferencesData;
import de.hybris.platform.commercefacades.product.data.PromotionData;
import de.hybris.platform.commercefacades.product.data.ReviewData;
import de.hybris.platform.commercefacades.product.data.SAPUnitData;
import de.hybris.platform.commercefacades.product.data.StockData;
import de.hybris.platform.commercefacades.product.data.SuggestionData;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercefacades.search.data.AutocompleteSuggestionData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commercefacades.storefinder.StoreFinderStockFacade;
import de.hybris.platform.commercefacades.storefinder.data.StoreFinderStockSearchPageData;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.store.data.GeoPoint;
import de.hybris.platform.commercewebservicescommons.dto.product.ProductReferenceListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ProductWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ReviewListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ReviewWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.StockWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.SuggestionListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.queues.ProductExpressUpdateElementListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductSearchPageWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.store.StoreFinderStockSearchPageWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.StockSystemException;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.formatters.WsDateFormatter;
import com.sncustomwebservices.product.data.ReviewDataList;
import com.sncustomwebservices.product.data.SuggestionDataList;
import com.sncustomwebservices.queues.data.ProductExpressUpdateElementData;
import com.sncustomwebservices.queues.data.ProductExpressUpdateElementDataList;
import com.sncustomwebservices.queues.impl.ProductExpressUpdateQueue;
import com.sncustomwebservices.stock.CommerceStockFacade;
import com.sncustomwebservices.v2.helper.ProductsHelper;
import com.sncustomwebservices.validator.PointOfServiceValidator;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * Web Services Controller to expose the functionality of the
 * {@link de.hybris.platform.commercefacades.product.ProductFacade} and SearchFacade.
 */

@Controller
@Tag(name = "Products")
@RequestMapping(value = "/{baseSiteId}/products")
public class ProductsController extends BaseController
{
	private static final EnumSet<ProductOption> PRODUCT_OPTIONS_SET = EnumSet.allOf(ProductOption.class);
	private static final String MAX_INTEGER = "2147483647";
	private static final int CATALOG_SIZE = 2;
	private static final int CATALOG_ID_POS = 0;
	private static final int CATALOG_VERSION_POS = 1;
	private static final String COMMA_SEPARATOR = ",";
	private static final Logger LOG = LoggerFactory.getLogger(ProductsController.class);
	private static final String CONSIDER_FIELDS_KEY = "toggle.occ.retrieving.product.performance.improvement.enabled";

	@Resource(name = "storeFinderStockFacade")
	private StoreFinderStockFacade storeFinderStockFacade;
	@Resource(name = "cwsProductFacade")
	private ProductFacade productFacade;
	@Resource(name = "wsDateFormatter")
	private WsDateFormatter wsDateFormatter;
	@Resource(name = "productSearchFacade")
	private ProductSearchFacade<ProductData> productSearchFacade;
	@Resource(name = "httpRequestReviewDataPopulator")
	private Populator<HttpServletRequest, ReviewData> httpRequestReviewDataPopulator;
	@Resource(name = "reviewValidator")
	private Validator reviewValidator;
	@Resource(name = "reviewDTOValidator")
	private Validator reviewDTOValidator;
	@Resource(name = "commerceStockFacade")
	private CommerceStockFacade commerceStockFacade;
	@Resource(name = "pointOfServiceValidator")
	private PointOfServiceValidator pointOfServiceValidator;
	@Resource(name = "productExpressUpdateQueue")
	private ProductExpressUpdateQueue productExpressUpdateQueue;
	@Resource(name = "catalogFacade")
	private CatalogFacade catalogFacade;
	@Resource(name = "productsHelper")
	private ProductsHelper productsHelper;
	@Resource(name = "configurationService")
	private ConfigurationService configurationService;

	@RequestMapping(value = "/search", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getProducts", summary = "Retrieves a list of products.", description =
			"Retrieves a list of products and related product search data, such as available attributes， available facets, available sorting, and spelling suggestions."
			+ " To enable spelling suggestions, you need to have indexed properties configured to be used for spell checking.")
	@ApiBaseSiteIdParam
	public ProductSearchPageWsDTO getProducts(
			@Parameter(description = "Filter conditions that combine attribute keys and attribute values. The attributes are the fields or properties for the Product type configured in the search index type."
					+ " The format is <attributeKey1>:<attributeValue1>:...:<attributeKeyN>:<attributeValueN>.") @RequestParam(required = false) final String filters,
			@Parameter(description = "Formatted query string. It contains query criteria like free text search, facet. The format is <freeTextSearch>:<sort>:<facetKey1>:<facetValue1>:...:<facetKeyN>:<facetValueN>.") @RequestParam(required = false) final String query,
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@Parameter(description = "Name of the search query template to be used in the search query. Examples: DEFAULT, SUGGESTIONS.") @RequestParam(required = false) final String searchQueryContext,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletResponse response)
	{
		final ProductSearchPageWsDTO result = productsHelper.searchProducts(filters, query, currentPage, pageSize, sort, addPaginationField(fields), searchQueryContext);
		setTotalCountHeader(response, result.getPagination());
		return result;
	}

	/**
	 * @deprecated since 2211.28, use {@link ProductsController#getProducts(String, String, int, int, String, String, String, HttpServletResponse)}
	 *             instead.
	 */
	@Deprecated(since = "2211.28", forRemoval = true)
	public ProductSearchPageWsDTO getProducts(final String query, final int currentPage, final int pageSize, final String sort, final String searchQueryContext,
			final String fields, final HttpServletResponse response)
	{
		final ProductSearchPageWsDTO result = productsHelper.searchProducts(query, currentPage, pageSize, sort,
				addPaginationField(fields), searchQueryContext);
		setTotalCountHeader(response, result.getPagination());
		return result;
	}


	@RequestMapping(value = "/search", method = RequestMethod.HEAD)
	@Operation(operationId = "countProducts", summary = "Retrieves the total number of products.", description = "In the response header, the \"x-total-count\" property indicates the total number of products for the query.")
	@ApiBaseSiteIdParam
	public void countProducts(
			@Parameter(description = "Formatted query string. It contains query criteria like free text search, facet. The format is <freeTextSearch>:<sort>:<facetKey1>:<facetValue1>:...:<facetKeyN>:<facetValueN>.") @RequestParam(required = false) final String query,
			final HttpServletResponse response)
	{
		final ProductSearchPageData<SearchStateData, ProductData> result = productsHelper.searchProducts(query, 0, 1, null);
		setTotalCountHeader(response, result.getPagination());
	}


	@RequestMapping(value = "/{productCode}", method = RequestMethod.GET)
	@CacheControl(directive = CacheControlDirective.PRIVATE, maxAge = 120)
	@Cacheable(value = "productCache", key = "T(de.hybris.platform.commercewebservicescommons.cache.ProductCacheKeyGenerator).generateKey(true,true,#productCode,#fields)")
	@ResponseBody
	@Operation(operationId = "getProduct", summary = "Retrieves product details.", description = "Retrieves the details of a single product using the product identifier.")
	@ApiBaseSiteIdParam
	public ProductWsDTO getProduct(
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		boolean isConsiderFields = configurationService.getConfiguration().getBoolean(CONSIDER_FIELDS_KEY, true);
		Collection<ProductOption> options = isConsiderFields?extractProductOptions(fields):EnumSet.allOf(ProductOption.class);
		if(LOG.isDebugEnabled()) {
			LOG.debug("getProduct: code={} | options={}", sanitize(productCode), options);
		}

		final ProductData product = productFacade.getProductForCodeAndOptions(productCode, options);return getDataMapper().map(product, ProductWsDTO.class, fields);
	}

	protected Collection<ProductOption> extractProductOptions(final String fields)
	{
		final ProductData tempProductData = new ProductData();
		tempProductData.setImages(Lists.newArrayList(new ImageData()));
		tempProductData.setReviews(Lists.newArrayList(new ReviewData()));
		tempProductData.setNumberOfReviews(Integer.valueOf(0));
		tempProductData.setPotentialPromotions(Lists.newArrayList(new PromotionData()));
		tempProductData.setPrice(new PriceData());
		tempProductData.setPurchasable(Boolean.FALSE);
		tempProductData.setPriceRange(new PriceRangeData());
		tempProductData.setStock(new StockData());
		tempProductData.setSapUnit(new SAPUnitData());

		final ProductWsDTO productWsDTO = getDataMapper().map(tempProductData, ProductWsDTO.class, fields);
		final boolean skipImages = CollectionUtils.isEmpty(productWsDTO.getImages());
		final EnumSet<ProductOption> options = EnumSet.allOf(ProductOption.class);
		if (skipImages)
		{
			options.remove(ProductOption.IMAGES);
			options.remove(ProductOption.GALLERY);
		}
		final boolean skipReviews = CollectionUtils.isEmpty(productWsDTO.getReviews()) && productWsDTO.getNumberOfReviews() == null;
		if (skipReviews)
		{
			options.remove(ProductOption.REVIEW);
		}
		final boolean skipPromotions = CollectionUtils.isEmpty(productWsDTO.getPotentialPromotions());
		if (skipPromotions)
		{
			options.remove(ProductOption.PROMOTIONS);
		}
		final boolean skipPrice = productWsDTO.getPrice() == null && productWsDTO.getPurchasable() == null && productWsDTO.getSapUnit() == null;
		if (skipPrice)
		{
			options.remove(ProductOption.PRICE);
		}
		final boolean skipPriceRange = productWsDTO.getPriceRange() == null;
		if (skipPriceRange)
		{
			options.remove(ProductOption.PRICE_RANGE);
		}
		final boolean skipStock = productWsDTO.getStock() == null;
		if (skipStock)
		{
			options.remove(ProductOption.STOCK);
		}
		return options;
	}

	@RequestMapping(value = "/{productCode}/stock/{storeName}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getStoreProductStock", summary = "Retrieves the stock level of a product.", description = "Retrieves the stock level of a product for the store.")
	public StockWsDTO getStoreProductStock(
			@Parameter(description = "Base site identifier.", required = true) @PathVariable final String baseSiteId,
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@Parameter(description = "Store identifier.", required = true) @PathVariable final String storeName,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		validate(storeName, "storeName", pointOfServiceValidator);
		if (!commerceStockFacade.isStockSystemEnabled(baseSiteId))
		{
			throw new StockSystemException("Stock system is not enabled on this site", StockSystemException.NOT_ENABLED, baseSiteId);
		}
		final StockData stockData = commerceStockFacade.getStockDataForProductAndPointOfService(productCode, storeName);
		return getDataMapper().map(stockData, StockWsDTO.class, fields);
	}


	@RequestMapping(value = "/{productCode}/stock", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getLocationProductStock", summary = "Retrieves the stock level of a product.", description =
			"The stock levels are sorted by distance from the specified location,"
			+ " which is defined using the free-text location parameter or the longitude and latitude parameters."
			+ " Either location parameter or longitude and latitude parameters are required.")
	@ApiBaseSiteIdParam
	public StoreFinderStockSearchPageWsDTO getLocationProductStock(
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@Parameter(description = "Free-text location") @RequestParam(required = false) final String location,
			@Parameter(description = "Coordinate that specifies the north-south position of a point on the Earth's surface.") @RequestParam(required = false) final Double latitude,
			@Parameter(description = "Coordinate that specifies the east-west position of a point on the Earth's surface.") @RequestParam(required = false) final Double longitude,
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(required = false, defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(required = false, defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletResponse response)
	{
		LOG.debug("getLocationProductStock: code={}  | location={} | latitude={} | longitude={}", sanitize(productCode),
				sanitize(location), latitude, longitude);

		final StoreFinderStockSearchPageData result = doSearchProductStockByLocation(productCode, location, latitude, longitude,
				currentPage, pageSize);
		setTotalCountHeader(response, result.getPagination());
		return getDataMapper().map(result, StoreFinderStockSearchPageWsDTO.class, addPaginationField(fields));
	}


	@RequestMapping(value = "/{productCode}/stock", method = RequestMethod.HEAD)
	@Operation(operationId = "countProductStockByLocation", summary = "Retrieves the total number of stock levels of a product.", description =
			"In the response header, the \"x-total-count\" property indicates the total number of stock levels of a product."
			+ " Either location parameter or longitude and latitude parameters are required.")
	@ApiBaseSiteIdParam
	public void countProductStockByLocation(
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@Parameter(description = "Free-text location") @RequestParam(required = false) final String location,
			@Parameter(description = "Coordinate that specifies the north-south position of a point on the Earth's surface.") @RequestParam(required = false) final Double latitude,
			@Parameter(description = "Coordinate that specifies the east-west position of a point on the Earth's surface.") @RequestParam(required = false) final Double longitude,
			final HttpServletResponse response)
	{
		final StoreFinderStockSearchPageData result = doSearchProductStockByLocation(productCode, location, latitude, longitude, 0,
				1);
		setTotalCountHeader(response, result.getPagination());
	}

	protected StoreFinderStockSearchPageData doSearchProductStockByLocation(final String productCode, final String location,
			final Double latitude, final Double longitude, final int currentPage, final int pageSize)
	{
		final Set<ProductOption> opts = EnumSet.of(ProductOption.BASIC);
		final StoreFinderStockSearchPageData result;
		if (latitude != null && longitude != null)
		{
			result = storeFinderStockFacade.productSearch(createGeoPoint(latitude, longitude),
					productFacade.getProductForCodeAndOptions(productCode, opts), createPageableData(currentPage, pageSize, null));
		}
		else if (location != null)
		{
			result = storeFinderStockFacade.productSearch(location, productFacade.getProductForCodeAndOptions(productCode, opts),
					createPageableData(currentPage, pageSize, null));
		}
		else
		{
			throw new RequestParameterException("You need to provide location or longitute and latitute parameters",
					RequestParameterException.MISSING, "location or longitute and latitute");
		}
		return result;
	}


	@RequestMapping(value = "/{productCode}/reviews", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getProductReviews", summary = "Retrieves the reviews of a product.", description = "Retrieves all the reviews for a product. To limit the number of reviews returned, use the maxCount parameter.")
	@ApiBaseSiteIdParam
	public ReviewListWsDTO getProductReviews(
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@Parameter(description = "Maximum number of reviews.") @RequestParam(required = false) final Integer maxCount,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final ReviewDataList reviewDataList = new ReviewDataList();
		reviewDataList.setReviews(productFacade.getReviews(productCode, maxCount));
		return getDataMapper().map(reviewDataList, ReviewListWsDTO.class, fields);
	}

	/**
	 * @deprecated since 2005. Please use {@link ProductsController#createProductReview(String, ReviewWsDTO, String)} instead.
	 */
	@Deprecated(since = "2005", forRemoval = true)
	@RequestMapping(value = "/{productCode}/reviews", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@Operation(hidden = true, summary = "Creates a customer review as an anonymous user.", description = "Creates a customer review for a product as an anonymous user.")
	@Parameter(name = "headline", description = "Headline of customer review", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@Parameter(name = "comment", description = "Comment of customer review", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@Parameter(name = "rating", description = "Rating of customer review", required = true, schema = @Schema(type = "double"), in = ParameterIn.QUERY)
	@Parameter(name = "alias", description = "Alias of customer review", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@ApiBaseSiteIdParam
	public ReviewWsDTO createProductReview(
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletRequest request)
	{
		final ReviewData reviewData = new ReviewData();
		httpRequestReviewDataPopulator.populate(request, reviewData);
		validate(reviewData, "reviewData", reviewValidator);
		final ReviewData reviewDataRet = productFacade.postReview(productCode, reviewData);
		return getDataMapper().map(reviewDataRet, ReviewWsDTO.class, fields);
	}


	@RequestMapping(value = "/{productCode}/reviews", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "createProductReview", summary = "Creates a customer review as an anonymous user.", description = "Creates a customer review for a product as an anonymous user.")
	@ApiBaseSiteIdParam
	public ReviewWsDTO createProductReview(
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@Parameter(description = "Object contains review details like : rating, alias, headline, comment.", required = true) @RequestBody final ReviewWsDTO review,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		validate(review, "review", reviewDTOValidator);
		final ReviewData reviewData = getDataMapper().map(review, ReviewData.class, "alias,rating,headline,comment");
		final ReviewData reviewDataRet = productFacade.postReview(productCode, reviewData);
		return getDataMapper().map(reviewDataRet, ReviewWsDTO.class, fields);
	}


	@RequestMapping(value = "/{productCode}/references", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getProductReferences", summary = "Retrieves the product references.", description = "Retrieves the references using the product code and reference type.")
	@ApiBaseSiteIdParam
	public ProductReferenceListWsDTO getProductReferences(
			@Parameter(description = "Product identifier.", required = true) @PathVariable final String productCode,
			@Parameter(description = "Number of results returned per page.") @RequestParam(required = false, defaultValue = MAX_INTEGER) final int pageSize,
			@Parameter(description = "Comma-separated list of reference types. If not specified, all types of product references will be used. Example: ACCESSORIES,BASE_PRODUCT.") @RequestParam(required = false) final String referenceType,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final List<ProductOption> opts = Lists.newArrayList(PRODUCT_OPTIONS_SET);

		final List<ProductReferenceTypeEnum> productReferenceTypeList = StringUtils.isNotEmpty(referenceType) ?
				getProductReferenceTypeEnums(referenceType) :
				List.of(ProductReferenceTypeEnum.values());

		final List<ProductReferenceData> productReferences = productFacade.getProductReferencesForCode(productCode,
				productReferenceTypeList, opts, Integer.valueOf(pageSize));
		final ProductReferencesData productReferencesData = new ProductReferencesData();
		productReferencesData.setReferences(productReferences);

		return getDataMapper().map(productReferencesData, ProductReferenceListWsDTO.class, fields);
	}

	protected List<ProductReferenceTypeEnum> getProductReferenceTypeEnums(final String referenceType)
	{
		final String[] referenceTypes = referenceType.split(COMMA_SEPARATOR);
		return Arrays.stream(referenceTypes).map(ProductReferenceTypeEnum::valueOf).collect(Collectors.toList());
	}

	protected PageableData createPageableData(final int currentPage, final int pageSize, final String sort)
	{
		final PageableData pageable = new PageableData();

		pageable.setCurrentPage(currentPage);
		pageable.setPageSize(pageSize);
		pageable.setSort(sort);
		return pageable;
	}

	protected GeoPoint createGeoPoint(final Double latitude, final Double longitude)
	{
		final GeoPoint point = new GeoPoint();
		point.setLatitude(latitude.doubleValue());
		point.setLongitude(longitude.doubleValue());

		return point;
	}



	@RequestMapping(value = "/suggestions", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getSuggestions", summary = "Retrieves the available suggestions.", description = "Retrieves the suggestions related to a specified term and limits the results according to the value of the max parameter.")
	@ApiBaseSiteIdParam
	public SuggestionListWsDTO getSuggestions(
			@Parameter(description = "Specified term. The suggestions will be given based on it.", required = true) @RequestParam final String term,
			@Parameter(description = "Maximum number of results.") @RequestParam(defaultValue = "10") final int max,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final List<SuggestionData> suggestions = new ArrayList<>();
		final SuggestionDataList suggestionDataList = new SuggestionDataList();

		List<AutocompleteSuggestionData> autoSuggestions = productSearchFacade.getAutocompleteSuggestions(term);
		if (max < autoSuggestions.size())
		{
			autoSuggestions = autoSuggestions.subList(0, max);
		}

		for (final AutocompleteSuggestionData autoSuggestion : autoSuggestions)
		{
			final SuggestionData suggestionData = new SuggestionData();
			suggestionData.setValue(autoSuggestion.getTerm());
			suggestions.add(suggestionData);
		}
		suggestionDataList.setSuggestions(suggestions);

		return getDataMapper().map(suggestionDataList, SuggestionListWsDTO.class, fields);
	}


	@Secured("ROLE_TRUSTED_CLIENT")
	@RequestMapping(value = "/expressupdate", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getExpressUpdateProducts", summary = "Retrieves products that were added to the express update feed.", description =
			"Retrieves products that were added to the express update feed."
			+ " Only the properties that were updated after the specified timestamp are returned. The queue is cleared using a defined cronjob.")
	@ApiBaseSiteIdParam
	public ProductExpressUpdateElementListWsDTO getExpressUpdateProducts(
			@Parameter(description = "Only products that are more recent than the given parameter are returned. The value should be in ISO-8601 format: 2018-01-09T16:28:45+0000.", required = true) @RequestParam final String timestamp,
			@Parameter(description = "Only products from this catalog are returned. Format: catalogId:catalogVersion.") @RequestParam(required = false) final String catalog,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final Date timestampDate;
		try
		{
			timestampDate = wsDateFormatter.toDate(timestamp);
		}
		catch (final IllegalArgumentException ex)
		{
			throw new RequestParameterException("Wrong time format. The only accepted format is ISO-8601.",
					RequestParameterException.INVALID, "timestamp", ex);
		}
		final ProductExpressUpdateElementDataList productExpressUpdateElementDataList = new ProductExpressUpdateElementDataList();
		final List<ProductExpressUpdateElementData> products = productExpressUpdateQueue.getItems(timestampDate);
		filterExpressUpdateQueue(products, validateAndSplitCatalog(catalog));
		productExpressUpdateElementDataList.setProductExpressUpdateElements(products);
		return getDataMapper().map(productExpressUpdateElementDataList, ProductExpressUpdateElementListWsDTO.class, fields);
	}

	protected void filterExpressUpdateQueue(final List<ProductExpressUpdateElementData> products, final List<String> catalogInfo)
	{
		if (catalogInfo.size() == CATALOG_SIZE && StringUtils.isNotEmpty(catalogInfo.get(CATALOG_ID_POS)) && StringUtils.isNotEmpty(
				catalogInfo.get(CATALOG_VERSION_POS)) && CollectionUtils.isNotEmpty(products))
		{
			final Iterator<ProductExpressUpdateElementData> dataIterator = products.iterator();
			while (dataIterator.hasNext())
			{
				final ProductExpressUpdateElementData productExpressUpdateElementData = dataIterator.next();
				if (!catalogInfo.get(CATALOG_ID_POS).equals(productExpressUpdateElementData.getCatalogId()) || !catalogInfo.get(
						CATALOG_VERSION_POS).equals(productExpressUpdateElementData.getCatalogVersion()))
				{
					dataIterator.remove();
				}
			}
		}
	}

	protected List<String> validateAndSplitCatalog(final String catalog)
	{
		final List<String> catalogInfo = new ArrayList<>();
		if (StringUtils.isNotEmpty(catalog))
		{
			catalogInfo.addAll(Lists.newArrayList(Splitter.on(':').trimResults().omitEmptyStrings().split(catalog)));
			if (catalogInfo.size() == 2)
			{
				catalogFacade.getProductCatalogVersionForTheCurrentSite(catalogInfo.get(CATALOG_ID_POS),
						catalogInfo.get(CATALOG_VERSION_POS), Collections.emptySet());
			}
			else if (!catalogInfo.isEmpty())
			{
				throw new RequestParameterException("Invalid format. You have to provide catalog as 'catalogId:catalogVersion'",
						RequestParameterException.INVALID, "catalog");
			}
		}
		return catalogInfo;
	}

}
