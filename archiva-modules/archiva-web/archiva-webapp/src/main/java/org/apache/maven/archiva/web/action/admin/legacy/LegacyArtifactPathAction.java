package org.apache.maven.archiva.web.action.admin.legacy;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.LegacyArtifactPath;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.web.util.ContextUtils;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the LegacyArtifactPath Tab for the administrator.
 *
 * @since 1.1
 */
@Controller( "legacyArtifactPathAction" )
@Scope( "prototype" )
public class LegacyArtifactPathAction
    extends AbstractActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{

    @Inject
    private ArchivaAdministration archivaAdministration;

    private List<LegacyArtifactPath> legacyArtifactPaths;

    /**
     * Used to construct the repository WebDAV URL in the repository action.
     */
    private String baseUrl;

    public void setServletRequest( HttpServletRequest request )
    {
        // TODO: is there a better way to do this?
        this.baseUrl = ContextUtils.getBaseURL( request, "repository" );
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public void prepare()
        throws RepositoryAdminException
    {
        legacyArtifactPaths = new ArrayList<LegacyArtifactPath>( getArchivaAdministration().getLegacyArtifactPaths() );
    }

    public List<LegacyArtifactPath> getLegacyArtifactPaths()
    {
        return legacyArtifactPaths;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public ArchivaAdministration getArchivaAdministration()
    {
        return archivaAdministration;
    }

    public void setArchivaAdministration( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }
}
