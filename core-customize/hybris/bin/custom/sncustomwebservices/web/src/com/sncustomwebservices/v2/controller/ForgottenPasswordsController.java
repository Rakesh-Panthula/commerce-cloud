/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commerceservices.customer.TokenInvalidatedException;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.commercewebservicescommons.dto.user.ResetPasswordWsDTO;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


@Controller
@RequestMapping(value = "/{baseSiteId}")
@CacheControl(directive = CacheControlDirective.NO_STORE)
@Tag(name = "Forgotten Passwords")
public class ForgottenPasswordsController extends BaseController
{
	private static final Logger LOG = LoggerFactory.getLogger(ForgottenPasswordsController.class);

	@Resource(name = "wsCustomerFacade")
	private CustomerFacade customerFacade;

	@SecurePortalUnauthenticatedAccess
	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/forgottenpasswordtokens", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(operationId = "doRestorePassword", summary = "Generates a token to restore a customer's forgotten password.", description = "Generates a token in order to restore a customer's forgotten password.")
	@ApiBaseSiteIdParam
	public void doRestorePassword(
			@Parameter(description = "Customer's user id. Customer user id is case insensitive.", required = true) @RequestParam final String userId)
	{
		LOG.debug("doRestorePassword: user unique property: {}", sanitize(userId));
		try
		{
			customerFacade.forgottenPassword(userId);
		}
		catch (final UnknownIdentifierException unknownIdentifierException)
		{
			LOG.warn("User with unique property: {} does not exist in the database.", sanitize(userId));
		}
	}

	@SecurePortalUnauthenticatedAccess
	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/resetpassword", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(operationId = "doResetPassword", summary = "Reset password after customer's clicked forgotten password link.", description = "Reset password after customer's clicked forgotten password link. A new password needs to be provided.")
	@ApiBaseSiteIdParam
	public void doResetPassword(
			@Parameter(description = "Request body parameter that contains details such as token and new password", required = true) @RequestBody final ResetPasswordWsDTO resetPassword)
			throws TokenInvalidatedException
	{
		LOG.debug("Executing method doResetPassword");
		customerFacade.updatePassword(resetPassword.getToken(), resetPassword.getNewPassword());

	}

}
