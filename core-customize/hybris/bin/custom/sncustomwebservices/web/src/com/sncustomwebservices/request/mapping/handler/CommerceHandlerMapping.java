/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.request.mapping.handler;


import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commerceservices.request.mapping.annotation.RequestMappingOverride;
import de.hybris.platform.util.Config;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


/**
 * Creates {@link RequestMappingInfo} instances from type and method-level {@link RequestMapping @RequestMapping}
 * annotations in {@link Controller @Controller} classes. Additionally it resolve RequestMapping conflicts base on
 * {@link RequestMappingOverride @RequestMappingOverride} annotation. Method with highest priority will be added as
 * handler mapping. CommerceHandlerMapping use also {@link ApiVersion @ApiVersion} annotation to avoid adding request
 * mapping dedicated for different API version of commerce web services
 */
public class CommerceHandlerMapping extends RequestMappingHandlerMapping
{
	private static final Logger LOG = LoggerFactory.getLogger(CommerceHandlerMapping.class);

	private final Map<RequestMappingInfo, Integer> overriddenRequestMapping = new LinkedHashMap<RequestMappingInfo, Integer>();
	/**
	 * Api version for which this request handler mapping was defined
	 */
	private final String apiVersion;
	private boolean detectInAncestorContexts;
	private int defaultRequestMappingOverridePriority = 0;

	/**
	 * @param apiVersion - Api version for which this request handler mapping should be defined
	 */
	public CommerceHandlerMapping(final String apiVersion)
	{
		super();
		this.apiVersion = apiVersion;
	}


	@Override
	protected boolean isHandler(final Class<?> beanType)
	{
		if (super.isHandler(beanType))
		{
			final ApiVersion v = AnnotationUtils.findAnnotation(beanType, ApiVersion.class);
			return v == null ? true : v.value().equals(apiVersion);
		}

		return false;
	}


	/**
	 * Scan beans in the ApplicationContext, detect and register handler methods. Additionally this method detect which
	 * request mapping was overridden
	 */
	@Override
	protected void initHandlerMethods()
	{
		initOverridenRequestMappings();
		super.initHandlerMethods();
	}

	/**
	 * Gets mapping from parent method and check if it wasn't overridden by {@link RequestMappingOverride} annotation
	 *
	 * @return the created RequestMappingInfo, or {@code null} if the method does not have a {@code @RequestMapping}
	 * annotation or mapping was overridden by {@code RequestMappingOverride} annotation.
	 */
	@Override
	protected RequestMappingInfo getMappingForMethod(final Method method, final Class<?> handlerType)
	{
		final RequestMappingInfo mapping = super.getMappingForMethod(method, handlerType);

		if (mapping != null && isRequestMappingOverridden(mapping, method))
		{
			return null;
		}

		return mapping;
	}

	/**
	 * Method checks if mapping was overridden
	 */
	protected boolean isRequestMappingOverridden(final RequestMappingInfo mapping, final Method method)
	{
		final Integer maxPriority = overriddenRequestMapping.get(mapping);
		if (maxPriority == null)
		{
			return false;
		}
		final Integer methodPriority = getMethodPriorityValue(method);
		if (maxPriority.equals(methodPriority))
		{
			return false;
		}

		return true;
	}

	/**
	 * Checks if method has {@code RequestMappingOverride} annotation
	 */
	protected boolean hasRequestMappingOverrideAnnotation(final Method method)
	{
		final RequestMappingOverride requestMappingOverride = AnnotationUtils.findAnnotation(method, RequestMappingOverride.class);
		return requestMappingOverride != null;
	}

