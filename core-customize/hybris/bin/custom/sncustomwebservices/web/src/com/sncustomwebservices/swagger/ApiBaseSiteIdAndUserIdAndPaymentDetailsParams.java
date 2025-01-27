/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;


@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
@Parameter(name = "baseSiteId", description = "Base site identifier", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
@Parameter(name = "userId", description = "User identifier", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
@Parameter(name = "accountHolderName", description = "Name on card.", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "cardType", description = "Card type. Call GET /{baseSiteId}/cardtypes beforehand to see what card types are supported.", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "expiryMonth", description = "Month of expiry date.", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "expiryYear", description = "Year of expiry date.", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "issueNumber", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "startMonth", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "startYear", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "subscriptionId", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "saved", description = "Parameter defines if the payment details should be saved for the customer and than could be reused", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "defaultPaymentInfo", description = "Parameter defines if the payment details should be used as default for customer.", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddress.firstName", description = "Customer's first name.", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddress.lastName", description = "Customer's last name.", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddress.titleCode", description = "Customer's title code. For a list of codes, see /{baseSiteId}/titles resource", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddress.country.isocode", description = "Country isocode. This parameter havs influence on how rest of address parameters are validated (e.g. if parameters are required: line1,line2,town,postalCode,region.isocode)", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddress.line1", description = "If this parameter is required depends on country (usually it is required).", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddress.line2", description = "Second part of address. If this parameter is required depends on country (usually it is not required)", schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "bbillingAddress.town", description = "If this parameter is required depends on country (usually it is required)", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddress.postalCode", description = "Postal code. If this parameter is required depends on country (usually it is required)", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "billingAddressregion.isocode", description = "Isocode for region. If this parameter is required depends on country.", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
public @interface ApiBaseSiteIdAndUserIdAndPaymentDetailsParams
{
	//empty
}
