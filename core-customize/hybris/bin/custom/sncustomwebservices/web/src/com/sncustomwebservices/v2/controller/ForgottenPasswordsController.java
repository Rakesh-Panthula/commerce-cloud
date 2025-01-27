/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commerceservices.customer.TokenInvalidatedException;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.commercewebservicescommons.dto.user.PasswordRestoreTokenInputWsDTO;
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
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
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

	@Resource(name = "passwordRestoreTokenInputValidator")
	private Validator passwordRestoreTokenInputValidator;

	/**
	 *
	 * @deprecated since 2211.24. Please use {@link de.hybris.platform.commercewebservices.core.v2.controller.ForgottenPasswordsController#sendPasswordRestoreToken(PasswordRestoreTokenInputWsDTO)} instead.
	 */
	@Deprecated(since = "2211.24", forRemoval = true)
	@SecurePortalUnauthenticatedAccess
	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/forgottenpasswordtokens", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(operationId = "doRestorePassword", summary = "Creates a token to restore a forgotten password.", description = "Creates a token so that customers can restore their forgotten passwords. This endpoint is deprecated in the 2211.24 update and its deletion is planned. Please use the POST /{baseSiteId}/passwordRestoreToken instead.")
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
	@PostMapping(value = "/passwordRestoreToken", consumes = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(operationId = "sendPasswordRestoreToken", summary = "Creates and sends a token to restore a forgotten password.", description = "Creates and sends a token that is used to restore a forgotten password in a following asynchronous customer communication. This endpoint is added in the 2211.24 update.")
	@ApiBaseSiteIdParam
	public void sendPasswordRestoreToken(
			@Parameter(description = "Object contains information required for creating and sending a token to restore a forgotten password.", required = true) @RequestBody final PasswordRestoreTokenInputWsDTO passwordRestoreTokenInputWsDTO)
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("sendPasswordRestoreToken: user loginId: {}", sanitize(passwordRestoreTokenInputWsDTO.getLoginId()));
		}
		validate(passwordRestoreTokenInputWsDTO, "passwordRestoreTokenInput", passwordRestoreTokenInputValidator);
		try
		{
			customerFacade.forgottenPassword(passwordRestoreTokenInputWsDTO.getLoginId());
		}
		catch (final UnknownIdentifierException unknownIdentifierException)
		{
			LOG.warn("User with unique property: {} does not exist in the database.", sanitize(passwordRestoreTokenInputWsDTO.getLoginId()));
		}
	}

	@SecurePortalUnauthenticatedAccess
	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT" })
	@RequestMapping(value = "/resetpassword", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(operationId = "doResetPassword", summary = "Resets a forgotten password.", description = "Reset password after customer's clicked forgotten password link. A new password needs to be provided.")
	@ApiBaseSiteIdParam
	public void doResetPassword(
			@Parameter(description = "Request body parameter that contains details such as token and new password", required = true) @RequestBody final ResetPasswordWsDTO resetPassword)
			throws TokenInvalidatedException
	{
		LOG.debug("Executing method doResetPassword");
		customerFacade.updatePassword(resetPassword.getToken(), resetPassword.getNewPassword());

	}

}
