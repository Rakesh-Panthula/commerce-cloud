/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.basestore.data.BaseStoreData;
import de.hybris.platform.commercefacades.basestores.BaseStoreFacade;
import de.hybris.platform.commercewebservicescommons.dto.basestore.BaseStoreWsDTO;
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

	@RequestMapping(value = "/{baseStoreUid}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getBaseStore", summary = "Get a base store.", description = "Returns details of a specific base store based on its identifier. The response contains detailed base store information.")
	@ApiBaseSiteIdParam
	public BaseStoreWsDTO getBaseStore(
			@Parameter(description = "Base store name", required = true) @PathVariable final String baseStoreUid,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final BaseStoreData baseStoreData = baseStoreFacade.getBaseStoreByUid(baseStoreUid);

		return getDataMapper().map(baseStoreData, BaseStoreWsDTO.class, fields);
	}
}
