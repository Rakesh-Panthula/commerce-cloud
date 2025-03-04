/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.UserWsDTO;
import de.hybris.platform.storelocator.model.PointOfServiceModel;
import de.hybris.platform.storelocator.pos.PointOfServiceService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


/**
 * Default commerce web services point of service validator. Checks if point of service with given name exist.
 */
public class PointOfServiceValidator implements Validator
{
	private PointOfServiceService pointOfServiceService;
	private String fieldPath;

	@Override
	public boolean supports(final Class<?> clazz)
	{
		return String.class.equals(clazz)
				|| OrderEntryData.class.isAssignableFrom(clazz)
				|| OrderEntryWsDTO.class.isAssignableFrom(clazz)
				|| UserWsDTO.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(final Object target, final Errors errors)
	{
		final String storeName = fieldPath == null ? (String) target : (String) errors.getFieldValue(fieldPath);

		if (StringUtils.isNotEmpty(storeName))
		{
			final PointOfServiceModel pointOfServiceModel = getPointOfServiceService().getPointOfServiceForName(storeName);
			if (pointOfServiceModel == null)
			{
				errors.rejectValue(getFieldPath(), "pointOfService.notExists",
						new String[] { storeName }, "Store with the name '{0}' doesn't exist");
			}
		}
	}

	protected PointOfServiceService getPointOfServiceService()
	{
		return pointOfServiceService;
	}

	@Required
	public void setPointOfServiceService(final PointOfServiceService pointOfServiceService)
	{
		this.pointOfServiceService = pointOfServiceService;
	}

	public String getFieldPath()
	{
		return fieldPath;
	}

	public void setFieldPath(final String fieldPath)
	{
		this.fieldPath = fieldPath;
	}

}
