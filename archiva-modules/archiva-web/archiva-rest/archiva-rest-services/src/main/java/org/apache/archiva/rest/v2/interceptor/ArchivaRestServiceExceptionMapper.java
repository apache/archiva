package org.apache.archiva.rest.v2.interceptor;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.rest.api.v2.svc.ArchivaRestError;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestServiceException;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps exceptions to REST responses.
 *
 * @author Martin Stockhammer
 * @since 3.0
 */
@Provider
@Service( "v2.archivaRestServiceExceptionMapper" )
public class ArchivaRestServiceExceptionMapper
    implements ExceptionMapper<ArchivaRestServiceException>
{
    @Override
    public Response toResponse( final ArchivaRestServiceException e )
    {
        ArchivaRestError restError = new ArchivaRestError( e );

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
                    return Response.Status.Family.familyOf( e.getHttpErrorCode( ) );
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
