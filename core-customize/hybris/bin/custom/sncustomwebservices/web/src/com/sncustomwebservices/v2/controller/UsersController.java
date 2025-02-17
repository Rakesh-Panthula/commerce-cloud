/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.customergroups.CustomerGroupFacade;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.commercefacades.user.data.RegisterData;
import de.hybris.platform.commercefacades.user.data.UserGroupDataList;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.commercewebservicescommons.annotation.CaptchaAware;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.constants.CommercewebservicescommonsConstants;
import de.hybris.platform.commercewebservicescommons.dto.user.UserGroupListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.UserSignUpWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.UserWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import com.sncustomwebservices.constants.YcommercewebservicesConstants;
import com.sncustomwebservices.populator.HttpRequestCustomerDataPopulator;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;


@Controller
@RequestMapping(value = "/{baseSiteId}/users")
@CacheControl(directive = CacheControlDirective.PRIVATE)
@Tag(name = "Users")
public class UsersController extends BaseCommerceController
{
	private static final Logger LOG = LoggerFactory.getLogger(UsersController.class);
	public static final String USER_MAPPER_CONFIG = "firstName,lastName,titleCode,currency(isocode),language(isocode),sitePreference";

	@Resource(name = "wsCustomerFacade")
	private CustomerFacade customerFacade;
	@Resource(name = "wsCustomerGroupFacade")
	private CustomerGroupFacade customerGroupFacade;
	@Resource(name = "httpRequestCustomerDataPopulator")
	private HttpRequestCustomerDataPopulator httpRequestCustomerDataPopulator;
	@Resource(name = "HttpRequestUserSignUpDTOPopulator")
	private Populator<HttpServletRequest, UserSignUpWsDTO> httpRequestUserSignUpDTOPopulator;
	@Resource(name = "putUserDTOValidator")
	private Validator putUserDTOValidator;
	@Resource(name = "userSignUpDTOValidator")
	private Validator userSignUpDTOValidator;
	@Resource(name = "guestConvertingDTOValidator")
	private Validator guestConvertingDTOValidator;
	@Resource(name = "passwordStrengthValidator")
	private Validator passwordStrengthValidator;
	@Resource(name = "patchUserWsDTOValidator")
	private Validator patchUserWsDTOValidator;

	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@SecurePortalUnauthenticatedAccess
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	@ResponseBody
	@Operation(hidden = true, summary = " Registers a customer", description =
			"Registers a customer. There are two options for registering a customer. The first option requires "
					+ "the following parameters: login, password, firstName, lastName, titleCode. The second option converts a guest to a customer. In this case, the required parameters are: guid, password.")
	@ApiBaseSiteIdParam
	@Parameter(name = CommercewebservicescommonsConstants.CAPTCHA_TOKEN_HEADER, description = CommercewebservicescommonsConstants.CAPTCHA_TOKEN_HEADER_DESC, required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
	@CaptchaAware
	public UserWsDTO createUser(
			@Parameter(description = "Customer's login. Customer login is case insensitive.") @RequestParam(required = false) final String login,
			@Parameter(description = "Customer's password.", required = true) @RequestParam final String password,
			@Parameter(description = "Customer's title code. For a list of codes, see /{baseSiteId}/titles resource") @RequestParam(required = false) final String titleCode,
			@Parameter(description = "Customer's first name.") @RequestParam(required = false) final String firstName,
			@Parameter(description = "Customer's last name.") @RequestParam(required = false) final String lastName,
			@Parameter(description = "Guest order's guid.") @RequestParam(required = false) final String guid,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
			final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws DuplicateUidException
	{
		final UserSignUpWsDTO user = new UserSignUpWsDTO();
		httpRequestUserSignUpDTOPopulator.populate(httpRequest, user);
		CustomerData customer = null;
		final String userId;
		if (guid != null)
		{
			validate(user, "user", guestConvertingDTOValidator);
			convertToCustomer(password, guid);
			customer = customerFacade.getCurrentCustomer();
			userId = customer.getUid();
		}
		else
		{
			validate(user, "user", userSignUpDTOValidator);
			registerNewUser(login, password, titleCode, firstName, lastName);
			userId = login.toLowerCase(Locale.ENGLISH);
			customer = customerFacade.getUserForUID(userId);
		}
		httpResponse.setHeader(YcommercewebservicesConstants.LOCATION, getAbsoluteLocationURL(httpRequest, userId));
		return getDataMapper().map(customer, UserWsDTO.class, fields);
	}

	protected void convertToCustomer(final String password, final String guid) throws DuplicateUidException
	{
		LOG.debug("convertToCustomer: guid={}", sanitize(guid));

		try
		{
			customerFacade.changeGuestToCustomer(password, guid);
		}
		catch (final UnknownIdentifierException | IllegalArgumentException ex)
		{
			/* IllegalArgumentException - occurs when order does not belong to guest user.
			For security reasons it's better to treat it as "unknown identifier" error */

			throw new RequestParameterException("Order with guid " + sanitize(guid) + " not found in current BaseStore",
					RequestParameterException.UNKNOWN_IDENTIFIER, "guid", ex);
		}
	}

	protected void registerNewUser(final String login, final String password, final String titleCode, final String firstName,
			final String lastName) throws DuplicateUidException
	{
		LOG.debug("registerUser: login={}", sanitize(login));

		if (!EmailValidator.getInstance().isValid(login))
		{
			throw new RequestParameterException("Login [" + sanitize(login) + "] is not a valid e-mail address!",
					RequestParameterException.INVALID, "login");
		}

		final RegisterData registerData = createRegisterData(login, password, titleCode, firstName, lastName);
		customerFacade.register(registerData);
	}

	private RegisterData createRegisterData(final String login, final String password, final String titleCode,
			final String firstName, final String lastName)
	{
		final RegisterData registerData = new RegisterData();
		registerData.setFirstName(firstName);
		registerData.setLastName(lastName);
		registerData.setLogin(login);
		registerData.setPassword(password);
		registerData.setTitleCode(titleCode);
		return registerData;
	}


	@SecurePortalUnauthenticatedAccess
	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(value = HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "createUser", summary = " Registers a customer", description =
			"Registers a customer. Requires the following " + "parameters: login, password, firstName, lastName, titleCode.")
	@ApiBaseSiteIdParam
	@Parameter(name = CommercewebservicescommonsConstants.CAPTCHA_TOKEN_HEADER, description = CommercewebservicescommonsConstants.CAPTCHA_TOKEN_HEADER_DESC, required = false, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
	@CaptchaAware
	public UserWsDTO createUser(
			@Parameter(description = "User's object.", required = true) @RequestBody final UserSignUpWsDTO user,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
			final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
	{
		validate(user, "user", userSignUpDTOValidator);
		final RegisterData registerData = getDataMapper().map(user, RegisterData.class,
				"login,password,titleCode,firstName,lastName");
		boolean userExists = false;
		try
		{
			customerFacade.register(registerData);
		}
		catch (final DuplicateUidException ex)
		{
			userExists = true;
			LOG.debug("Duplicated UID", ex);
		}
		final String userId = user.getUid().toLowerCase(Locale.ENGLISH);
		httpResponse.setHeader(YcommercewebservicesConstants.LOCATION, getAbsoluteLocationURL(httpRequest, userId));
		final CustomerData customerData = getCustomerData(registerData, userExists, userId);
		return getDataMapper().map(customerData, UserWsDTO.class, fields);
	}

	protected CustomerData getCustomerData(final RegisterData registerData, final boolean userExists, final String userId)
	{
		final CustomerData customerData;
		if (userExists)
		{
			customerData = customerFacade.nextDummyCustomerData(registerData);

			// if user exists, make sure return the customer.uid is the same with exist one
			alignCustomerUidWithExistUser(userId, customerData);
		}
		else
		{
			customerData = customerFacade.getUserForUID(userId);
		}
		return customerData;
	}

	protected void alignCustomerUidWithExistUser(final String userId, final CustomerData customerData)
	{
		CustomerData existCustomerData = customerFacade.getUserForUID(userId);

		if (!StringUtils.equals(customerData.getUid(), existCustomerData.getUid()))
		{
			customerData.setUid(existCustomerData.getUid());
		}
	}

	protected String getAbsoluteLocationURL(final HttpServletRequest httpRequest, final String uid)
	{
		final String requestURL = httpRequest.getRequestURL().toString();
		final String encodedUid = UriUtils.encodePathSegment(uid, StandardCharsets.UTF_8.name());
		return UriComponentsBuilder.fromHttpUrl(requestURL).pathSegment(encodedUid).build().toString();
	}


	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}", method = RequestMethod.GET)
	@ResponseBody
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
	@Operation(operationId = "getUser", summary = "Get customer profile", description = "Returns customer profile.")
	@ApiBaseSiteIdAndUserIdParam
	public UserWsDTO getUser(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CustomerData customerData = customerFacade.getCurrentCustomer();
		return getDataMapper().map(customerData, UserWsDTO.class, fields);
	}

	/**
	 * @deprecated since 2005. Please use {@link UsersController#replaceUser(UserWsDTO)} instead.
	 */
	@Deprecated(since = "2005", forRemoval = true)
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	@Operation(hidden = true, summary = "Updates customer profile", description = "Updates customer profile. Attributes not provided in the request body will be defined again (set to null or default).")
	@Parameter(name = "baseSiteId", description = "Base site identifier", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
	@Parameter(name = "userId", description = "User identifier.", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
	@Parameter(name = "language", description = "Customer's language.", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@Parameter(name = "currency", description = "Customer's currency.", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	public void replaceUser(
			@Parameter(description = "Customer's first name.", required = true) @RequestParam final String firstName,
			@Parameter(description = "Customer's last name.", required = true) @RequestParam final String lastName,
			@Parameter(description = "Customer's title code. For a list of codes, see /{baseSiteId}/titles resource", required = false) @RequestParam final String titleCode,
			final HttpServletRequest request) throws DuplicateUidException
	{
		final CustomerData customer = customerFacade.getCurrentCustomer();
		LOG.debug("putCustomer: userId={}", customer.getUid());
		customer.setFirstName(firstName);
		customer.setLastName(lastName);
		customer.setTitleCode(titleCode);
		customer.setLanguage(null);
		customer.setCurrency(null);
		httpRequestCustomerDataPopulator.populate(request, customer);

		customerFacade.updateFullProfile(customer);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "replaceUser", summary = "Updates customer profile", description = "Updates customer profile. Attributes not provided in the request body will be defined again (set to null or default).")
	@ApiBaseSiteIdAndUserIdParam
	public void replaceUser(@Parameter(description = "User's object", required = true) @RequestBody final UserWsDTO user)
			throws DuplicateUidException
	{
		validate(user, "user", putUserDTOValidator);

		final CustomerData customer = customerFacade.getCurrentCustomer();
		LOG.debug("replaceUser: userId={}", customer.getUid());

		getDataMapper().map(user, customer, USER_MAPPER_CONFIG, true);
		customerFacade.updateFullProfile(customer);
	}

	/**
	 * @deprecated since 2005. Please use {@link UsersController#updateUser(UserWsDTO)} instead.
	 */
	@Deprecated(since = "2005", forRemoval = true)
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}", method = RequestMethod.PATCH)
	@ResponseStatus(HttpStatus.OK)
	@Operation(hidden = true, summary = "Updates customer profile", description = "Updates customer profile. Only attributes provided in the request body will be changed.")
	@Parameter(name = "baseSiteId", description = "Base site identifier", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
	@Parameter(name = "userId", description = "User identifier", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
	@Parameter(name = "firstName", description = "Customer's first name", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@Parameter(name = "lastName", description = "Customer's last name", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@Parameter(name = "titleCode", description = "Customer's title code. Customer's title code. For a list of codes, see /{baseSiteId}/titles resource", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@Parameter(name = "language", description = "Customer's language", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	@Parameter(name = "currency", description = "Customer's currency", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
	public void updateUser(final HttpServletRequest request) throws DuplicateUidException
	{
		final CustomerData customer = customerFacade.getCurrentCustomer();
		LOG.debug("updateUser: userId={}", customer.getUid());
		httpRequestCustomerDataPopulator.populate(request, customer);
		customerFacade.updateFullProfile(customer);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}", method = RequestMethod.PATCH, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "updateUser", summary = "Updates customer profile", description = "Updates customer profile. Only attributes provided in the request body will be changed.")
	@ApiBaseSiteIdAndUserIdParam
	public void updateUser(@Parameter(description = "User's object.", required = true) @RequestBody final UserWsDTO user)
			throws DuplicateUidException
	{
		validate(user, "user", patchUserWsDTOValidator);
		final CustomerData customer = customerFacade.getCurrentCustomer();
		LOG.debug("updateUser: userId={}", customer.getUid());

		getDataMapper().map(user, customer, USER_MAPPER_CONFIG, false);
		if (user.getDefaultPointOfServiceName() == null && customer.getSitePreference() != null)
		{
			customer.getSitePreference().setPickUpLocationName(null);
		}
		customerFacade.updateFullProfile(customer);
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeUser", summary = "Delete customer profile.", description = "Removes customer profile.")
	@ApiBaseSiteIdAndUserIdParam
	public void removeUser()
	{
		final CustomerData customer = customerFacade.closeAccount();
		LOG.debug("removeUser: userId={}", customer.getUid());
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}/login", method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "replaceUserLogin", summary = "Changes customer's login name.", description = "Changes a customer's login name. Requires the customer's current password.")
	@ApiBaseSiteIdAndUserIdParam
	public void replaceUserLogin(
			@Parameter(description = "Customer's new login name. Customer login is case insensitive.", required = true) @RequestParam final String newLogin,
			@Parameter(description = "Customer's current password.", required = true) @RequestParam final String password)
			throws DuplicateUidException
	{
		if (!EmailValidator.getInstance().isValid(newLogin))
		{
			throw new RequestParameterException("Login [" + newLogin + "] is not a valid e-mail address!",
					RequestParameterException.INVALID, "newLogin");
		}
		customerFacade.changeUid(newLogin, password);
	}

	@Secured({ "ROLE_CUSTOMERGROUP"})
	@RequestMapping(value = "/{userId}/password", method = RequestMethod.PUT)
	@ResponseStatus(value = HttpStatus.ACCEPTED)
	@Operation(operationId = "replaceUserPassword", summary = "Changes customer's password", description = "Changes customer's password.")
	@ApiBaseSiteIdAndUserIdParam
	public void replaceUserPassword(
			@Parameter(description = "User identifier.", required = true) @PathVariable final String userId,
			@Parameter(description = "Old password.", required = true) @RequestParam(value = "old") final String old,
			@Parameter(description = "New password.", required = true) @RequestParam(value = "new") final String newPassword)
	{
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		final UserSignUpWsDTO customer = new UserSignUpWsDTO();
		customer.setPassword(newPassword);
		validate(customer, "password", passwordStrengthValidator);
		customerFacade.changePassword(old, newPassword);
	}

	protected boolean containsRole(final Authentication auth, final String role)
	{
		for (final GrantedAuthority ga : auth.getAuthorities())
		{
			if (ga.getAuthority().equals(role))
			{
				return true;
			}
		}
		return false;
	}

	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP" })
	@RequestMapping(value = "/{userId}/customergroups", method = RequestMethod.GET)
	@Operation(operationId = "getUserCustomerGroups", summary = "Get all customer groups of a customer.", description = "Returns all customer groups of a customer.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseBody
	public UserGroupListWsDTO getUserCustomerGroups(
			@Parameter(description = "User identifier.", required = true) @PathVariable final String userId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final UserGroupDataList userGroupDataList = new UserGroupDataList();
		userGroupDataList.setUserGroups(customerGroupFacade.getCustomerGroupsForUser(userId));
		return getDataMapper().map(userGroupDataList, UserGroupListWsDTO.class, fields);
	}
}
