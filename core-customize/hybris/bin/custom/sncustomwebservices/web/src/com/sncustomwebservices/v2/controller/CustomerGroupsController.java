/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.catalog.PageOption;
import de.hybris.platform.commercefacades.customergroups.CustomerGroupFacade;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.UserGroupOption;
import de.hybris.platform.commercefacades.user.data.PrincipalData;
import de.hybris.platform.commercefacades.user.data.UserGroupData;
import de.hybris.platform.commercefacades.user.data.UserGroupDataList;
import de.hybris.platform.commercewebservicescommons.dto.user.MemberListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.PrincipalWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.UserGroupListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.UserGroupWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import static java.util.stream.Collectors.toSet;


/**
 * Controller for {@link de.hybris.platform.commercefacades.customergroups.CustomerGroupFacade}
 */
@Controller
@RequestMapping(value = "/{baseSiteId}/customergroups")
@Tag(name = "Customer Groups")
public class CustomerGroupsController extends BaseController
{
	private static final Logger LOG = LoggerFactory.getLogger(CustomerGroupsController.class);
	private static final Set<UserGroupOption> OPTIONS = EnumSet.allOf(UserGroupOption.class);
	private static final String REMOVE_OPERATION_MESSAGE = "You cannot remove user from group: ";
	private static final String ADD_OPERATION_MESSAGE = "You cannot add user to group: ";

	@Resource(name = "wsCustomerGroupFacade")
	private CustomerGroupFacade customerGroupFacade;
	@Resource(name = "wsUserFacade")
	private UserFacade userFacade;
	@Resource(name = "wsPrincipalListDTOValidator")
	private Validator principalListDTOValidator;
	@Resource(name = "wsUserGroupDTOValidator")
	private Validator userGroupDTOValidator;

	/**
	 * @deprecated since 2005. Please use {@link CustomerGroupsController#createCustomerGroup(UserGroupWsDTO)} instead.
	 */
	@Deprecated(since = "2005", forRemoval = true)
	@ResponseStatus(value = HttpStatus.CREATED)
	@RequestMapping(method = RequestMethod.POST)
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@Operation(hidden = true, summary = "Creates a customer group.", description = "Creates a new customer group that is a direct subgroup of customerGroups.")
	@ApiBaseSiteIdParam
	public void createCustomerGroup(
			@Parameter(description = "Identifier of new customer group.", required = true) @RequestParam final String groupId,
			@Parameter(description = "Name in current locale.") @RequestParam(required = false) final String localizedName)
	{
		customerGroupFacade.createCustomerGroup(groupId, localizedName);
	}

