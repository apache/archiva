package org.apache.maven.archiva;

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

import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.converter.RepositoryConverter;
import org.apache.maven.archiva.discoverer.ArtifactDiscoverer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.filter.AcceptAllArtifactFilter;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.archiva.reporting.ReportGroup;
import org.apache.maven.archiva.reporting.ReportingDatabase;
import org.apache.maven.archiva.reporting.ReportingStore;
import org.apache.maven.archiva.reporting.ReportingStoreException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

/**
 * @author Jason van Zyl
 * @plexus.component
 */
public class DefaultRepositoryManager
    implements RepositoryManager
{
    /**
     * @plexus.requirement role-hint="legacy"
     */
    private ArtifactDiscoverer artifactDiscoverer;

    /**
     * @plexus.requirement role-hint="legacy"
     */
    private ArtifactRepositoryLayout legacyLayout;

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArtifactRepositoryLayout defaultLayout;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryConverter repositoryConverter;

    /**
     * @plexus.requirement
     */
    private ReportingStore reportingStore;

    /**
     * @plexus.requirement role-hint="health"
     */
    private ReportGroup reportGroup;

    public void convertLegacyRepository( File legacyRepositoryDirectory, File repositoryDirectory,
                                         boolean includeSnapshots )
        throws RepositoryConversionException, DiscovererException
    {
        ArtifactRepository legacyRepository;

        ArtifactRepository repository;

        try
        {
            legacyRepository = artifactRepositoryFactory.createArtifactRepository( "legacy",
                                                                                   legacyRepositoryDirectory.toURI().toURL().toString(),
                                                                                   legacyLayout, null, null );

            repository = artifactRepositoryFactory.createArtifactRepository( "default",
                                                                             repositoryDirectory.toURI().toURL().toString(),
                                                                             defaultLayout, null, null );
        }
        catch ( MalformedURLException e )
        {
            throw new RepositoryConversionException( "Error convering legacy repository.", e );
        }

        ArtifactFilter filter =
            includeSnapshots ? new AcceptAllArtifactFilter() : (ArtifactFilter) new SnapshotArtifactFilter();
        List legacyArtifacts = artifactDiscoverer.discoverArtifacts( legacyRepository, null, filter );

        ReportingDatabase reporter;
        try
        {
            reporter = reportingStore.getReportsFromStore( repository, reportGroup );

            repositoryConverter.convert( legacyArtifacts, repository, reporter );

            reportingStore.storeReports( reporter, repository );
        }
        catch ( ReportingStoreException e )
        {
            throw new RepositoryConversionException( "Error convering legacy repository.", e );
        }
    }
}
