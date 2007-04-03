package org.apache.maven.archiva.consumers.lucene;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.record.RepositoryIndexRecordFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;

/**
 * IndexArtifactConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 *
 * @plexus.component role="org.apache.maven.archiva.common.consumers.Consumer"
 *     role-hint="index-artifact"
 *     instantiation-strategy="per-lookup"
 */
public class IndexArtifactConsumer
    extends GenericArtifactConsumer
{
    /**
     * @plexus.requirement
     */
    private RepositoryArtifactIndexFactory indexFactory;
    
    /**
     * @plexus.requirement role-hint="standard"
     */
    private RepositoryIndexRecordFactory recordFactory;

    /**
     * Configuration store.
     *
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private RepositoryArtifactIndex index;

    public boolean init( ArtifactRepository repository )
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        File indexPath = new File( configuration.getIndexPath() );

        index = indexFactory.createStandardIndex( indexPath );

        return super.init( repository );
    }

    public void processArtifact( Artifact artifact, BaseFile file )
    {
        try
        {
            index.indexArtifact( artifact, recordFactory );
        }
        catch ( RepositoryIndexException e )
        {
            getLogger().warn( "Unable to index artifact " + artifact, e );
        }
    }

    public void processFileProblem( BaseFile path, String message )
    {

    }
    
    public String getName()
    {
        return "Index Artifact Consumer";
    }
}