	/**
	 * Gets method priority value based on {@code RequestMappingOverride} annotation. Priority value is read from
	 * properties files (project.properties, local.properties). If there is no {@code priorityProperty} then
	 * {@code <className>.<methodName>.priority} will be used as property name. If there is no value for property with
	 * given name, default value = 0 will be returned.
	 */
	protected Integer getMethodPriorityValue(final Method method)
	{
		final RequestMappingOverride requestMappingOverride = AnnotationUtils.findAnnotation(method, RequestMappingOverride.class);
		if (requestMappingOverride != null)
		{
			String priorityProperty = requestMappingOverride.priorityProperty();
			if (priorityProperty.isEmpty())
			{
				priorityProperty =
						"requestMappingOverride." + method.getDeclaringClass().getName() + "." + method.getName() + ".priority";
			}

			return Integer.valueOf(Config.getInt(priorityProperty, getDefaultRequestMappingOverridePriority()));
		}
		return null;
	}

	/**
	 * Scan beans in the ApplicationContext, detect and register overridden request mappings.
	 */
	protected void initOverridenRequestMappings()
	{
		final ApplicationContext applicationContext = getApplicationContext();
		Objects.requireNonNull(applicationContext, "Application context cannot be null");

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Looking for overridden request mappings in application context: {}", applicationContext);
		}

		final String[] beanNames = this.detectInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(applicationContext, Object.class) :
				applicationContext.getBeanNamesForType(Object.class);

		for (final String beanName : beanNames)
		{
			final Class<?> beanType = applicationContext.getType(beanName);
			if (beanType != null && isHandler(beanType))
			{
				detectOverriddenMappings(beanName);
			}
		}
	}


	/**
	 * Looks for overridden request mapping.
	 *
	 * @param handler the bean name of a handler or a handler instance
	 */
	protected void detectOverriddenMappings(final Object handler)
	{
		final ApplicationContext applicationContext = getApplicationContext();
		Objects.requireNonNull(applicationContext, "Application context cannot be null");

		final Class<?> handlerType = (handler instanceof String) ?
				applicationContext.getType((String) handler) :
				handler.getClass();

		if (handlerType != null)
		{
			final Class<?> userType = ClassUtils.getUserClass(handlerType);

			final Set<Method> methods = MethodIntrospector.selectMethods(userType, new MethodFilter()
			{
				@Override
				public boolean matches(final Method method)
				{
					return hasRequestMappingOverrideAnnotation(method);
				}
			});

			for (final Method method : methods)
			{
				final RequestMappingInfo mapping = super.getMappingForMethod(method, userType);
				if (mapping != null)
				{
					registerOverriddenMapping(method, mapping);
				}
			}
		}
	}

	/**
	 * Adds overridden request mapping to map
	 */
	protected void registerOverriddenMapping(final Method method, final RequestMappingInfo mapping)
	{
		final Integer newPriority = getMethodPriorityValue(method);
		final Integer maxPriority = overriddenRequestMapping.get(mapping);
		if (newPriority != null)
		{
			if (maxPriority != null)
			{
				if (maxPriority.intValue() < newPriority.intValue())
				{
					this.overriddenRequestMapping.put(mapping, newPriority);
				}
			}
			else
			{
				this.overriddenRequestMapping.put(mapping, newPriority);
			}

			if (LOG.isInfoEnabled())
			{
				LOG.info("Mapping \"{}\" overridden with priority = {}", mapping, newPriority);
			}
		}
	}

	@Override
	public void setDetectHandlerMethodsInAncestorContexts(final boolean detectHandlerMethodsInAncestorContexts)
	{
		super.setDetectHandlerMethodsInAncestorContexts(detectHandlerMethodsInAncestorContexts);
		this.detectInAncestorContexts = detectHandlerMethodsInAncestorContexts;
	}

	public Map<RequestMappingInfo, Integer> getOverriddenRequestMapping()
	{
		return Collections.unmodifiableMap(this.overriddenRequestMapping);
	}

	public int getDefaultRequestMappingOverridePriority()
	{
		return defaultRequestMappingOverridePriority;
	}

	public void setDefaultRequestMappingOverridePriority(final int defaultRequestMappingOverridePriority)
	{
		this.defaultRequestMappingOverridePriority = defaultRequestMappingOverridePriority;
	}
}
