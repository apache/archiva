package org.apache.archiva.consumers.core.repository;

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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Purge the repository by retention count. Retain only the specified number of snapshots.
 */
public class RetentionCountRepositoryPurge
    extends AbstractRepositoryPurge
{
    private int retentionCount;

    public RetentionCountRepositoryPurge( ManagedRepositoryContent repository, int retentionCount,
                                          RepositorySession repositorySession, List<RepositoryListener> listeners )
    {
        super( repository, repositorySession, listeners );
        this.retentionCount = retentionCount;
    }

    @Override
    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            ContentItem item = repository.toItem( path );
            if (item instanceof Artifact )
            {
                Artifact artifact = (Artifact) item;
                if (!artifact.exists()) {
                    return;
                }

                if ( VersionUtil.isSnapshot( artifact.getVersion( ).getVersion() ) )
                {
                    ArchivaItemSelector selector = ArchivaItemSelector.builder( )
                        .withNamespace( artifact.getVersion( ).getProject( ).getNamespace( ).getNamespace( ) )
                        .withProjectId( artifact.getVersion( ).getProject( ).getId( ) )
                        .withArtifactId( artifact.getId( ) )
                        .withVersion( artifact.getVersion( ).getVersion( ) )
                        .withClassifier( "*" )
                        .includeRelatedArtifacts()
                        .build( );


                    List<String> versions;
                    try( Stream<? extends Artifact> stream = repository.newArtifactStream( selector) ){
                        versions = stream.map( a -> a.getArtifactVersion( ) )
                            .filter( StringUtils::isNotEmpty )
                            .distinct()
                            .collect( Collectors.toList( ) );
                    }

                    Collections.sort( versions, VersionComparator.getInstance( ) );

                    if ( retentionCount > versions.size( ) )
                    {
                        log.trace( "No deletion, because retention count is higher than actual number of artifacts." );
                        // Done. nothing to do here. skip it.
                        return;
                    }

                    ArchivaItemSelector.Builder selectorBuilder = ArchivaItemSelector.builder( )
                        .withNamespace( artifact.getVersion( ).getProject( ).getNamespace( ).getNamespace( ) )
                        .withProjectId( artifact.getVersion( ).getProject( ).getId( ) )
                        .withArtifactId( artifact.getId( ) )
                        .withClassifier( "*" )
                        .includeRelatedArtifacts()
                        .withVersion( artifact.getVersion( ).getVersion( ) );
                    int countToPurge = versions.size( ) - retentionCount;
                    Set<Artifact> artifactsToDelete = new HashSet<>( );
                    for ( String version : versions )
                    {
                        if ( countToPurge-- <= 0 )
                        {
                            break;
                        }
                        List<? extends Artifact> delArtifacts = repository.getArtifacts( selectorBuilder.withArtifactVersion( version ).build( ) );
                        if (delArtifacts!=null && delArtifacts.size()>0)
                        {
                            artifactsToDelete.addAll( delArtifacts );
                        }
                    }
                    purge( artifactsToDelete );
                }
            } else {
                throw new RepositoryPurgeException( "Bad artifact path " + path );
            }
        }
        catch ( LayoutException le )
        {
            throw new RepositoryPurgeException( le.getMessage( ), le );
        }
        catch ( org.apache.archiva.repository.ContentAccessException e )
        {
            log.error( "Error while accessing the repository data: {}", e.getMessage( ), e );
            throw new RepositoryPurgeException( e.getMessage( ), e );
        }
    }

    /*
     * Returns a new artifact reference with different version
     */
    private ArtifactReference getNewArtifactReference( ArtifactReference reference, String version )
        throws LayoutException
    {
        ArtifactReference artifact = new ArtifactReference( );
        artifact.setGroupId( reference.getGroupId( ) );
        artifact.setArtifactId( reference.getArtifactId( ) );
        artifact.setVersion( version );
        artifact.setClassifier( reference.getClassifier( ) );
        artifact.setType( reference.getType( ) );
        return artifact;

    }
}
