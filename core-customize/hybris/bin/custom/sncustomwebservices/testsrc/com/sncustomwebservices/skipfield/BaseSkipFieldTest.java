package com.sncustomwebservices.skipfield;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class BaseSkipFieldTest
{
	protected static final String FIELD_ENTRIES = "entries(FULL)";
	@Mock
	protected SessionService sessionService;
	@Mock
	protected DataMapper dataMapper;
}
