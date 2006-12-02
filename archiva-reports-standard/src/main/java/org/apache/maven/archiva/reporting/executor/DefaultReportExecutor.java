package org.apache.maven.archiva.reporting.executor;

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
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.executor.ReportExecutor;
import org.apache.maven.archiva.reporting.store.ReportingStore;
import org.apache.maven.archiva.reporting.store.ReportingStoreException;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
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
 * @todo should the report set be limitable by configuration?
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
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.ArtifactDiscoverer"
     */
    private Map artifactDiscoverers;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.discoverer.MetadataDiscoverer"
     */
    private Map metadataDiscoverers;

    private static final int ARTIFACT_BUFFER_SIZE = 1000;

    public void runMetadataReports( ReportGroup reportGroup, List metadata, ArtifactRepository repository )
        throws ReportingStoreException
    {
        ReportingDatabase reporter = getReportDatabase( repository, reportGroup );

        for ( Iterator i = metadata.iterator(); i.hasNext(); )
        {
            RepositoryMetadata repositoryMetadata = (RepositoryMetadata) i.next();

            File file =
                new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repositoryMetadata ) );
            reporter.cleanMetadata( repositoryMetadata, file.lastModified() );

            reportGroup.processMetadata( repositoryMetadata, repository, reporter );
        }

        reportingStore.storeReports( reporter, repository );
    }

    public void runArtifactReports( ReportGroup reportGroup, List artifacts, ArtifactRepository repository )
        throws ReportingStoreException
    {
        ReportingDatabase reporter = getReportDatabase( repository, reportGroup );

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
            catch ( InvalidArtifactRTException e )
            {
                reporter.addWarning( artifact, null, null, "Invalid artifact [" + artifact + "] : " + e );
            }
            catch ( ProjectBuildingException e )
            {
                reporter.addWarning( artifact, null, null, "Error reading project model: " + e );
            }

            reporter.removeArtifact( artifact );

            reportGroup.processArtifact( artifact, model, reporter );
        }

        reportingStore.storeReports( reporter, repository );
    }

    public ReportingDatabase getReportDatabase( ArtifactRepository repository, ReportGroup reportGroup )
        throws ReportingStoreException
    {
        getLogger().debug(
            "Reading previous report database " + reportGroup.getName() + " from repository " + repository.getId() );
        return reportingStore.getReportsFromStore( repository, reportGroup );
    }

    public void runReports( ReportGroup reportGroup, ArtifactRepository repository, List blacklistedPatterns,
                            ArtifactFilter filter )
        throws DiscovererException, ReportingStoreException
    {
        // Flush (as in toilet, not store) the report database
        ReportingDatabase database = getReportDatabase( repository, reportGroup );
        database.clear();

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

            // Work through these in batches, then flush the project cache.
            for ( int j = 0; j < artifacts.size(); j += ARTIFACT_BUFFER_SIZE )
            {
                int end = j + ARTIFACT_BUFFER_SIZE;
                List currentArtifacts = artifacts.subList( j, end > artifacts.size() ? artifacts.size() : end );

                // TODO: proper queueing of this in case it was triggered externally (not harmful to do so at present, but not optimal)

                // run the reports.
                runArtifactReports( reportGroup, currentArtifacts, repository );

                // MNG-142 - the project builder retains a lot of objects in its inflexible cache. This is a hack
                // around that. TODO: remove when it is configurable
                flushProjectBuilderCacheHack();
            }
        }

        MetadataDiscoverer metadataDiscoverer = (MetadataDiscoverer) metadataDiscoverers.get( layoutProperty );
        List metadata =
            metadataDiscoverer.discoverMetadata( repository, blacklistedPatterns, new AcceptAllMetadataFilter() );

        if ( !metadata.isEmpty() )
        {
            getLogger().info( "Discovered " + metadata.size() + " metadata files" );

            // run the reports
            runMetadataReports( reportGroup, metadata, repository );
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

    private void flushProjectBuilderCacheHack()
    {
        try
        {
            if ( projectBuilder != null )
            {
                java.lang.reflect.Field f = projectBuilder.getClass().getDeclaredField( "rawProjectCache" );
                f.setAccessible( true );
                Map cache = (Map) f.get( projectBuilder );
                cache.clear();

                f = projectBuilder.getClass().getDeclaredField( "processedProjectCache" );
                f.setAccessible( true );
                cache = (Map) f.get( projectBuilder );
                cache.clear();
            }
        }
        catch ( NoSuchFieldException e )
        {
            throw new RuntimeException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
    }
}
