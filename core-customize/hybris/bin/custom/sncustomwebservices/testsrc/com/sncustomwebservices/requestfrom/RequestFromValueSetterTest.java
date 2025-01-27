/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.requestfrom;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.session.SessionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.jgroups.util.Util.assertEquals;
import static org.mockito.Mockito.verify;



@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class RequestFromValueSetterTest
{
	private static final String TEST_CONTROLLER = "TEST_CONTROLLER";
	private static final String TEST_REQUEST_SOURCE = "SAP_REQUEST_FROM_";
	@InjectMocks
	private RequestFromValueSetter requestFromValueSetter;
	@Mock
	protected SessionService sessionService;

	@Before
	public void setUp()
	{
		requestFromValueSetter.setSessionService(sessionService);
	}

	@Test
	public void testRequestFrom()
	{
		requestFromValueSetter.setRequestFrom(TEST_CONTROLLER);
		verify(sessionService).setAttribute(TEST_REQUEST_SOURCE, TEST_CONTROLLER);
	}
	@Test
	public void testGetSessionService() {
		SessionService result = requestFromValueSetter.getSessionService();
		assertEquals(sessionService, result);
	}
}
