package org.apache.archiva.web.interceptor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.struts2.ServletActionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.ServletContext;

/**
 * An interceptor that makes the configuration bits available, both to the application and the webapp.
 */
@Service( "configurationInterceptor" )
@Scope( "prototype" )
public class ConfigurationInterceptor
    implements Interceptor
{

    @Inject
    private ArchivaConfiguration configuration;

    public String intercept( ActionInvocation actionInvocation )
        throws Exception
    {
        // populate webapp configuration bits into the session
        ServletContext applicationScope = ServletActionContext.getServletContext();
        applicationScope.setAttribute( "uiOptions", configuration.getConfiguration().getWebapp().getUi() );

        return actionInvocation.invoke();
    }

    public void destroy()
    {
        // This space left intentionally blank
    }

    public void init()
    {
        // This space left intentionally blank
    }

}
