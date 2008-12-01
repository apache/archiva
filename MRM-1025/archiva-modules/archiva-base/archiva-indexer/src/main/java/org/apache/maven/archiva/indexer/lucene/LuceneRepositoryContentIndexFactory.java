package org.apache.maven.archiva.indexer.lucene;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.bytecode.BytecodeHandlers;
import org.apache.maven.archiva.indexer.filecontent.FileContentHandlers;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesHandlers;

import java.io.File;

/**
 * Factory for Lucene repository content index instances.
 *
 * @plexus.component role="org.apache.maven.archiva.indexer.RepositoryContentIndexFactory" role-hint="lucene"
 */
public class LuceneRepositoryContentIndexFactory
    implements RepositoryContentIndexFactory
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    public RepositoryContentIndex createBytecodeIndex( ManagedRepositoryConfiguration repository )
    {
        File indexDir = toIndexDir( repository, "bytecode" );
        return new LuceneRepositoryContentIndex( repository, indexDir, new BytecodeHandlers() );
    }

    public RepositoryContentIndex createFileContentIndex( ManagedRepositoryConfiguration repository )
    {
        File indexDir = toIndexDir( repository, "filecontent" );
        return new LuceneRepositoryContentIndex( repository, indexDir, new FileContentHandlers() );
    }

    public RepositoryContentIndex createHashcodeIndex( ManagedRepositoryConfiguration repository )
    {
        File indexDir = toIndexDir( repository, "hashcodes" );
        return new LuceneRepositoryContentIndex( repository, indexDir, new HashcodesHandlers() );
    }

    /**
     * Obtain the index directory for the provided repository.
     *
     * @param repository the repository to obtain the index directory from.
     * @param indexId    the id of the index
     * @return the directory to put the index into.
     */
    private File toIndexDir( ManagedRepositoryConfiguration repository, String indexId )
    {
        // Attempt to get the specified indexDir in the configuration first.
        ManagedRepositoryConfiguration repoConfig =
            configuration.getConfiguration().findManagedRepositoryById( repository.getId() );
        File indexDir;

        if ( repoConfig == null )
        {
            // No configured index dir, use the repository path instead.
            String repoPath = repository.getLocation();
            indexDir = new File( repoPath, ".index/" + indexId + "/" );
        }
        else
        {
            // Use configured index dir.
            String repoPath = repoConfig.getIndexDir();
            if ( StringUtils.isBlank( repoPath ) )
            {
                repoPath = repository.getLocation();
                if ( !repoPath.endsWith( "/" ) )
                {
                    repoPath += "/";
                }
                repoPath += ".index";
            }
            indexDir = new File( repoPath, "/" + indexId + "/" );
        }

        return indexDir;
    }
}
