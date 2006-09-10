package org.apache.maven.archiva.web.action;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.ActionSupport;
import com.opensymphony.xwork.Preparable;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.discoverer.filter.AcceptAllArtifactFilter;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.archiva.reporting.ReportExecutor;
import org.apache.maven.archiva.reporting.ReportGroup;
import org.apache.maven.archiva.reporting.ReportingDatabase;
import org.apache.maven.archiva.reporting.ReportingStore;
import org.apache.maven.archiva.reporting.ReportingStoreException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Repository reporting.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="reportsAction"
 */
public class ReportsAction
    extends ActionSupport
    implements Preparable
{
    /**
     * @plexus.requirement
     */
    private ReportingStore reportingStore;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory factory;

    private List databases;

    private String repositoryId;

    /**
     * @plexus.requirement
     */
    private ReportExecutor executor;

    private Configuration configuration;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.reporting.ReportGroup"
     */
    private Map reports;

    private String reportGroup = DEFAULT_REPORT_GROUP;

    private static final String DEFAULT_REPORT_GROUP = "health";

    public String execute()
        throws Exception
    {
        ReportGroup reportGroup = (ReportGroup) reports.get( this.reportGroup );

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

    private void getReport( RepositoryConfiguration repositoryConfiguration, ReportGroup reportGroup )
        throws ReportingStoreException
    {
        ArtifactRepository repository = factory.createRepository( repositoryConfiguration );

        ReportingDatabase database = reportingStore.getReportsFromStore( repository, reportGroup );

        databases.add( database );
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

        database.setInProgress( true );

        List blacklistedPatterns = new ArrayList();
        if ( repositoryConfiguration.getBlackListPatterns() != null )
        {
            blacklistedPatterns.addAll( repositoryConfiguration.getBlackListPatterns() );
        }
        if ( configuration.getGlobalBlackListPatterns() != null )
        {
            blacklistedPatterns.addAll( configuration.getGlobalBlackListPatterns() );
        }

        ArtifactFilter filter;
        if ( repositoryConfiguration.isIncludeSnapshots() )
        {
            filter = new AcceptAllArtifactFilter();
        }
        else
        {
            filter = new SnapshotArtifactFilter();
        }

        try
        {
            executor.runReports( reportGroup, repository, blacklistedPatterns, filter );
        }
        finally
        {
            database.setInProgress( false );
        }

        return SUCCESS;
    }

    public void setReportGroup( String reportGroup )
    {
        this.reportGroup = reportGroup;
    }

    public String getReportGroup()
    {
        return reportGroup;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public List getDatabases()
    {
        return databases;
    }

    public void prepare()
        throws Exception
    {
        configuration = configurationStore.getConfigurationFromStore();
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public Map getReports()
    {
        return reports;
    }
}
