/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.filter;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * Util class for retrieving spring beans.
 */
public class FilterSpringUtil
{
	private static final Logger LOG = LoggerFactory.getLogger(FilterSpringUtil.class);

	private FilterSpringUtil()
	{
	}

	/**
	 * Returns the Spring bean with name <code>beanName</code> and of type <code>beanClass</code>. If no bean could be
	 * resolved for the specified name, the bean is looked up using type.
	 *
	 * @param <T>
	 * 		type of the bean
	 * @param httpRequest
	 * 		the http request
	 * @param beanName
	 * 		name of the bean or <code>null</code> if it should be automatically resolved using type
	 * @param beanClass
	 * 		expected type of the bean
	 * @return the bean matching the given arguments or <code>null</code> if no bean could be resolved
	 */
	public static <T> T getSpringBean(final HttpServletRequest httpRequest, final String beanName, final Class<T> beanClass)
	{
		final HttpSession session = httpRequest.getSession();
		final ServletContext servletContext = session.getServletContext();
		return getSpringBean(servletContext, beanName, beanClass);
	}

	/**
	 * The same as {@link #getSpringBean(HttpServletRequest, String, Class)} but uses ServletContext as the first
	 * parameter. It might be used in places, where HttpServletRequest is not available, but ServletContext is.
	 */
	public static <T> T getSpringBean(final ServletContext servletContext, final String beanName, final Class<T> beanClass)
	{
		final WebApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

		if (StringUtils.isNotBlank(beanName))
		{
			try
			{
				return (T) appContext.getBean(beanName);
			}
			catch (final NoSuchBeanDefinitionException nsbde)
			{
				LOG.warn("No bean found with name {}. Trying to resolve bean using type...", beanName);
				LOG.debug("Exception thrown:", nsbde);
			}
		}

		if (beanClass == null)
		{
			LOG.warn("No bean could be resolved. Reason: No type specified.");
			return null;
		}

		final Map<String, T> beansOfType = appContext.getBeansOfType(beanClass);

		if (beansOfType.size() > 1)
		{
			LOG.warn("More than one matching bean found of type {}. Returning the first one found.",
					beanClass.getSimpleName());
		}

		return beansOfType.isEmpty() ? null : beansOfType.values().iterator().next();
	}
}
