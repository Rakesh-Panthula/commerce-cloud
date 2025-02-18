/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.basestore.data.BaseStoreData;
import de.hybris.platform.commercefacades.basestores.converters.populator.BaseStorePopulator;
import de.hybris.platform.commercefacades.basestores.BaseStoreFacade;
import de.hybris.platform.commercefacades.storelocator.data.PointOfServiceData;
import de.hybris.platform.commercewebservicescommons.dto.basestore.BaseStoreWsDTO;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


/**
 * Web Services Controller to expose the functionality of the {@link BaseStoreFacade}
 */
@Controller
@RequestMapping(value = "/{baseSiteId}/basestores")
@CacheControl(directive = CacheControlDirective.PUBLIC, maxAge = 1800)
@Tag(name = "Base Stores")
public class BaseStoresController extends BaseController
{
	@Resource(name = "baseStoreFacade")
	private BaseStoreFacade baseStoreFacade;

	@Resource(name = "sessionService")
	private SessionService sessionService;

	@RequestMapping(value = "/{baseStoreUid}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getBaseStore", summary = "Retrieves a base store.", description = "Retrieves the details of the base store.")
	@ApiBaseSiteIdParam
	public BaseStoreWsDTO getBaseStore(
			@Parameter(description = "Base store identifier.", required = true) @PathVariable final String baseStoreUid,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		// construct temporary BaseStoreData within PointOfService
		// call getDataMapper().map() with "fields" to check whether to skip loading PointOfService
		// add SkipPoS attribute to SessionContext, so as to retrieve in BaseStorePopulator and skip loading PointOfService for performance improvement.
		final BaseStoreData tempBaseStoreData = new BaseStoreData();
		final List<PointOfServiceData> pointOfService = Lists.newArrayList(new PointOfServiceData());
		tempBaseStoreData.setPointsOfService(pointOfService);

		final BaseStoreWsDTO baseStoreWsDTO = getDataMapper().map(tempBaseStoreData, BaseStoreWsDTO.class, fields);
		final boolean skipPointOfService = CollectionUtils.isEmpty(baseStoreWsDTO.getPointsOfService());
		sessionService.setAttribute(BaseStorePopulator.SKIP_BASESTORE_POINTSOFSERVICE, skipPointOfService);

		final BaseStoreData baseStoreData = baseStoreFacade.getBaseStoreByUid(baseStoreUid);

		return getDataMapper().map(baseStoreData, BaseStoreWsDTO.class, fields);
	}
}
