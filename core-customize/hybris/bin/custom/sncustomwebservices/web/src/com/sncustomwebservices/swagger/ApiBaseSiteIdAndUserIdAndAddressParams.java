/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.swagger;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(value = { ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
@Parameter(name = "baseSiteId", description = "Base site identifier", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
@Parameter(name = "userId", description = "User identifier", required = true, schema = @Schema(type = "string"), in = ParameterIn.PATH)
@Parameter(name = "firstName", description = "Customer's first name", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "lastName", description = "Customer's last name", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "titleCode", description = "Customer's title code. Customer's title code. For a list of codes, see /{baseSiteId}/titles resource", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "country.isocode", description = "Country isocode. This parameter is required and have influence on how rest of parameters are validated (e.g. if parameters are required : line1,line2,town,postalCode,region.isocode)", required = true, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "line1", description = "First part of address. If this parameter is required depends on country (usually it is required).", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "line2", description = "Second part of address. If this parameter is required depends on country (usually it is not required)", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "town", description = "Town name. If this parameter is required depends on country (usually it is required)", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "postalCode", description = "Postal code. Isocode for region. If this parameter is required depends on country.", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "region.isocode", description = "Isocode for region. If this parameter is required depends on country.", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
@Parameter(name = "defaultAddress", description = "Parameter specifies if address should be default for customer.", required = false, schema = @Schema(type = "string"), in = ParameterIn.QUERY)
public @interface ApiBaseSiteIdAndUserIdAndAddressParams
{
	//empty
}
