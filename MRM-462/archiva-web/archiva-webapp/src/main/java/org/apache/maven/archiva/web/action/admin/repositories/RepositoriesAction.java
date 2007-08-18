package org.apache.maven.archiva.web.action.admin.repositories;

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

import com.opensymphony.webwork.interceptor.ServletRequestAware;
import com.opensymphony.xwork.Preparable;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.list.TransformedList;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.functors.RepositoryConfigurationComparator;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.util.ContextUtils;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * Shows the Repositories Tab for the administrator.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="repositoriesAction"
 */
public class RepositoriesAction
    extends PlexusActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{
    /**
     * @plexus.requirement role-hint="adminrepoconfig"
     */
    private Transformer repoConfigToAdmin;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private List managedRepositories;

    private List remoteRepositories;

    private String baseUrl;

    public void setServletRequest( HttpServletRequest request )
    {
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
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();

        remoteRepositories = TransformedList.decorate( config.getRemoteRepositories(), repoConfigToAdmin );
        managedRepositories = TransformedList.decorate( config.getManagedRepositories(), repoConfigToAdmin );

        Collections.sort( managedRepositories, new RepositoryConfigurationComparator() );
        Collections.sort( remoteRepositories, new RepositoryConfigurationComparator() );
    }

    public List getManagedRepositories()
    {
        return managedRepositories;
    }

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }
}
