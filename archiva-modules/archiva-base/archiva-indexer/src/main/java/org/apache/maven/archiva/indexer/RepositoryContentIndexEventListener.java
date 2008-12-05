package org.apache.maven.archiva.indexer;

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

import org.apache.maven.archiva.indexer.bytecode.BytecodeRecord;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.events.RepositoryListener;

/**
 * Process repository management events and respond appropriately.
 * 
 * @todo creating index instances every time is inefficient, the plugin needs to have a repository context to operate in
 * @plexus.component role="org.apache.maven.archiva.repository.events.RepositoryListener" role-hint="indexer"
 */
public class RepositoryContentIndexEventListener
    implements RepositoryListener
{
    /**
     * @plexus.requirement role-hint="lucene"
     */
    private RepositoryContentIndexFactory indexFactory;

    public void deleteArtifact( ManagedRepositoryContent repository, ArchivaArtifact artifact )
    {
        try
        {
            RepositoryContentIndex index = indexFactory.createFileContentIndex( repository.getRepository() );
            FileContentRecord fileContentRecord = new FileContentRecord();
            fileContentRecord.setRepositoryId( repository.getRepository().getId() );
            fileContentRecord.setFilename( repository.toPath( artifact ) );
            index.deleteRecord( fileContentRecord );

            index = indexFactory.createHashcodeIndex( repository.getRepository() );
            HashcodesRecord hashcodesRecord = new HashcodesRecord();
            fileContentRecord.setRepositoryId( repository.getRepository().getId() );
            hashcodesRecord.setArtifact( artifact );
            index.deleteRecord( hashcodesRecord );

            index = indexFactory.createBytecodeIndex( repository.getRepository() );
            BytecodeRecord bytecodeRecord = new BytecodeRecord();
            fileContentRecord.setRepositoryId( repository.getRepository().getId() );
            bytecodeRecord.setArtifact( artifact );
            index.deleteRecord( bytecodeRecord );
        }
        catch ( RepositoryIndexException e )
        {
            // Ignore
        }
    }
}
