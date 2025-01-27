/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.commercefacades.product.availability.SapProductAvailabilityFacade;
import de.hybris.platform.commercefacades.product.data.SapAvailabilityData;
import de.hybris.platform.commercefacades.product.data.SapProductAvailabilityQueryContext;
import de.hybris.platform.commercewebservicescommons.dto.availability.SapAvailabilityWsDTO;
import de.hybris.platform.servicelayer.exceptions.SystemException;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import com.sncustomwebservices.validator.SapProductAvailabilityFilterValidator;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.InternalServerErrorException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Web Services Controller to expose the functionality of the
 * {@link de.hybris.platform.commercefacades.product.availability.SapProductAvailabilityFacade}.
 */

@Controller
@Tag(name = "Product Availability")
@RequestMapping(value = "/{baseSiteId}/productAvailabilities")
public class ProductAvailabilityController extends BaseController
{
	@Resource(name = "sapProductAvailabilityFacade")
	private SapProductAvailabilityFacade productAvailabilityFacade;

	@Resource
	private SapProductAvailabilityFilterValidator filterValidator;

	@GetMapping(produces = "application/json")
	@ResponseBody
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SapAvailabilityWsDTO.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorListWsDTO.class), examples = @ExampleObject(value = "{\"errors\":[{\"message\":\"Base site electronicsd doesn't exist\",\"type\":\"InvalidResourceError\"}]}"))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorListWsDTO.class), examples = @ExampleObject(value = "{\"errors\":[{\"type\":\"InvalidTokenError\",\"message\":\"Invalid access token\"}]}"))),
			@ApiResponse(responseCode = "403", description = "Forbidden. Have no access to this method", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorListWsDTO.class))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorListWsDTO.class))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorListWsDTO.class), examples = @ExampleObject(value = "{\"errors\":[{\"message\":\"System not maintain an unit for product 1934793 , please check!\",\"type\":\"SystemError\"}]}"))) })
	@Operation(operationId = "getProductAvailability", summary = "Retrieves availability information for a list of products.", description = "Retrieves the stock availability for the requested products in the requested units. This endpoint is added in the 2211.30 update.”")
	@ApiBaseSiteIdParam
	public SapAvailabilityWsDTO getProductAvailability(@Parameter(description = "Product codes and their respective units in which the availability should be retrieved. In the following format - \n\n" +
			"productCodeA:unitCodeA,unitCodeB;productCodeB:unitCodeA,unitCodeB.\n\n Example - 3318057_A:EA,PC;4112097_B:EA", required = true) @RequestParam @NotBlank final String filters)
	{
		final SapProductAvailabilityQueryContext queryContext = filterValidator.decodeAndValidateFilters(filters);

		final SapAvailabilityData availabilityData = productAvailabilityFacade.getAvailabilityForProductsAndUnits(queryContext);

		return getDataMapper().map(availabilityData, SapAvailabilityWsDTO.class);
	}

	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	@ExceptionHandler({ InternalServerErrorException.class, SystemException.class })
	public ErrorListWsDTO handleInternalServerError(final Throwable ex)
	{
		return handleErrorInternal(ex.getClass().getSimpleName(), ex.getMessage());
	}
}
