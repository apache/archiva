package org.apache.archiva.redback.rest.api.services;
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
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Path( "/utilServices/" )
public interface UtilServices
{

    @Path( "getBundleResources" )
    @GET
    @Produces( { MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    String getI18nResources( @QueryParam( "locale" ) String locale )
        throws RedbackServiceException;

    /**
     * <b>not intended to be exposed as a REST service.</b>
     * will load i18N resource org/apache/archiva/redback/users/messages in default en then in the asked locale.
     * @param locale
     * @return
     * @throws RedbackServiceException
     */
    Properties getI18nProperties( String locale )
        throws RedbackServiceException;


}
