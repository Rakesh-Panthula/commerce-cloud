/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.basesite.data.BaseSiteData;
import de.hybris.platform.commercefacades.basesites.BaseSiteFacade;
import de.hybris.platform.commercewebservicescommons.dto.basesite.BaseSiteListWsDTO;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.basesite.data.BaseSiteDataList;

import javax.annotation.Resource;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;


@Controller
@RequestMapping(value = "/basesites")
@CacheControl(directive = CacheControlDirective.PUBLIC, maxAge = 360)
@Tag(name = "Base Sites")
public class BaseSitesController extends BaseController
{
	@Resource(name = "baseSiteFacade")
	private BaseSiteFacade baseSiteFacade;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getBaseSites", summary = "Retrieves the base sites.", description = "Retrieves the base sites and the details of the corresponding base stores.")
	public BaseSiteListWsDTO getBaseSites(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final List<BaseSiteData> allBaseSites = baseSiteFacade.getAllBaseSites();
		final BaseSiteDataList baseSiteDataList = new BaseSiteDataList();
		baseSiteDataList.setBaseSites(allBaseSites);
		return getDataMapper().map(baseSiteDataList, BaseSiteListWsDTO.class, fields);
	}
}
