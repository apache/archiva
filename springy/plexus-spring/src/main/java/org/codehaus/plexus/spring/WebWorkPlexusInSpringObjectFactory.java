package org.codehaus.plexus.spring;

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

import java.util.Map;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.NestedRuntimeException;

import com.opensymphony.webwork.spring.WebWorkSpringObjectFactory;
import com.opensymphony.xwork.Action;
import com.opensymphony.xwork.Result;
import com.opensymphony.xwork.interceptor.Interceptor;

/**
 * Replacement for WebWorkSpringObjectFactory ("webwork.objectFactory = spring") to support
 * plexus components lookup as expected by plexus-xwork integration.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class WebWorkPlexusInSpringObjectFactory
    extends WebWorkSpringObjectFactory
{

    /**
     * {@inheritDoc}
     *
     * @see com.opensymphony.xwork.spring.SpringObjectFactory#buildBean(java.lang.String,
     * java.util.Map)
     */
    public Object buildBean( String name, Map map )
        throws Exception
    {
        String id = PlexusToSpringUtils.buildSpringId( Action.class, name );
        if ( appContext.containsBean( id ) )
        {
            return super.buildBean( id, map );
        }

        id = PlexusToSpringUtils.buildSpringId( Result.class, name );
        if ( appContext.containsBean( id ) )
        {
            return super.buildBean( id, map );
        }

        id = PlexusToSpringUtils.buildSpringId( Interceptor.class, name );
        if ( appContext.containsBean( id ) )
        {
            return super.buildBean( id, map );
        }
        return super.buildBean( name, map );
    }
}
