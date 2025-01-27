/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.requestfrom;

import de.hybris.platform.commercewebservicescommons.requestfrom.AbstractRequestFromValueSetter;



public class RequestFromValueSetter extends AbstractRequestFromValueSetter
{
	private static final String REQUEST_FROM = "SAP_REQUEST_FROM_";
	@Override
	public void setRequestFrom(final String requestFrom)
	{
		getSessionService().setAttribute(REQUEST_FROM, requestFrom);
	}
}
