package org.apache.maven.archiva.web.xmlrpc.server;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.atlassian.xmlrpc.BinderXmlRpcServlet;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping.AuthenticationHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ArchivaXmlRpcServlet extends BinderXmlRpcServlet
{
    private final String XMLRPC_SERVICES_BEAN_NAME = "xmlrpcServices";

    private ApplicationContext context;

    @Override
    public void init(ServletConfig pConfig)
        throws ServletException
    {
        super.init(pConfig);
        
        context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        setAuthenticationHandler(new ArchivaAuthenticationHandler());
        initRequestProcessorFactoryFactory();
    }

    private void initRequestProcessorFactoryFactory() throws ServletException
    {
        List serviceList = (List)context.getBean(XMLRPC_SERVICES_BEAN_NAME);
        if (serviceList == null)
        {
            throw new ServletException("Could not find bean " +  XMLRPC_SERVICES_BEAN_NAME);
        }

        setRequestProcessorFactoryFactory(new ArchivaRequestProcessorFactoryFactory(serviceList));
    }

    /**
     * Servlet Security
     */
    private class ArchivaAuthenticationHandler implements AuthenticationHandler
    {
        public boolean isAuthorized(XmlRpcRequest request) throws XmlRpcException {
            return false;
        }
    }
}
