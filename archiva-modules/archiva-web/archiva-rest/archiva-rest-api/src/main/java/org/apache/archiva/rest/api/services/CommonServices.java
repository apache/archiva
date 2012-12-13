package org.apache.archiva.rest.api.services;
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

import org.apache.archiva.redback.authorization.RedbackAuthorization;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * contains some "free" services (i18n)
 *
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Path( "/commonServices/" )
public interface CommonServices
{

    /**
     * will return properties available in org/apache/archiva/i18n/default.properties
     * load default (en) then override with locale used so at least en are returned if no
     * translation in the locale asked.
     */
    @Path( "getI18nResources" )
    @GET
    @Produces( { MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    String getI18nResources( @QueryParam( "locale" ) String locale )
        throws ArchivaRestServiceException;

    /**
     * will return properties available in org/apache/archiva/i18n/default.properties
     * load default (en) then override with locale used so at least en are returned if no
     * translation in the locale asked.
     * This method will add redback resources too. note Archva wins
     */
    @Path( "getAllI18nResources" )
    @GET
    @Produces( { MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    String getAllI18nResources( @QueryParam( "locale" ) String locale )
        throws ArchivaRestServiceException;


    @Path( "validateCronExpression" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true )
    Boolean validateCronExpression( @QueryParam( "cronExpression" ) String cronExpression )
        throws ArchivaRestServiceException;

}