	@ResponseStatus(value = HttpStatus.CREATED)
	@RequestMapping(method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@Operation(operationId = "createCustomerGroup", summary = "Creates a new customer group.", description =
			"Creates a new customer group that is a direct subgroup of a customergroup.\n\nTo try out the methods of the Customer "
					+ "Groups controller, you must authorize a user who belongs to the “customermanagergroup”.")
	@ApiBaseSiteIdParam
	public void createCustomerGroup(
			@Parameter(description = "User group object with id and name.", required = true) @RequestBody final UserGroupWsDTO userGroup)
	{
		validate(userGroup, "userGroup", userGroupDTOValidator);

		customerGroupFacade.createCustomerGroup(userGroup.getUid(), userGroup.getName());
		if (userGroup.getMembers() != null)
		{
			for (final PrincipalWsDTO member : userGroup.getMembers())
			{
				customerGroupFacade.addUserToCustomerGroup(userGroup.getUid(), member.getUid());
			}
		}
	}

	/**
	 * @deprecated since 2005. Please use {@link CustomerGroupsController#updateCustomerGroupWithUsers(String, MemberListWsDTO)} instead.
	 */
	@Deprecated(since = "2005", forRemoval = true)
	@RequestMapping(value = "/{groupId}/members", method = RequestMethod.PATCH)
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(hidden = true, summary = "List of users to assign to customer group.", description =
			"Assigns user(s) to a customer group.\n\nTo try out the methods of the Customer Groups "
					+ "controller, you must authorize a user who belongs to the “customermanagergroup”.")
	@ApiBaseSiteIdParam
	public void updateCustomerGroupWithUsers(
			@Parameter(description = "Group identifier. ", example = "CustomerForSiteA", required = true) @PathVariable final String groupId,
			@Parameter(description = "List of users ids to assign to customer group. List should be in form: members=uid1&members=uid2...", required = true) @RequestParam(value = "members") final List<String> members)
	{
		checkIfAllUsersExist(members, userId -> createOperationErrorMessage("add", groupId, userId));
		members.forEach(id -> customerGroupFacade.addUserToCustomerGroup(groupId, id));
	}

	@RequestMapping(value = "/{groupId}/members", method = RequestMethod.PATCH, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(operationId = "updateCustomerGroupWithUsers", summary = "Assigns members to a customer group.", description =
			"Assigns members to a customer group. To perform this call, you must be an authorized member of the customermanagergroup.")
	@ApiBaseSiteIdParam
	public void updateCustomerGroupWithUsers(
			@Parameter(description = "Group identifier. ", example = "CustomerForSiteA", required = true) @PathVariable final String groupId,
			@Parameter(description = "List of users to assign to customer group.", required = true) @RequestBody final MemberListWsDTO members)
	{
		validate(members.getMembers(), "members", principalListDTOValidator);
		members.getMembers().forEach(member -> customerGroupFacade.addUserToCustomerGroup(groupId, member.getUid()));
	}

	/**
	 * @deprecated since 2005. Please use {@link CustomerGroupsController#replaceUsersForCustomerGroup(String, MemberListWsDTO)} instead.
	 */
	@Deprecated(since = "2005", forRemoval = true)
	@RequestMapping(value = "/{groupId}/members", method = RequestMethod.PUT)
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(hidden = true, summary = "Assigns members to a customer group.", description =
			"Assigns members to a customer group. By adding members in the request body, you overwrite the existing list. To perform this call, you must be an authorized member of the customermanagergroup.")
	@ApiBaseSiteIdParam
	public void replaceUserListForCustomerGroup(
			@Parameter(description = "Group identifier. ", example = "CustomerForSiteA", required = true) @PathVariable final String groupId,
			@Parameter(description = "List of users ids to assign to customer group. List should be in form: members=uid1&members=uid2...") @RequestParam(required = false, value = "members") final List<String> members)
	{
		setUserListForCustomerGroupInternal(groupId, members);
	}

	protected void setUserListForCustomerGroupInternal(final String groupId, final List<String> members)
	{
		final UserGroupData userGroup = customerGroupFacade.getCustomerGroup(groupId,
				Collections.singleton(UserGroupOption.MEMBERS));
		final Set<String> oldMembers = userGroup.getMembers().stream().map(PrincipalData::getUid).collect(toSet());
		final Set<String> newMembers = Stream.ofNullable(members).flatMap(List::stream).distinct()
				.map(id -> this.toUid(id, userId -> createOperationErrorMessage(ADD_OPERATION_MESSAGE, groupId, userId)))
				.collect(toSet());

		final Set<String> oldRetained = new HashSet<>(oldMembers);
		oldRetained.retainAll(newMembers);
		// to remove
		oldMembers.removeAll(newMembers);
		// to add
		newMembers.removeAll(oldRetained);

		checkIfAllUsersExist(oldMembers, userId -> createOperationErrorMessage(REMOVE_OPERATION_MESSAGE, groupId, userId));
		checkIfAllUsersExist(newMembers, userId -> createOperationErrorMessage(ADD_OPERATION_MESSAGE, groupId, userId));

		oldMembers.forEach(id -> customerGroupFacade.removeUserFromCustomerGroup(groupId, id));
		newMembers.forEach(id -> customerGroupFacade.addUserToCustomerGroup(groupId, id));
	}

	protected String toUid(final String userId, final Function<String, String> messageSupport)
	{
		try
		{
			return userFacade.getUserUID(userId);
		}
		catch (final UnknownIdentifierException ex)
		{
			LOG.debug(ex.getMessage(), ex);
			throw new RequestParameterException(messageSupport.apply(userId));
		}
	}

	/*
	 * Verifies whether users exist for given IDs. If user doesn't exist throw RequestParameterException
	 * @param ids list of user ids
	 * @param messageSupport function that gets error message for given id
	 */
	protected void checkIfAllUsersExist(final Collection<String> ids, final Function<String, String> messageSupport)
	{
		ids.forEach(id -> checkIfUserExist(id, messageSupport));
	}

	protected void checkIfUserExist(final String id, final Function<String, String> messageSupport)
	{
		if (!userFacade.isUserExisting(id))
		{
			throw new RequestParameterException(messageSupport.apply(id));
		}
	}

	@RequestMapping(value = "/{groupId}/members", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(operationId = "replaceUsersForCustomerGroup", summary = "Sets members for a user group.", description =
			"Sets members for a user group. The list of existing members is overwritten with a new one.\n\nTo try out the methods "
					+ "of the Customer Groups controller, you must authorize a user who belongs to the “customermanagergroup”.")
	@ApiBaseSiteIdParam
	public void replaceUsersForCustomerGroup(
			@Parameter(description = "Group identifier. ", example = "CustomerForSiteA", required = true) @PathVariable final String groupId,
			@Parameter(description = "List of users to set for customer group.", required = true) @RequestBody final MemberListWsDTO members)
	{
		final List<String> membersIds = new ArrayList<>();
		if (members.getMembers() != null)
		{
			if (!members.getMembers().isEmpty())
			{
				validate(members.getMembers(), "members", principalListDTOValidator);
			}

			for (final PrincipalWsDTO member : members.getMembers())
			{
				membersIds.add(member.getUid());
			}
		}
		setUserListForCustomerGroupInternal(groupId, membersIds);
	}

	@RequestMapping(value = "/{groupId}/members/{userId:.*}", method = RequestMethod.DELETE)
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(operationId = "removeUsersFromCustomerGroup", summary = "Deletes the customer from a customer group.", description =
			"Deletes the customer from a customer group. To try out the methods in the customer groups controller, authorize a customer who belongs to the customermanagergroup.")
	@ApiBaseSiteIdParam
	public void removeUsersFromCustomerGroup(
			@Parameter(description = "Group identifier.", example = "CustomerForSiteA", required = true) @PathVariable final String groupId,
			@Parameter(description = "User identifier or one of the literals: ‘current’ for currently authenticated user, ‘anonymous’ for anonymous user.", required = true) @PathVariable(value = "userId") final String userId)
	{
		checkIfUserExist(userId, id -> createOperationErrorMessage(REMOVE_OPERATION_MESSAGE, groupId, userId));
		customerGroupFacade.removeUserFromCustomerGroup(groupId, userId);
	}

	@RequestMapping(method = RequestMethod.GET)
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	@Operation(operationId = "getCustomerGroups", summary = "Retrieves the subgroups of a customer group.", description =
			"Retrieves the subgroup information of a customer group. To perform this call, you must be an authorized member of the customermanagergroup.")
	@ApiBaseSiteIdParam
	public UserGroupListWsDTO getCustomerGroups(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@ApiFieldsParam(defaultValue = BASIC_FIELD_SET) @RequestParam(defaultValue = BASIC_FIELD_SET) final String fields)
	{
		final PageOption pageOption = PageOption.createForPageNumberAndPageSize(currentPage, pageSize);
		final UserGroupDataList userGroupDataList = customerGroupFacade.getAllCustomerGroups(pageOption);
		return getDataMapper().map(userGroupDataList, UserGroupListWsDTO.class, fields);
	}

	@RequestMapping(value = "/{groupId}", method = RequestMethod.GET)
	@Secured("ROLE_CUSTOMERMANAGERGROUP")
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	@Operation(operationId = "getCustomerGroup", summary = "Retrieves a customer group.", description =
			"Retrieves a customer group. To try out the methods of the customer groups controller, authorize a customer who belongs to the customermanagergroup.")
	@ApiBaseSiteIdParam
	public UserGroupWsDTO getCustomerGroup(
			@Parameter(description = "Group identifier.", example = "CustomerForSiteA",  required = true) @PathVariable final String groupId,
			@ApiFieldsParam(defaultValue = BASIC_FIELD_SET) @RequestParam(defaultValue = BASIC_FIELD_SET) final String fields)
	{
		final UserGroupData userGroupData = customerGroupFacade.getCustomerGroup(groupId, OPTIONS);
		return getDataMapper().map(userGroupData, UserGroupWsDTO.class, fields);
	}

	protected String createOperationErrorMessage(final String operationMessage, final String groupId, final String userId)
	{
		return String.join("", operationMessage, sanitize(groupId), ". User '", sanitize(userId),
				"' doesn't exist or you have no privileges");
	}

}
