/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package com.sncustomwebservices.v2.config;


import de.hybris.platform.swagger.ApiDocInfo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;


@Configuration
@PropertySource("classpath:/v2/swagger.properties")
@ImportResource(value = "classpath*:/swagger/swaggerintegration/web/spring/*-web-spring.xml")
public class CommerceSwaggerConfig
{
	@Bean("apiDocInfo")
	public ApiDocInfo apiDocInfo()
	{
		return () -> "sncustomwebservices.v2";
	}
}
