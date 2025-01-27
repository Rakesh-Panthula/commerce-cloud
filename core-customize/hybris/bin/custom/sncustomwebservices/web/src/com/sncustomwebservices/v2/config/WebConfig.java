/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.config;


import de.hybris.platform.servicelayer.config.ConfigurationService;
import com.sncustomwebservices.request.mapping.handler.CommerceHandlerMapping;

import javax.annotation.Resource;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.ResourceUrlProvider;


/**
 * Spring configuration which replace <mvc:annotation-driven> tag. It allows override default
 * RequestMappingHandlerMapping with our own mapping handler
 */

@Configuration
@ImportResource({ "WEB-INF/config/v2/springmvc-v2-servlet.xml" })
public class WebConfig extends DelegatingWebMvcConfiguration
{

	private static final String LEGACY_CONTENT_NEGOTIATION = "commercewebservices.content.negotiation.legacy";

	@Resource(name = "configurationService")
	private ConfigurationService configurationService;

	@Resource(name = "messageConvertersV2")
	private List<HttpMessageConverter<?>> messageConvertersV2;

	@Resource
	private List<HandlerExceptionResolver> exceptionResolversV2;

	private ApplicationContext applicationContext;

	@Value("${sncustomwebservices.core.v2.config.webconfig.MultipartResolver.maxUploadSize}")
	private int maximumUploadSizeForMultipartResolver;

	@Override
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping(final ContentNegotiationManager mvcContentNegotiationManager,
			final FormattingConversionService mvcConversionService, final ResourceUrlProvider mvcResourceUrlProvider)
	{
		final CommerceHandlerMapping handlerMapping = new CommerceHandlerMapping("v2");
		handlerMapping.setOrder(0);
		handlerMapping.setDetectHandlerMethodsInAncestorContexts(true);
		handlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
		handlerMapping.setContentNegotiationManager(mvcContentNegotiationManager);

		if (isLegacyContentNegotiationEnabled())
		{
			/*
			 * For more details about deprecation see: https://github.com/spring-projects/spring-framework/issues/24179
			 */
			handlerMapping.setUseRegisteredSuffixPatternMatch(true);
		}
		return handlerMapping;
	}

	@Override
	protected void configureMessageConverters(final List<HttpMessageConverter<?>> converters)
	{
		converters.addAll(messageConvertersV2);
		super.addDefaultHttpMessageConverters(converters);
	}

	@Override
	protected void configureHandlerExceptionResolvers(final List<HandlerExceptionResolver> exceptionResolvers)
	{
		final ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver = new ExceptionHandlerExceptionResolver();
		exceptionHandlerExceptionResolver.setApplicationContext(applicationContext);
		exceptionHandlerExceptionResolver.setContentNegotiationManager(mvcContentNegotiationManager());
		exceptionHandlerExceptionResolver.setMessageConverters(getMessageConverters());
		exceptionHandlerExceptionResolver.afterPropertiesSet();

		exceptionResolvers.add(exceptionHandlerExceptionResolver);
		exceptionResolvers.addAll(exceptionResolversV2);
		exceptionResolvers.add(new ResponseStatusExceptionResolver());
		exceptionResolvers.add(new DefaultHandlerExceptionResolver());
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
	{
		super.setApplicationContext(applicationContext);
		this.applicationContext = applicationContext;
	}

	@Override
	public void configureContentNegotiation(final ContentNegotiationConfigurer configurer)
	{
		if (isLegacyContentNegotiationEnabled())
		{
			configurer.favorPathExtension(false).favorParameter(true);
		}
	}

	@Bean
	public MultipartResolver multipartResolver() {
		final CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
		multipartResolver.setMaxUploadSize(maximumUploadSizeForMultipartResolver);
		return multipartResolver;
	}
	
	protected boolean isLegacyContentNegotiationEnabled()
	{
		return configurationService.getConfiguration().getBoolean(LEGACY_CONTENT_NEGOTIATION, false);
	}

}
