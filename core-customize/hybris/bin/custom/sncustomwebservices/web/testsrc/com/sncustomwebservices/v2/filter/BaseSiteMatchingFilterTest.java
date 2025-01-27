/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.filter;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.site.BaseSiteService;
import com.sncustomwebservices.exceptions.InvalidResourceException;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * Test suite for {@link BaseSiteMatchingFilter}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class BaseSiteMatchingFilterTest
{
	static final String DEFAULT_REGEXP = "^/([^/]+)";
	static final String UNKNOWN_BASE_SITE_ID = "unknownBaseSiteId";
	static final String BASE_SITE_ID = "baseSiteID";
	private BaseSiteMatchingFilter baseSiteMatchingFilter;
	@Mock
	private BaseSiteService baseSiteService;
	@Mock
	private HttpServletRequest httpServletRequest;
	@Mock
	private HttpServletResponse httpServletResponse;
	@Mock
	private FilterChain filterChain;
	@Mock
	private BaseSiteModel baseSiteModel;
	@Mock
	private BaseSiteModel currentBaseSiteModel;

	@Before
	public void setUp()
	{
		baseSiteMatchingFilter = new BaseSiteMatchingFilter();
		baseSiteMatchingFilter.setRegexp(DEFAULT_REGEXP);
		baseSiteMatchingFilter.setBaseSiteService(baseSiteService);
		given(httpServletRequest.getDispatcherType()).willReturn(DispatcherType.REQUEST);
	}

	@Test
	public void testNullPathInfo() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn(null);

		baseSiteMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(baseSiteService, never()).setCurrentBaseSite(any(BaseSiteModel.class), anyBoolean());
		verify(baseSiteService, never()).setCurrentBaseSite(anyString(), anyBoolean());
		verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test(expected = InvalidResourceException.class)
	public void testUnknownBaseSite() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/" + UNKNOWN_BASE_SITE_ID);
		given(baseSiteService.getBaseSiteForUID(UNKNOWN_BASE_SITE_ID)).willReturn(null);

		baseSiteMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test
	public void testKnownBaseSite() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/" + BASE_SITE_ID + "/some/longer/path");
		given(baseSiteService.getBaseSiteForUID(BASE_SITE_ID)).willReturn(baseSiteModel);
		given(baseSiteService.getCurrentBaseSite()).willReturn(currentBaseSiteModel);

		baseSiteMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(baseSiteService, times(1)).setCurrentBaseSite(baseSiteModel, true);
		verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
	}

	@Test
	public void testBaseSiteThatEqualsCurrentSite() throws ServletException, IOException
	{
		given(httpServletRequest.getPathInfo()).willReturn("/" + BASE_SITE_ID + "/some/longer/path");
		given(baseSiteService.getBaseSiteForUID(BASE_SITE_ID)).willReturn(baseSiteModel);
		given(baseSiteService.getCurrentBaseSite()).willReturn(baseSiteModel);

		baseSiteMatchingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(baseSiteService, never()).setCurrentBaseSite(baseSiteModel, true);
		verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
	}

}
