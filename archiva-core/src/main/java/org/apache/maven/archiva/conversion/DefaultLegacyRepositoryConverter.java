package org.apache.maven.archiva.conversion;

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

import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.converter.RepositoryConverter;
import org.apache.maven.archiva.discoverer.ArtifactDiscoverer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.filter.AcceptAllArtifactFilter;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.store.ReportingStore;
import org.apache.maven.archiva.reporting.store.ReportingStoreException;
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
 * @todo turn this into a general conversion component and hide all this crap here.
 * @todo it should be possible to move this to the converter module without causing it to gain additional dependencies
 */
public class DefaultLegacyRepositoryConverter
    implements LegacyRepositoryConverter
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
                                         List blacklistedPatterns, boolean includeSnapshots )
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
        List legacyArtifacts = artifactDiscoverer.discoverArtifacts( legacyRepository, blacklistedPatterns, filter );

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
