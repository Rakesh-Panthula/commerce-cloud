/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.skipfield;

import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CommerceSaveCartResultData;
import de.hybris.platform.commercewebservicescommons.dto.order.SaveCartResultWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipSaveCartResultFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final CommerceSaveCartResultData resultData = new CommerceSaveCartResultData();
		final CartData cartData = new CartData();

		cartData.setRootGroups(List.of(new EntryGroupData()));
		resultData.setSavedCartData(cartData);
		final SaveCartResultWsDTO resultWsDTO = getDataMapper().map(resultData, SaveCartResultWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(resultWsDTO.getSavedCartData().getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
