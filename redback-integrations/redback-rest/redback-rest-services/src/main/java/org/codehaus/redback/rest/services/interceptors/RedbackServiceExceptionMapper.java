package org.codehaus.redback.rest.services.interceptors;
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

import org.codehaus.redback.rest.api.model.ErrorMessage;
import org.codehaus.redback.rest.api.model.RedbackRestError;
import org.codehaus.redback.rest.api.services.RedbackServiceException;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Provider
@Service( "redbackServiceExceptionMapper" )
public class RedbackServiceExceptionMapper
    implements ExceptionMapper<RedbackServiceException>
{
    public Response toResponse( final RedbackServiceException e )
    {
        RedbackRestError restError = new RedbackRestError( e );

        Response.ResponseBuilder responseBuilder = Response.status( e.getHttpErrorCode() ).entity( restError );
        if ( e.getMessage() != null )
        {
            responseBuilder = responseBuilder.status( new Response.StatusType()
            {
                public int getStatusCode()
                {
                    return e.getHttpErrorCode();
                }

                public Response.Status.Family getFamily()
                {
                    return Response.Status.Family.SERVER_ERROR;
                }

                public String getReasonPhrase()
                {
                    return e.getMessage();
                }
            } );
        }
        return responseBuilder.build();
    }
}
