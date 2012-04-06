package org.codehaus.redback.rest.services.interceptors;

/*
* Copyright 2011 The Codehaus.
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

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.codehaus.plexus.redback.authorization.RedbackAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import java.lang.reflect.Method;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
public class AbstractInterceptor
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    public HttpServletRequest getHttpServletRequest( Message message )
    {
        return httpServletRequest;
    }

    public HttpServletResponse getHttpServletResponse( Message message )
    {
        return httpServletResponse;
    }

    public RedbackAuthorization getRedbackAuthorization( Message message )
    {
        OperationResourceInfo operationResourceInfo = message.getExchange().get( OperationResourceInfo.class );
        if ( operationResourceInfo == null )
        {
            return null;
        }

        Method method = operationResourceInfo.getAnnotatedMethod();

        log.debug( " method name {}", method == null ? "null" : method.getName() );
        RedbackAuthorization redbackAuthorization = method.getAnnotation( RedbackAuthorization.class );
        return redbackAuthorization;
    }
}
