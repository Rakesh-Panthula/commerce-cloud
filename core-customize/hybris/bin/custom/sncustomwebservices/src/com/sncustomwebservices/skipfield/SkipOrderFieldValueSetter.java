/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.order.data.ConsignmentData;
import de.hybris.platform.commercefacades.order.data.EntryArrivalSlotData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.ArrayList;
import java.util.List;

import de.hybris.platform.ordersplitting.model.ConsignmentModel;
import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipOrderFieldValueSetter extends AbstractSkipFieldValueSetter
{
	private static final String SKIP_CONSIGNMENT_ARRIVALSLOT = "SKIP_" + ConsignmentModel._TYPECODE + "_" + ConsignmentModel.ARRIVALSLOT;

	@Override
	public void setValue(final String fields)
	{
		final OrderData orderData = new OrderData();
		orderData.setRootGroups(List.of(new EntryGroupData()));
		final OrderWsDTO orderWsDTO = getDataMapper().map(orderData, OrderWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(orderWsDTO.getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);

		// Skip consignment arrival slot
		final OrderData orderDataArrivalSlot = new OrderData();
		List<ConsignmentData> consignments = new ArrayList<>();
		ConsignmentData consignment = new ConsignmentData();
		consignment.setArrivalSlot(new EntryArrivalSlotData());
		consignments.add(consignment);
		orderDataArrivalSlot.setConsignments(consignments);
		final OrderWsDTO orderArrivalSlotWsDTO = getDataMapper().map(orderDataArrivalSlot, OrderWsDTO.class, fields);

		final boolean skipOrderArrivalSlots = CollectionUtils.isNotEmpty(orderArrivalSlotWsDTO.getConsignments())
				&& orderArrivalSlotWsDTO.getConsignments().get(0).getArrivalSlot() != null;
		getSessionService().setAttribute(SKIP_CONSIGNMENT_ARRIVALSLOT, !skipOrderArrivalSlots);
	}
}
