package org.apache.maven.archiva.indexer.hashcodes;

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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.AbstractIndexCreationTestCase;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.lucene.LuceneIndexHandlers;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;

/**
 * HashcodesIndexTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class HashcodesIndexTest extends AbstractIndexCreationTestCase
{
    public String getIndexName()
    {
        return "hashcodes";
    }

    public LuceneIndexHandlers getIndexHandler()
    {
        return new HashcodesHandlers();
    }

    public RepositoryContentIndex createIndex( RepositoryContentIndexFactory indexFactory, ManagedRepositoryConfiguration repository )
    {
        return indexFactory.createHashcodeIndex( repository );
    }

    protected LuceneRepositoryContentRecord createSimpleRecord()
    {
        ArchivaArtifact artifact = new ArchivaArtifact( "com.foo", "projfoo", "1.0", "", "jar" );
        
        HashcodesRecord record = new HashcodesRecord();
        record.setRepositoryId( "test-repo" );
        record.setArtifact( artifact );
        
        artifact.getModel().setChecksumSHA1( "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        artifact.getModel().setChecksumMD5( "3a0adc365f849366cd8b633cad155cb7" );
        
        return record;
    }
}
