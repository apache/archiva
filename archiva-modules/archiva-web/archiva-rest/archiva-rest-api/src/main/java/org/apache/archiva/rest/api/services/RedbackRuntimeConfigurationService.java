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

import org.apache.archiva.admin.model.beans.LdapConfiguration;
import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.model.RBACManagerImplementationInformation;
import org.apache.archiva.rest.api.model.RedbackImplementationsInformations;
import org.apache.archiva.rest.api.model.UserManagerImplementationInformation;
import org.apache.archiva.security.common.ArchivaRoleConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Path("/redbackRuntimeConfigurationService/")
public interface RedbackRuntimeConfigurationService
{
    @Path("redbackRuntimeConfiguration")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    RedbackRuntimeConfiguration getRedbackRuntimeConfiguration()
        throws ArchivaRestServiceException;

    @Path("redbackRuntimeConfiguration")
    @PUT
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    Boolean updateRedbackRuntimeConfiguration( RedbackRuntimeConfiguration redbackRuntimeConfiguration )
        throws ArchivaRestServiceException;

    @Path("userManagerImplementationInformations")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    List<UserManagerImplementationInformation> getUserManagerImplementationInformations()
        throws ArchivaRestServiceException;

    @Path("rbacManagerImplementationInformations")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    List<RBACManagerImplementationInformation> getRbacManagerImplementationInformations()
        throws ArchivaRestServiceException;


    @Path("redbackImplementationsInformations")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    RedbackImplementationsInformations getRedbackImplementationsInformations()
        throws ArchivaRestServiceException;

    @Path( "checkLdapConnection" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean checkLdapConnection()
        throws ArchivaRestServiceException;

    @Path("checkLdapConnection")
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    Boolean checkLdapConnection( LdapConfiguration ldapConfiguration )
        throws ArchivaRestServiceException;
}
