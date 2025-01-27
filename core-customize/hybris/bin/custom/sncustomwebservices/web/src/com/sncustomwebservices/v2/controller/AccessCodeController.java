/*
 *  Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */

package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.order.CartAccessCodeFacade;
import de.hybris.platform.accesscode.exceptions.AccessCodeException;
import de.hybris.platform.commercewebservicescommons.dto.accessCode.SapAccessCodePublicKeyWsDTO;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RequestMapping(value = "/{baseSiteId}")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@RestController
@Tag(name = "Access Code")
public class AccessCodeController extends BaseCommerceController
{
	@Resource(name = "cartAccessCodeFacade")
	CartAccessCodeFacade cartAccessCodeFacade;

	/**
	 * Get rsa public key for access code.
	 *
	 * @return public key to verify access code
	 * @throws AccessCodeException get access code public key failed exception.
	 */
	@ResponseBody
	@GetMapping(value = "/accessCode/publicKey", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getAccessCodePublicKey", summary = "Retrieves the public key to verify the access code.", description = "Retrieves the public key used to verify the authenticity and integrity of the generated accessCode. "
			+ "AccessCodes are used to securely enable access to resources supporting the generation of an accessCode.")
	@ApiBaseSiteIdParam
	public SapAccessCodePublicKeyWsDTO getAccessCodePublicKey() throws AccessCodeException
	{
		final SapAccessCodePublicKeyWsDTO accessCodePublicKeyWsDTO = new SapAccessCodePublicKeyWsDTO();
		accessCodePublicKeyWsDTO.setPublicKey(this.cartAccessCodeFacade.getAccessCodePublicKey());
		return accessCodePublicKeyWsDTO;
	}
}
