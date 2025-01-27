/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.EntryArrivalSlotData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercewebservicescommons.dto.order.CartWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.List;

import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipCartFieldValueSetter extends AbstractSkipFieldValueSetter
{
	private static final String SKIP_ORDERENTRY_ARRIVALSLOTS = "SKIP_" + AbstractOrderEntryModel._TYPECODE + "_" + AbstractOrderEntryModel.ARRIVALSLOTS;

	@Override
	public void setValue(final String fields)
	{
		final CartData orderData = new CartData();
		orderData.setRootGroups(List.of(new EntryGroupData()));
		final CartWsDTO orderWsDTO = getDataMapper().map(orderData, CartWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(orderWsDTO.getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);

		// Skip Order entry arrival slots
		final CartData orderArrivalSlotData = new CartData();
		OrderEntryData orderEntry =  new OrderEntryData();
		orderEntry.setArrivalSlots(List.of(new EntryArrivalSlotData()));
		orderArrivalSlotData.setEntries(List.of(orderEntry));
		final CartWsDTO orderArrivalSlotWsDTO = getDataMapper().map(orderArrivalSlotData, CartWsDTO.class, fields);

		final boolean skipArrivalSlots = CollectionUtils.isNotEmpty(orderArrivalSlotWsDTO.getEntries())
				&& CollectionUtils.isNotEmpty(orderArrivalSlotWsDTO.getEntries().get(0).getArrivalSlots());

		getSessionService().setAttribute(SKIP_ORDERENTRY_ARRIVALSLOTS, !skipArrivalSlots);
	}
}
