/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import com.sncustomwebservices.returns.data.ReturnRequestsData;
import de.hybris.platform.commercewebservicescommons.dto.order.ReturnRequestListWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;
import de.hybris.platform.ordermanagementfacades.returns.data.ReturnRequestData;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipReturnRequestListFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final ReturnRequestsData requests = new ReturnRequestsData();
		final ReturnRequestData result = new ReturnRequestData();
		final OrderData orderData = new OrderData();
		orderData.setRootGroups(List.of(new EntryGroupData()));
		result.setOrder(orderData);
		requests.setReturnRequests(List.of(result));
		final ReturnRequestListWsDTO listWsDTO = getDataMapper().map(requests, ReturnRequestListWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(listWsDTO.getReturnRequests().get(0).getOrder().getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
