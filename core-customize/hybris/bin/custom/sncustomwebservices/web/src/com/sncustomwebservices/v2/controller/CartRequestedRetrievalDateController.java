/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import static de.hybris.platform.util.localization.Localization.getLocalizedString;

import de.hybris.platform.commercefacades.order.CartRetrievalDateFacade;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Carts")
public class CartRequestedRetrievalDateController
{

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	@Resource(name = "cartRetrievalDateFacade")
	private CartRetrievalDateFacade cartRetrievalDateFacade;

	@Secured(
	{ "ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PutMapping(value = "/{cartId}/requestedretrievaldate")
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "setCartRequestedRetrievalDate", summary = "Sets the requested retrieval date for a cart.", description = "Sets the requested retrieval date with a given date for the cart.")
	@ApiBaseSiteIdUserIdAndCartIdParam
	public void setCartRequestedRetrievalDate(
			@Parameter(description = "Customer requested date for order retrieval", example = "2023-03-29")
			@RequestParam(required = false)
			final String requestedRetrievalAt)
	{
		if (requestedRetrievalAt != null)
		{
			validate(requestedRetrievalAt);
			cartRetrievalDateFacade.updateCartRequestedRetrievalDate(requestedRetrievalAt);
		}

	}

	protected void validate(final String retrievalDate)
	{
		try
		{
			final LocalDate requestedRetrievalDate = LocalDate.parse(retrievalDate, DateTimeFormatter.ISO_LOCAL_DATE);
			final String earliestDate = cartRetrievalDateFacade.getCartEarliestRetrievalDate();
			if (earliestDate != null)
			{
				final LocalDate earliestRetrievalDate = LocalDate.parse(earliestDate, DateTimeFormatter.ISO_LOCAL_DATE);
				if (requestedRetrievalDate.isBefore(earliestRetrievalDate))
				{
					throw new RequestParameterException(getLocalizedString("checkout.multi.requestedretrievaldatevalid.error"),
							RequestParameterException.INVALID);
				}
			}
		}
		catch (final DateTimeParseException e)
		{
			throw new RequestParameterException(getLocalizedString("checkout.multi.requestedretrievaldatevalid.error"),
					RequestParameterException.INVALID);
		}
	}

}
