package org.codehaus.redback.integration.filter;

/*
 * Copyright 2005-2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * SpringServletFilter
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class SpringServletFilter
    implements Filter
{
    private ApplicationContext applicationContext;

    public void destroy()
    {
        // Do nothing here.
    }

    protected ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        applicationContext = WebApplicationContextUtils.getWebApplicationContext( filterConfig.getServletContext() );

    }
}
