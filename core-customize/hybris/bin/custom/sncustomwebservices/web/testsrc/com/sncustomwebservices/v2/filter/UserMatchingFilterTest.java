/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.filter;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commerceservices.enums.SiteChannel;
import de.hybris.platform.commerceservices.user.UserMatchingService;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.site.BaseSiteService;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * Test suite for {@link UserMatchingFilter}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class UserMatchingFilterTest
{
	static final String DEFAULT_REGEXP = "^/[^/]+/users/([^/]+)";
	static final String ANONYMOUS_UID = "anonymous";
	static final String CUSTOMER_UID = "customerUID";
	static final String CUSTOMER_MANAGER_UID = "customerManagerUID";
	static final String ROLE_UNKNOWN = "ROLE_UNKNOWN";
	final String agentUserId = "asagent";
	final String customerUserId = "william.hunter@pronto-hw.com";
	public static final String ROLE_ASAGENTGROUP = "ROLE_ASAGENTGROUP";
	private UserMatchingFilter userMatchingFilter;
	@Mock
	private HttpServletRequest httpServletRequest;
	@Mock
	private HttpServletResponse httpServletResponse;
	@Mock
	private FilterChain filterChain;
	@Mock
	private UserService userService;
	@Mock
	private UserMatchingService userMatchingService;
	@Mock
	private SessionService sessionService;
	@Mock
	private BaseSiteService baseSiteService;
	@Mock
	private BaseSiteModel baseSiteModel;
	@Mock
	private CustomerModel principalUserModel;
	@Mock
	private CustomerModel customerUserModel;
	@Mock
	private CustomerModel anonymousUserModel;
	@Mock
	private TestingAuthenticationToken authentication;
	@Mock
	private GrantedAuthority grantedAuthority;
	private Collection<GrantedAuthority> authorities;

	@Before
	public void setUp()
	{
		userMatchingFilter = new UserMatchingFilter()
		{
			@Override
			protected Authentication getAuth()
			{
				return authentication;
			}
		};
		userMatchingFilter.setRegexp(DEFAULT_REGEXP);
		userMatchingFilter.setUserService(userService);
		userMatchingFilter.setUserMatchingService(userMatchingService);
		userMatchingFilter.setSessionService(sessionService);
		userMatchingFilter.setBaseSiteService(baseSiteService); 
		authorities = new ArrayList<>();
		given(userService.getAnonymousUser()).willReturn(anonymousUserModel);
		given(httpServletRequest.getDispatcherType()).willReturn(DispatcherType.REQUEST);
		given(baseSiteService.getCurrentBaseSite()).willReturn(baseSiteModel);
		given(baseSiteModel.getChannel()).willReturn(SiteChannel.B2B); 
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/" + customerUserId + "/and/more");
	}

	public void createAuthority(final String role, final String principal)
	{
		given(grantedAuthority.getAuthority()).willReturn(role);
		authorities.add(grantedAuthority);
		given(authentication.getAuthorities()).willReturn(authorities);
		given(authentication.getPrincipal()).willReturn(principal);
		given(principalUserModel.getUid()).willReturn(principal);
		given(userMatchingService.getUserByProperty(principal, UserModel.class)).willReturn(principalUserModel);

	}

	private void createAuthorities(final String principal, final String... roles)
	{
		authorities.clear();
		for (String role : roles)
		{
			GrantedAuthority grantedAuthority = new GrantedAuthority()
			{
				@Override
				public String getAuthority()
				{
					return role;
				}
			};
			authorities.add(grantedAuthority);
		}
		given(authentication.getAuthorities()).willReturn(authorities);
		given(authentication.getPrincipal()).willReturn(principal);
		given(principalUserModel.getUid()).willReturn(principal);
		given(userMatchingService.getUserByProperty(principal, UserModel.class)).willReturn(principalUserModel);
	}


	public void testNullPathInfo(final String role, final String principal) throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn(null);
		createAuthority(role, principal);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(principalUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test
	public void testNullPathInfoOnAnonymous() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn(null);
		given(grantedAuthority.getAuthority()).willReturn(UserMatchingFilter.ROLE_ANONYMOUS);
		authorities.add(grantedAuthority);
		given(authentication.getAuthorities()).willReturn(authorities);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(anonymousUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test
	public void testNullPathInfoOnCustomer() throws ServletException, IOException
	{
		given(baseSiteModel.getChannel()).willReturn(SiteChannel.B2C);
		testNullPathInfo(UserMatchingFilter.ROLE_CUSTOMERGROUP, CUSTOMER_UID);
	}

	@Test
	public void testNullPathInfoOnCustomerManager() throws ServletException, IOException
	{
		testNullPathInfo(UserMatchingFilter.ROLE_CUSTOMERMANAGERGROUP, CUSTOMER_MANAGER_UID);
	}

	@Test
	public void testNotMatchingPathForTrustedClient() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/test/some/longer/path");
		createAuthority(UserMatchingFilter.ROLE_TRUSTED_CLIENT, "trusted_client");

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(anonymousUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test(expected = AccessDeniedException.class)
	public void testMatchingPathForUnknownRole() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/admin");
		createAuthority(ROLE_UNKNOWN, "unknown");

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	}

	public void testMatchingPathForCustomerManagingUser(final String role, final String principal)
			throws IOException, ServletException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/" + CUSTOMER_UID + "/and/more");
		createAuthority(role, principal);
		given(userMatchingService.getUserByProperty(CUSTOMER_UID, UserModel.class)).willReturn(customerUserModel);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(customerUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test
	public void testMatchingPathForTrustedClient() throws ServletException, IOException
	{
		testMatchingPathForCustomerManagingUser(UserMatchingFilter.ROLE_TRUSTED_CLIENT, "trusted_client");
	}

	@Test
	public void testMatchingPathForCustomerManager() throws ServletException, IOException
	{
		testMatchingPathForCustomerManagingUser(UserMatchingFilter.ROLE_CUSTOMERMANAGERGROUP, "customermanager");
	}

	@Test
	public void testMatchingPathForAuthenticatedCustomer() throws ServletException, IOException
	{
		given(baseSiteModel.getChannel()).willReturn(SiteChannel.B2C);
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/" + CUSTOMER_UID + "/and/more");
		createAuthority(UserMatchingFilter.ROLE_CUSTOMERGROUP, CUSTOMER_UID);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(principalUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test(expected = AccessDeniedException.class)
	public void testFailMatchingPathForUnauthenticatedCustomer() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/admin/and/more");
		given(userMatchingService.getUserByProperty("admin", UserModel.class)).willThrow(UnknownIdentifierException.class);
		createAuthority(UserMatchingFilter.ROLE_CUSTOMERGROUP, CUSTOMER_UID);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	}

	@Test
	public void testMatchingFilterForAnonymousUser() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/" + ANONYMOUS_UID + "/and/more");

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(anonymousUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test
	public void testMatchingPathForCurrentCustomer() throws ServletException, IOException
	{
		given(baseSiteModel.getChannel()).willReturn(SiteChannel.B2C);
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/current/and/more");
		createAuthority(UserMatchingFilter.ROLE_CUSTOMERGROUP, CUSTOMER_UID);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(principalUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test(expected = AccessDeniedException.class)
	public void testExceptionForCurrentCustomer() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/current/and/more");
		createAuthority(UserMatchingFilter.ROLE_CUSTOMERGROUP, CUSTOMER_UID);
		given(userMatchingService.getUserByProperty(CUSTOMER_UID, UserModel.class)).willReturn(principalUserModel);

		given(principalUserModel.getUid()).willReturn(CUSTOMER_UID + "diff");

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(principalUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}


	@Test(expected = AccessDeniedException.class)
	public void testB2CCustomerCannotAccessB2BSite() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/" + CUSTOMER_UID + "/and/more");
		createAuthority(UserMatchingFilter.ROLE_CUSTOMERGROUP, CUSTOMER_UID);

		given(baseSiteModel.isRequiresAuthentication()).willReturn(true);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	}

	@Test
	public void testB2BGroupUserCanAccessOwnInfoOnB2BSite() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/wsTest/users/" + customerUserId + "/and/more");
		createAuthorities(customerUserId, UserMatchingFilter.ROLE_B2BGROUP, UserMatchingFilter.ROLE_CUSTOMERGROUP);
		given(userMatchingService.getUserByProperty(customerUserId, UserModel.class)).willReturn(principalUserModel);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(principalUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test
	public void testAsAgentGroupUserAccessingCustomerInfoOnB2BSite() throws ServletException, IOException
	{
		createAuthorities(agentUserId, ROLE_ASAGENTGROUP, UserMatchingFilter.ROLE_CUSTOMERMANAGERGROUP);
		given(userMatchingService.getUserByProperty(customerUserId, UserModel.class)).willReturn(customerUserModel);

		userMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(userService, times(1)).setCurrentUser(customerUserModel);
		verify(filterChain, times(1)).doFilter(httpServletRequest, httpServletResponse);
	}
}
