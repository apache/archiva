package org.apache.maven.archiva.repository.project.resolvers;

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

import java.io.File;

import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class ManagedRepositoryProjectResolverTest
    extends PlexusInSpringTestCase
{
    private ManagedRepositoryProjectResolver resolver;
    
    public void setUp() throws Exception
    {
        super.setUp();
        
        FileTypes fileTypes = new MockFileTypes();
        
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( "test-repo" );
        repoConfig.setLocation( new File( getBasedir(), "target/test-classes/test-repo" ).getPath() );
        repoConfig.setName( "Test Repository" );
        
        ManagedDefaultRepositoryContent repository = new ManagedDefaultRepositoryContent();
        repository.setRepository( repoConfig );
        repository.setFiletypes( fileTypes );
        
        resolver = new ManagedRepositoryProjectResolver( repository, new ProjectModel400Reader() );        
    }
    
    public void testResolveSnapshotUniqueVersionPresent()
        throws Exception
    {
        VersionedReference ref = new VersionedReference();
        ref.setGroupId( "org.apache.archiva" );
        ref.setArtifactId( "unique-version" );
        ref.setVersion( "1.0-SNAPSHOT" );
        
        try
        {
            ArchivaProjectModel model = resolver.resolveProjectModel( ref );
            
            assertNotNull( model );
            assertEquals( "org.apache.archiva", model.getGroupId() );
            assertEquals( "unique-version", model.getArtifactId() );
            assertEquals( "1.0-SNAPSHOT", model.getVersion() );
            assertEquals( "Unique Version Snapshot - Build 3", model.getName() );
        } 
        catch ( Exception e )
        {
            fail( "The latest timestamp should have been found!" );
        }
    }
    
    public void testResolveSnapshotGenericVersionPresent()
        throws Exception
    {
        VersionedReference ref = new VersionedReference();
        ref.setGroupId( "org.apache.archiva" );
        ref.setArtifactId( "generic-version" );
        ref.setVersion( "1.0-SNAPSHOT" );
        
        ArchivaProjectModel model = resolver.resolveProjectModel( ref );
        
        assertNotNull( model );
        assertEquals( "org.apache.archiva", model.getGroupId() );
        assertEquals( "generic-version", model.getArtifactId() );
        assertEquals( "1.0-SNAPSHOT", model.getVersion() );
    }
    
    public void testResolveSuccessful()
        throws Exception
    {
        VersionedReference ref = new VersionedReference();
        ref.setGroupId( "org.apache.archiva" );
        ref.setArtifactId( "released-version" );
        ref.setVersion( "1.0" );
        
        ArchivaProjectModel model = resolver.resolveProjectModel( ref );
        
        assertNotNull( model );
        assertEquals( "org.apache.archiva", model.getGroupId() );
        assertEquals( "released-version", model.getArtifactId() );
        assertEquals( "1.0", model.getVersion() );
    }
    
    public void testResolveNotFound()
        throws Exception
    {
        VersionedReference ref = new VersionedReference();
        ref.setGroupId( "org.apache.archiva" );
        ref.setArtifactId( "non-existant" );
        ref.setVersion( "2.0" );
        
        try
        {
            resolver.resolveProjectModel( ref );
            fail( "An exception should have been thrown." );
        }
        catch( Exception e )
        {
            assertTrue( true );
        }
    }    
    
    class MockFileTypes
        extends FileTypes
    {
        public boolean matchesArtifactPattern( String relativePath )
        {
            return true;
        }
    }
}
