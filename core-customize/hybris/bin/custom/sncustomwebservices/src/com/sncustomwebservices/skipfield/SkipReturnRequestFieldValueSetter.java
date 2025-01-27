/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;
import de.hybris.platform.ordermanagementfacades.returns.data.ReturnRequestData;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipReturnRequestFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final ReturnRequestData resultData = new ReturnRequestData();
		final OrderData orderData = new OrderData();
		orderData.setRootGroups(List.of(new EntryGroupData()));
		resultData.setOrder(orderData);
		final ReturnRequestWsDTO requestWsDTO = getDataMapper().map(resultData, ReturnRequestWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(requestWsDTO.getOrder().getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
