package org.apache.maven.archiva.consumers.core.repository;

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

import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Purge the repository by retention count. Retain only the specified number of snapshots.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class RetentionCountRepositoryPurge
    extends AbstractRepositoryPurge
{
    private int retentionCount;

    public RetentionCountRepositoryPurge( ManagedRepositoryContent repository, ArtifactDAO artifactDao,
                                          int retentionCount, Map<String, RepositoryContentIndex> indices )
    {
        super( repository, artifactDao, indices );
        this.retentionCount = retentionCount;
    }

    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            File artifactFile = new File( repository.getRepoRoot(), path );

            if ( !artifactFile.exists() )
            {
                return;
            }
                                                                     
            ArtifactReference artifact = repository.toArtifactReference( path );

            if ( VersionUtil.isSnapshot( artifact.getVersion() ) )
            {
                VersionedReference reference = new VersionedReference();
                reference.setGroupId( artifact.getGroupId() );
                reference.setArtifactId( artifact.getArtifactId() );
                reference.setVersion( artifact.getVersion() );

                List<String> versions = new ArrayList<String>( repository.getVersions( reference ) );

                Collections.sort( versions, VersionComparator.getInstance() );

                if ( retentionCount > versions.size() )
                {
                    // Done. nothing to do here. skip it.
                    return;
                }

                int countToPurge = versions.size() - retentionCount;

                for ( String version : versions )
                {
                    if ( countToPurge-- <= 0 )
                    {
                        break;
                    }

                    doPurgeAllRelated( artifact, version );
                }
            }
        }
        catch ( LayoutException le )
        {
            throw new RepositoryPurgeException( le.getMessage(), le );
        }
        catch ( ContentNotFoundException e )
        {
            // Nothing to do here.
            // TODO: Log this condition?
        }
    }

    private void doPurgeAllRelated( ArtifactReference reference, String version )
        throws LayoutException
    {
        ArtifactReference artifact = new ArtifactReference();
        artifact.setGroupId( reference.getGroupId() );
        artifact.setArtifactId( reference.getArtifactId() );
        artifact.setVersion( version );
        artifact.setClassifier( reference.getClassifier() );
        artifact.setType( reference.getType() );
        
        try
        {
            Set<ArtifactReference> related = repository.getRelatedArtifacts( artifact );
            purge( related );
        }
        catch ( ContentNotFoundException e )
        {
            // Nothing to do here.
            // TODO: Log this?
        }
    }
}
