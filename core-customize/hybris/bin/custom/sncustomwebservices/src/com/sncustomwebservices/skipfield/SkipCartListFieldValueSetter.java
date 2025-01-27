/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.EntryArrivalSlotData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import com.sncustomwebservices.order.data.CartDataList;
import de.hybris.platform.commercewebservicescommons.dto.order.CartListWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipCartListFieldValueSetter extends AbstractSkipFieldValueSetter
{
	private static final String SKIP_ORDERENTRY_ARRIVALSLOTS = "SKIP_" + AbstractOrderEntryModel._TYPECODE + "_" + AbstractOrderEntryModel.ARRIVALSLOTS;

	@Override
	public void setValue(final String fields)
	{
		final CartDataList listData = new CartDataList();
		final CartData cartData = new CartData();
		cartData.setRootGroups(List.of(new EntryGroupData()));
		listData.setCarts(List.of(cartData));
		final CartListWsDTO listWsDTO = getDataMapper().map(listData, CartListWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(listWsDTO.getCarts().get(0).getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);

		// Skip Order entry arrival slots
		final CartDataList listCartArrivalSlotData = new CartDataList();
		final CartData cartArrivalSlotData = new CartData();
		OrderEntryData orderEntry =  new OrderEntryData();
		orderEntry.setArrivalSlots(List.of(new EntryArrivalSlotData()));
		cartArrivalSlotData.setEntries(List.of(orderEntry));
		listCartArrivalSlotData.setCarts(List.of(cartArrivalSlotData));
		final CartListWsDTO listCartArrivalSlotWsDTO = getDataMapper().map(listCartArrivalSlotData, CartListWsDTO.class, fields);

		final boolean skipArrivalSlots = CollectionUtils.isNotEmpty(listCartArrivalSlotWsDTO.getCarts())
				&& CollectionUtils.isNotEmpty(listCartArrivalSlotWsDTO.getCarts().get(0).getEntries())
				&& CollectionUtils.isNotEmpty(listCartArrivalSlotWsDTO.getCarts().get(0).getEntries().get(0).getArrivalSlots());

		getSessionService().setAttribute(SKIP_ORDERENTRY_ARRIVALSLOTS, !skipArrivalSlots);
	}
}
