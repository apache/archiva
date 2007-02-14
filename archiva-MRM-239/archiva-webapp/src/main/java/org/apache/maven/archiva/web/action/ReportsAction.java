package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork.Preparable;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.filter.AcceptAllArtifactFilter;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.archiva.reporting.executor.ReportExecutor;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.store.ReportingStoreException;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Repository reporting.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="reportsAction"
 * @todo split report access and report generation
 */
public class ReportsAction
    extends PlexusActionSupport
    implements Preparable, SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory factory;

    private Configuration configuration;

    public String execute()
        throws Exception
    {
        databases = new ArrayList();

        if ( repositoryId != null && !repositoryId.equals( "-" ) )
        {
            RepositoryConfiguration repositoryConfiguration = configuration.getRepositoryById( repositoryId );
            getReport( repositoryConfiguration, reportGroup );
        }
        else
        {
            for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
            {
                RepositoryConfiguration repositoryConfiguration = (RepositoryConfiguration) i.next();

                getReport( repositoryConfiguration, reportGroup );
            }
        }
        return SUCCESS;
    }

    public String runReport()
        throws Exception
    {
        ReportGroup reportGroup = (ReportGroup) reports.get( this.reportGroup );

        RepositoryConfiguration repositoryConfiguration = configuration.getRepositoryById( repositoryId );
        ArtifactRepository repository = factory.createRepository( repositoryConfiguration );

        ReportingDatabase database = executor.getReportDatabase( repository, reportGroup );
        if ( database.isInProgress() )
        {
            return SUCCESS;
        }

        generateReport( database, repositoryConfiguration, reportGroup, repository );

        return SUCCESS;
    }

    public void prepare()
        throws Exception
    {
        configuration = archivaConfiguration.getConfiguration();
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public Map getReports()
    {
        return reports;
    }

    public String getFilter()
    {
        return filter;
    }

    public void setFilter( String filter )
    {
        this.filter = filter;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_ACCESS_REPORT, Resource.GLOBAL );

        return bundle;
    }
}
