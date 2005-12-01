package org.apache.maven.repository.reporting;

/* 
 * Copyright 2001-2005 The Apache Software Foundation. 
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

import java.io.File;
import java.util.Iterator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.repository.RepositoryFileFilter;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;

/**
 * This class will report on bad metadata files.  These include invalid version declarations and incomplete version
 * information inside the metadata file.  Plugin metadata will be checked for validity of the latest plugin artifacts.
 *
 */
public class BadMetadataReporter implements MetadataReportProcessor
{
    private WagonManager wagon;
    private ArtifactFactory artifactFactory;
    
    public void processMetadata( RepositoryMetadata metadata, ArtifactRepository repository, ArtifactReporter reporter )
    {
        boolean hasFailures = false;

        String lastUpdated = metadata.getMetadata().getVersioning().getLastUpdated();
        if ( lastUpdated == null || lastUpdated.length() == 0 )
        {
            reporter.addFailure( metadata, "Missing lastUpdated element inside the metadata." );
            hasFailures = true;
        }
        
        if ( metadata.storedInGroupDirectory() )
        {
            checkPluginMetadata( metadata, repository, reporter );
        }
        else if ( metadata.storedInArtifactVersionDirectory() )
        {
            //snapshot metadata
        }
        else
        {
            if ( !checkMetadataVersions( metadata, repository, reporter ) ) hasFailures = true;

            if ( checkRepositoryVersions( metadata, repository, reporter ) ) hasFailures = true;
        }

        if ( !hasFailures ) reporter.addSuccess( metadata );
    }
    
    /**
     * Checks the plugin metadata
     */
    public boolean checkPluginMetadata( RepositoryMetadata metadata, ArtifactRepository repository, 
                                       ArtifactReporter reporter )
    {
       boolean hasFailures = false;
       
       
       
       return hasFailures;
    }
    
    /**
     * Checks the snapshot metadata
     */
    public boolean checkSnapshotMetadata( RepositoryMetadata metadata, ArtifactRepository repository, 
                                       ArtifactReporter reporter )
    {
       boolean hasFailures = false;
       
       Snapshot snapshot = metadata.getMetadata().getVersioning().getSnapshot();
       String timestamp = snapshot.getTimestamp();
       String buildNumber = String.valueOf( snapshot.getBuildNumber() );
       String artifactName = metadata.getArtifactId() + "-" + timestamp + "-" + buildNumber + ".pom";
       
       //@todo use wagon instead
       Artifact artifact = createArtifact( metadata );
       File artifactFile = new File ( repository.pathOf( artifact ) );
       File snapshotFile = new File( artifactFile.getParentFile(), artifactName );
       if ( !snapshotFile.exists() )
       {
           reporter.addFailure( metadata, "Snapshot artifact " + artifactName + " does not exist." );
           hasFailures = true;
       }
       
       return hasFailures;
    }
    
    /**
     * Checks the declared metadata versions if the artifacts are present in the repository
     */
    public boolean checkMetadataVersions( RepositoryMetadata metadata, ArtifactRepository repository, 
                                       ArtifactReporter reporter )
    {
        boolean hasFailures = false;
        Versioning versioning = metadata.getMetadata().getVersioning();
        for ( Iterator versions = versioning.getVersions().iterator(); versions.hasNext(); )
        {
            String version = (String) versions.next();
            
            Artifact artifact = createArtifact( metadata, version );

            try
            {
                wagon.getArtifact( artifact, repository );
            }
            catch ( TransferFailedException e )
            {
                reporter.addWarning( artifact, "An error occurred during the transfer of the artifact in " +
                                     "the repository." );
            }
            catch ( ResourceDoesNotExistException e )
            {
                //do nothing, will check later that this artifact has not been resolved
            }

            if ( !artifact.isResolved() )
            {
                reporter.addFailure( metadata, "Artifact version " + version + " is present in metadata but " +
                                     "missing in the repository." );
                if ( !hasFailures ) hasFailures = true;
            }
        }
        return hasFailures;
    }
    
    /**
     * Searches the artifact repository directory for all versions and verifies that all of them are listed in the 
     * metadata file.
     */
    public boolean checkRepositoryVersions( RepositoryMetadata metadata, ArtifactRepository repository, 
                                       ArtifactReporter reporter )
    {
        boolean hasFailures = false;
        Versioning versioning = metadata.getMetadata().getVersioning();
        String repositoryPath = repository.getBasedir();
        File versionsDir = new File( repositoryPath, formatAsDirectory( metadata.getGroupId() ) + 
                                     File.pathSeparator + metadata.getArtifactId() );
        File[] versions = versionsDir.listFiles( new RepositoryFileFilter() );
        for( int idx=0; idx<versions.length; idx++ )
        {
            String version = versions[ idx ].getName();
            if ( !versioning.getVersions().contains( version ) )
            {
                reporter.addFailure( metadata, "Artifact version " + version + " found in the repository but " +
                                     "missing in the metadata." );
                if ( !hasFailures ) hasFailures = true;
            }
        }
        return hasFailures;
    }
    
    /**
     * Formats an artifact groupId to the directory structure format used for storage in repositories
     */
    private String formatAsDirectory( String directory )
    {
        return directory.replace( '.', File.pathSeparatorChar );
    }
    
    private Artifact createArtifact( RepositoryMetadata metadata )
    {
        return artifactFactory.createBuildArtifact( metadata.getGroupId(), metadata.getArtifactId(), 
                                                    metadata.getBaseVersion(), "pom" );
    }

    private Artifact createArtifact( RepositoryMetadata metadata, String version )
    {
        return artifactFactory.createBuildArtifact( metadata.getGroupId(), metadata.getArtifactId(), 
                                                    version, "pom" );
    }
}
