package org.apache.archiva.web.api;
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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.web.model.JavascriptLog;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Path( "/javascriptLogger/" )
public interface JavascriptLogger
{

    @PUT
    @Path( "trace" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true, noPermission = true)
    Boolean trace( JavascriptLog javascriptLog );

    @PUT
    @Path( "debug" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true, noPermission = true)
    Boolean debug( JavascriptLog javascriptLog );

    @PUT
    @Path( "info" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true, noPermission = true)
    Boolean info( JavascriptLog javascriptLog );

    @PUT
    @Path( "warn" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true, noPermission = true)
    Boolean warn( JavascriptLog javascriptLog );

    @PUT
    @Path( "error" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true, noPermission = true)
    Boolean error( JavascriptLog javascriptLog );

}
