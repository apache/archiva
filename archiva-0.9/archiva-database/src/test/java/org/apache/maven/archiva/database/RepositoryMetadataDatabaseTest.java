package org.apache.maven.archiva.database;

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

import org.apache.maven.archiva.database.key.MetadataKey;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.ibatis.PlexusIbatisHelper;

/**
 * RepositoryMetadataDatabaseTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryMetadataDatabaseTest
    extends PlexusTestCase
{
    /**
     * @plexus.requirement 
     */
    protected PlexusIbatisHelper ibatisHelper;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void testRepositoryMetadataCreationAndDeletion() throws Exception
    {
        RepositoryMetadataDatabase db = (RepositoryMetadataDatabase) lookup( "org.apache.maven.archiva.database.RepositoryMetadataDatabase", "default" );
        
        assertNotNull( db );
        assertTrue( db.tableExists( "RepositoryMetadata" ) );
        assertTrue( db.tableExists( "MetadataKeys" ) );
        
        db.dropTable( "RepositoryMetadata" );
        db.dropTable( "MetadataKeys" );
        
        assertFalse( db.tableExists( "RepositoryMetadata" ) );
        assertFalse( db.tableExists( "MetadataKeys" ) );
    }
    
    public void testMetadataKeyRetrieval() throws Exception
    {
        RepositoryMetadataDatabase db = (RepositoryMetadataDatabase) lookup( "org.apache.maven.archiva.database.RepositoryMetadataDatabase", "default" );
        
        Metadata metadata = new Metadata();
        metadata.setArtifactId( "testArtifactId" );
        metadata.setGroupId( "testGroupId" );
        metadata.setVersion( "testVersion" );
           
        MetadataKey metadataKey = db.getMetadataKey( metadata );
        
        assertTrue( metadataKey.getMetadataKey() > 0 );
        assertEquals( metadataKey.getArtifactId(), metadata.getArtifactId() );
        assertEquals( metadataKey.getGroupId(), metadata.getGroupId() );
        assertEquals( metadataKey.getVersion(), metadata.getVersion() );        
        
        db.dropTable( "RepositoryMetadata" );
        db.dropTable( "MetadataKeys" );
        
        assertFalse( db.tableExists( "RepositoryMetadata" ) );
        assertFalse( db.tableExists( "MetadataKeys" ) );
        
    }
    
    
}
