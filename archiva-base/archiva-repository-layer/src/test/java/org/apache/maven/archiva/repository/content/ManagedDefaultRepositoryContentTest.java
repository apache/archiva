package org.apache.maven.archiva.repository.content;

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

import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * ManagedDefaultRepositoryContentTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedDefaultRepositoryContentTest
    extends AbstractDefaultRepositoryContentTestCase
{
    private ManagedRepositoryContent repoContent;

    public void testGetVersionsBadArtifact()
        throws Exception
    {
        assertGetVersions( "bad_artifact", Collections.EMPTY_LIST );
    }

    public void testGetVersionsMissingMultipleVersions()
        throws Exception
    {
        assertGetVersions( "missing_metadata_b", Arrays.asList( "1.0", "1.0.1", "2.0", "2.0.1", "2.0-20070821-dev" ) );
    }

    public void testGetVersionsSimple()
        throws Exception
    {
        assertVersions( "proxied_multi", "2.1", new String[] { "2.1" } );
    }

    public void testGetVersionsSimpleYetIncomplete()
        throws Exception
    {
        assertGetVersions( "incomplete_metadata_a", Collections.singletonList( "1.0" ) );
    }

    public void testGetVersionsSimpleYetMissing()
        throws Exception
    {
        assertGetVersions( "missing_metadata_a", Collections.singletonList( "1.0" ) );
    }

    public void testGetVersionsSnapshotA()
        throws Exception
    {
        assertVersions( "snap_shots_a", "1.0-alpha-11-SNAPSHOT", new String[] {
            "1.0-alpha-11-SNAPSHOT",
            "1.0-alpha-11-20070221.194724-2",
            "1.0-alpha-11-20070302.212723-3",
            "1.0-alpha-11-20070303.152828-4",
            "1.0-alpha-11-20070305.215149-5",
            "1.0-alpha-11-20070307.170909-6",
            "1.0-alpha-11-20070314.211405-9",
            "1.0-alpha-11-20070316.175232-11" } );
    }

    public void testToMetadataPathFromProjectReference()
    {
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( "com.foo" );
        reference.setArtifactId( "foo-tool" );

        assertEquals( "com/foo/foo-tool/maven-metadata.xml", repoContent.toMetadataPath( reference ) );
    }

    public void testToMetadataPathFromVersionReference()
    {
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "com.foo" );
        reference.setArtifactId( "foo-tool" );
        reference.setVersion( "1.0" );

        assertEquals( "com/foo/foo-tool/1.0/maven-metadata.xml", repoContent.toMetadataPath( reference ) );
    }

    public void testToPathOnNullArtifactReference()
    {
        try
        {
            ArtifactReference reference = null;
            repoContent.toPath( reference );
            fail( "Should have failed due to null artifact reference." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
    }

    private void assertGetVersions( String artifactId, List<String> expectedVersions )
        throws Exception
    {
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );

        // Use the test metadata-repository, which is already setup for
        // These kind of version tests.
        File repoDir = getTestFile( "src/test/repositories/metadata-repository" );
        repoContent.getRepository().setLocation( repoDir.getAbsolutePath() );

        // Request the versions.
        Set<String> testedVersionSet = repoContent.getVersions( reference );

        // Sort the list (for asserts)
        List<String> testedVersions = new ArrayList<String>();
        testedVersions.addAll( testedVersionSet );
        Collections.sort( testedVersions, new VersionComparator() );

        // Test the expected array of versions, to the actual tested versions
        assertEquals( "available versions", expectedVersions, testedVersions );
    }

    private void assertVersions( String artifactId, String version, String[] expectedVersions )
        throws Exception
    {
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );
        reference.setVersion( version );

        // Use the test metadata-repository, which is already setup for
        // These kind of version tests.
        File repoDir = getTestFile( "src/test/repositories/metadata-repository" );
        repoContent.getRepository().setLocation( repoDir.getAbsolutePath() );

        // Request the versions.
        Set<String> testedVersionSet = repoContent.getVersions( reference );

        // Sort the list (for asserts later)
        List<String> testedVersions = new ArrayList<String>();
        testedVersions.addAll( testedVersionSet );
        Collections.sort( testedVersions, new VersionComparator() );

        // Test the expected array of versions, to the actual tested versions
        assertEquals( "Assert Versions: length/size", expectedVersions.length, testedVersions.size() );

        for ( int i = 0; i < expectedVersions.length; i++ )
        {
            String actualVersion = testedVersions.get( i );
            assertEquals( "Versions[" + i + "]", expectedVersions[i], actualVersion );
        }
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        File repoDir = getTestFile( "src/test/repositories/default-repository" );

        ManagedRepositoryConfiguration repository = createRepository( "testRepo", "Unit Test Repo", repoDir );

        repoContent = (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class, "default" );
        repoContent.setRepository( repository );
    }

    @Override
    protected ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        return repoContent.toArtifactReference( path );
    }

    @Override
    protected String toPath( ArtifactReference reference )
    {
        return repoContent.toPath( reference );
    }
}
