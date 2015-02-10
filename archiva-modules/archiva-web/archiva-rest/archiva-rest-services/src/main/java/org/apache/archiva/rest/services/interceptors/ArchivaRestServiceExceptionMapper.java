package org.apache.archiva.rest.services.interceptors;
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

import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.services.ArchivaRestError;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Provider
@Service( "archivaRestServiceExceptionMapper" )
public class ArchivaRestServiceExceptionMapper
    implements ExceptionMapper<ArchivaRestServiceException>
{
    @Override
    public Response toResponse( final ArchivaRestServiceException e )
    {
        ArchivaRestError restError = new ArchivaRestError( e );
        Response response = //
            Response.status( new Response.StatusType()
            {
                @Override
                public int getStatusCode()
                {
                    return e.getHttpErrorCode();
                }

                @Override
                public Response.Status.Family getFamily()
                {
                    return Response.Status.Family.familyOf( e.getHttpErrorCode() );
                }

                @Override
                public String getReasonPhrase()
                {
                    return e.getMessage();
                }
            } )//
                .entity( restError ) //
                .build();

        return response;
    }
}
