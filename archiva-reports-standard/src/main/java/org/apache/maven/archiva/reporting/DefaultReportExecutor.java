package org.apache.maven.archiva.reporting;

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

import org.apache.maven.archiva.discoverer.ArtifactDiscoverer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.MetadataDiscoverer;
import org.apache.maven.archiva.discoverer.filter.AcceptAllMetadataFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Report executor implementation.
 *
 * @plexus.component
 */
public class DefaultReportExecutor
    extends AbstractLogEnabled
    implements ReportExecutor
{
    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * @plexus.requirement
     */
    private ReportingStore reportingStore;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @todo replace with a ReportGroup that is identified as "health" and has requirements on the specific health reports
     * @plexus.requirement role="org.apache.maven.archiva.reporting.ArtifactReportProcessor"
     */
    private List artifactReports;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.reporting.MetadataReportProcessor"
     */
    private List metadataReports;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.ArtifactDiscoverer"
     */
    private Map artifactDiscoverers;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.MetadataDiscoverer"
     */
    private Map metadataDiscoverers;

    public void runMetadataReports( List metadata, ArtifactRepository repository )
        throws ReportingStoreException
    {
        ReportingDatabase reporter = getReportDatabase( repository );

        for ( Iterator i = metadata.iterator(); i.hasNext(); )
        {
            RepositoryMetadata repositoryMetadata = (RepositoryMetadata) i.next();

            File file =
                new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
            reporter.cleanMetadata( repositoryMetadata, file.lastModified() );

            // TODO: should the report set be limitable by configuration?
            runMetadataReports( repositoryMetadata, repository, reporter );
        }

        reportingStore.storeReports( reporter, repository );
    }

    private void runMetadataReports( RepositoryMetadata repositoryMetadata, ArtifactRepository repository,
                                     ReportingDatabase reporter )
    {
        for ( Iterator i = metadataReports.iterator(); i.hasNext(); )
        {
            MetadataReportProcessor report = (MetadataReportProcessor) i.next();

            report.processMetadata( repositoryMetadata, repository, reporter );
        }
    }

    public void runArtifactReports( List artifacts, ArtifactRepository repository )
        throws ReportingStoreException
    {
        ReportingDatabase reporter = getReportDatabase( repository );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            Model model = null;
            try
            {
                Artifact pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(),
                                                                              artifact.getArtifactId(),
                                                                              artifact.getVersion() );
                MavenProject project =
                    projectBuilder.buildFromRepository( pomArtifact, Collections.EMPTY_LIST, repository );

                model = project.getModel();
            }
            catch ( ProjectBuildingException e )
            {
                reporter.addWarning( artifact, "Error reading project model: " + e );
            }

            reporter.removeArtifact( artifact );

            runArtifactReports( artifact, model, reporter );
        }

        reportingStore.storeReports( reporter, repository );
    }

    public ReportingDatabase getReportDatabase( ArtifactRepository repository )
        throws ReportingStoreException
    {
        getLogger().debug( "Reading previous report database from repository " + repository.getId() );
        return reportingStore.getReportsFromStore( repository );
    }

    public void runReports( ArtifactRepository repository, List blacklistedPatterns, ArtifactFilter filter )
        throws DiscovererException, ReportingStoreException
    {
        // Flush (as in toilet, not store) the report database
        reportingStore.removeReportDatabase( repository );

        // Discovery process
        String layoutProperty = getRepositoryLayout( repository.getLayout() );
        ArtifactDiscoverer discoverer = (ArtifactDiscoverer) artifactDiscoverers.get( layoutProperty );

        // Save some memory by not tracking paths we won't use
        // TODO: Plexus CDC should be able to inject this configuration
        discoverer.setTrackOmittedPaths( false );

        List artifacts = discoverer.discoverArtifacts( repository, blacklistedPatterns, filter );

        if ( !artifacts.isEmpty() )
        {
            getLogger().info( "Discovered " + artifacts.size() + " artifacts" );

            // run the reports
            runArtifactReports( artifacts, repository );
        }

        MetadataDiscoverer metadataDiscoverer = (MetadataDiscoverer) metadataDiscoverers.get( layoutProperty );
        List metadata =
            metadataDiscoverer.discoverMetadata( repository, blacklistedPatterns, new AcceptAllMetadataFilter() );

        if ( !metadata.isEmpty() )
        {
            getLogger().info( "Discovered " + metadata.size() + " metadata files" );

            // run the reports
            runMetadataReports( metadata, repository );
        }
    }

    private String getRepositoryLayout( ArtifactRepositoryLayout layout )
    {
        // gross limitation that there is no reverse lookup of the hint for the layout.
        if ( layout.getClass().equals( DefaultRepositoryLayout.class ) )
        {
            return "default";
        }
        else if ( layout.getClass().equals( LegacyRepositoryLayout.class ) )
        {
            return "legacy";
        }
        else
        {
            throw new IllegalArgumentException( "Unknown layout: " + layout );
        }
    }

    private void runArtifactReports( Artifact artifact, Model model, ReportingDatabase reporter )
    {
        // TODO: should the report set be limitable by configuration?
        for ( Iterator i = artifactReports.iterator(); i.hasNext(); )
        {
            ArtifactReportProcessor report = (ArtifactReportProcessor) i.next();

            report.processArtifact( artifact, model, reporter );
        }
    }
}
