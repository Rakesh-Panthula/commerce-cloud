/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.consent.ConsentFacade;
import de.hybris.platform.commercefacades.consent.data.ConsentTemplateData;
import de.hybris.platform.commerceservices.consent.exceptions.CommerceConsentGivenException;
import de.hybris.platform.commerceservices.consent.exceptions.CommerceConsentWithdrawnException;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.commercewebservicescommons.dto.consent.ConsentTemplateListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.consent.ConsentTemplateWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.ConsentWithdrawnException;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.webservicescommons.errors.exceptions.AlreadyExistsException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.consent.data.ConsentTemplateDataList;

import javax.annotation.Resource;
import javax.ws.rs.ForbiddenException;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}")
@Tag(name = "Consents")
public class ConsentsController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(ConsentsController.class);

	@Resource(name = "consentFacade")
	private ConsentFacade consentFacade;

	@SecurePortalUnauthenticatedAccess
	@RequestMapping(value = "/consenttemplates", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	@Operation(operationId = "getConsentTemplates", summary = "Retrieves the consents.", description = "If a customer has not given or has withdrawn their consent to the template, a date is not returned.")
	@ApiBaseSiteIdAndUserIdParam
	public ConsentTemplateListWsDTO getConsentTemplates(
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final ConsentTemplateDataList consentTemplateDataList = new ConsentTemplateDataList();
		final List<ConsentTemplateData> consentTemplateDatas = getUserFacade().isAnonymousUser() ?
				consentFacade.getConsentTemplatesWithConsents().stream().filter(ConsentTemplateData::isExposed)
						.collect(Collectors.toList()) :
				consentFacade.getConsentTemplatesWithConsents();
		consentTemplateDataList.setConsentTemplates(consentTemplateDatas);
		return getDataMapper().map(consentTemplateDataList, ConsentTemplateListWsDTO.class, fields);
	}

	@RequestMapping(value = "/consents", method = RequestMethod.POST)
	@ResponseBody
	@Operation(operationId = "doGiveConsent", summary = "Creates consent.", description = "Creates consent to collect or transfer the personal data of a customer.")
	@ApiBaseSiteIdAndUserIdParam
	public ResponseEntity<ConsentTemplateWsDTO> doGiveConsent(
			@Parameter(description = "Consent template identifier.", example = "00001000", required = true) @RequestParam final String consentTemplateId,
			@Parameter(description = "Consent template version.", example = "00001000", required = true) @RequestParam final Integer consentTemplateVersion)
	{
		if (getUserFacade().isAnonymousUser())
		{
			throw new ForbiddenException("An anonymous user can't give a consent");
		}

		try
		{
			consentFacade.giveConsent(consentTemplateId, consentTemplateVersion);
		}
		catch (final CommerceConsentGivenException e)
		{
			LOG.warn(e.getMessage(), e);
			throw new AlreadyExistsException(e.getMessage());
		}
		final ConsentTemplateData consentTemplate = consentFacade.getLatestConsentTemplate(consentTemplateId);
		final ConsentTemplateWsDTO consentTemplateWsDto = getDataMapper().map(consentTemplate, ConsentTemplateWsDTO.class);

		final String uriLocation = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").replaceQuery(StringUtils.EMPTY)
				.buildAndExpand(consentTemplateWsDto.getId()).toUriString().replace("/consents", "/consenttemplates");

		return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.LOCATION, uriLocation).body(consentTemplateWsDto);
	}

	@SecurePortalUnauthenticatedAccess
	@RequestMapping(value = "/consenttemplates/{consentTemplateId}", method = RequestMethod.GET)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	@Operation(operationId = "getConsentTemplate", summary = "Retrieves the consent.", description = "Retrieves the consent using the template identifier. If a customer has not given or has withdrawn their consent to the template, a date is not returned.")
	@ApiBaseSiteIdAndUserIdParam
	public ConsentTemplateWsDTO getConsentTemplate(
			@Parameter(description = "Consent template identifier.", example = "00001000", required = true) @PathVariable final String consentTemplateId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{

		final ConsentTemplateData latestConsentTemplate = consentFacade.getLatestConsentTemplate(consentTemplateId);
		if (getUserFacade().isAnonymousUser() && !latestConsentTemplate.isExposed())
		{
			throw new NotFoundException("This consent template is not exposed to anonymous user");
		}
		return getDataMapper().map(latestConsentTemplate, ConsentTemplateWsDTO.class, fields);
	}

	@RequestMapping(value = "/consents/{consentCode}", method = RequestMethod.DELETE)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	@Operation(operationId = "removeConsent", summary = "Deletes the user consent.", description = "If the consent was given, then the consent is deleted. If the consent was withdrawn, then it returns a withdrawal error. If the consent doesn't exist, it returns a \"not found\" error. If the customer is anonymous, then it returns an \"access denied\" error.")
	@ApiBaseSiteIdAndUserIdParam
	public void removeConsent(
			@Parameter(description = "Consent code.", example = "0000001", required = true) @PathVariable(value = "consentCode") final String consentCode)
	{
		if (getUserFacade().isAnonymousUser())
		{
			throw new AccessDeniedException("Anonymous user cannot withdraw consent");
		}

		final String consentNotFoundMessage = "Consent with code [%s] was not found";
		try
		{
			consentFacade.withdrawConsent(consentCode);
		}
		catch (final CommerceConsentWithdrawnException e)
		{
			LOG.warn(e.getMessage(), e);
			throw new ConsentWithdrawnException(e.getMessage(), ConsentWithdrawnException.CONSENT_WITHDRAWN);
		}
		catch (final IllegalArgumentException e)
		{
			LOG.warn(String.format(consentNotFoundMessage, consentCode), e);
			throw new NotFoundException(String.format(consentNotFoundMessage, consentCode));
		}
		catch (final ModelNotFoundException e)
		{
			throw new NotFoundException(String.format(consentNotFoundMessage, consentCode));
		}
		catch (final AccessDeniedException e)
		{
			throw e;
		}
	}

}
