package org.apache.maven.archiva.web.xmlrpc.server;

import java.util.Map;
import java.util.HashMap;

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
import java.util.List;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;

public class ArchivaRequestProcessorFactoryFactory implements RequestProcessorFactoryFactory
{
    private final Map<Class, ArchivaRequestProcessorFactory> services;

    public ArchivaRequestProcessorFactoryFactory(List serviceList)
    {
        services = new HashMap<Class, ArchivaRequestProcessorFactory>();
        for (Object service : serviceList)
        {
            services.put(service.getClass(), new ArchivaRequestProcessorFactory(service.getClass(), service));
        }
    }

    public RequestProcessorFactory getRequestProcessorFactory(Class pClass)
        throws XmlRpcException
    {
        ArchivaRequestProcessorFactory processorFactory = services.get(pClass);
        if (processorFactory == null)
        {
            throw new XmlRpcException("Could not find service object instance for type " + pClass.getName());
        }
        return processorFactory;
    }
    
    private class ArchivaRequestProcessorFactory implements RequestProcessorFactory
    {
        private final Class pType;

        private final Object serviceObject;

        public ArchivaRequestProcessorFactory(Class pType, Object serviceObject)
        {
            this.pType = pType;
            this.serviceObject = serviceObject;
        }

        public Object getRequestProcessor(XmlRpcRequest request)
            throws XmlRpcException
        {
            return serviceObject;
        }
    }
}
