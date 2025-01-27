/**
 *
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sncustomwebservices.facades.CustomBrandFacade;
import com.sncustomwebservices.facades.data.BrandData;
import com.sncustomwebservices.v2.data.BrandWsDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author Anand.Mund
 *
 */

@Controller
@RequestMapping(value = "/{baseSiteId}/brand")
@CacheControl(directive = CacheControlDirective.PRIVATE)
@Tag(name = "Brand")
public class BrandController extends BaseController {

	private static final Logger LOG = LoggerFactory.getLogger(BrandController.class);

	@Resource(name = "customBrandFacade")
	private CustomBrandFacade brandFacade;

	@RequestMapping(value ={ "/search" }, method = RequestMethod.GET)
	@ResponseBody
	@Operation(operationId = "getBrand", summary = "Get the list of brand for a particular basestore..")
	@ApiBaseSiteIdParam
	public BrandWsDTO getBrand(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)	{

		final BrandData brandData = brandFacade.getBrand();

		return getDataMapper().map(brandData, BrandWsDTO.class, fields);
	}
}
