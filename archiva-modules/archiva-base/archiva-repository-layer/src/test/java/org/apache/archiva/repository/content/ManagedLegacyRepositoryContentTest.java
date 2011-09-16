package org.apache.archiva.repository.content;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.layout.LayoutException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import static org.junit.Assert.*;

/**
 * ManagedLegacyRepositoryContentTest
 *
 * @version $Id$
 */
public class ManagedLegacyRepositoryContentTest
    extends AbstractLegacyRepositoryContentTestCase
{
    @Inject
    @Named( value = "managedRepositoryContent#legacy" )
    private ManagedRepositoryContent repoContent;

    @Before
    public void setUp()
        throws Exception
    {
        File repoDir = new File( "src/test/repositories/legacy-repository" );

        ManagedRepository repository = createRepository( "testRepo", "Unit Test Repo", repoDir );
        repository.setLayout( "legacy" );

        //repoContent = (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class, "legacy" );
        repoContent.setRepository( repository );
    }

    @Test
    public void testGetVersionsFromProjectReference()
        throws Exception
    {
        assertVersions( "org.apache.maven", "testing", new String[]{ "UNKNOWN",
//            "1.0-javadoc",
//            "1.0-sources",
            "1.0", "1.0-20050611.112233-1" } );
    }

    @Test
    public void testGetVersionsFromVersionedReference()
        throws Exception
    {
        assertVersions( "org.apache.maven", "testing", "1.0", new String[]{
//            "1.0-javadoc",
//            "1.0-sources",
            "1.0", "1.0-20050611.112233-1" } );
    }

    private void assertVersions( String groupId, String artifactId, String[] expectedVersions )
        throws Exception
    {
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( groupId );
        reference.setArtifactId( artifactId );

        // Request the versions.
        Set<String> testedVersionSet = repoContent.getVersions( reference );

        // Sort the list (for asserts later)
        List<String> testedVersions = new ArrayList<String>();
        testedVersions.addAll( testedVersionSet );
        Collections.sort( testedVersions, new VersionComparator() );

        // Test the expected array of versions, to the actual tested versions
        assertEquals( "Assert (Project) Versions: length/size", expectedVersions.length, testedVersions.size() );

        for ( int i = 0; i < expectedVersions.length; i++ )
        {
            String actualVersion = testedVersions.get( i );
            assertEquals( "(Project) Versions[" + i + "]", expectedVersions[i], actualVersion );
        }
    }

    private void assertVersions( String groupId, String artifactId, String version, String[] expectedVersions )
        throws Exception
    {
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( groupId );
        reference.setArtifactId( artifactId );
        reference.setVersion( version );

        // Request the versions.
        Set<String> testedVersionSet = repoContent.getVersions( reference );

        // Sort the list (for asserts later)
        List<String> testedVersions = new ArrayList<String>();
        testedVersions.addAll( testedVersionSet );
        Collections.sort( testedVersions, new VersionComparator() );

        // Test the expected array of versions, to the actual tested versions
        assertEquals( "Assert (Project) Versions: length/size", expectedVersions.length, testedVersions.size() );

        for ( int i = 0; i < expectedVersions.length; i++ )
        {
            String actualVersion = testedVersions.get( i );
            assertEquals( "(Project) Versions[" + i + "]", expectedVersions[i], actualVersion );
        }
    }

    @Test
    public void testGetRelatedArtifacts()
        throws Exception
    {
        ArtifactReference reference = createArtifact( "org.apache.maven", "testing", "1.0", null, "jar" );

        Set<ArtifactReference> related = repoContent.getRelatedArtifacts( reference );
        assertNotNull( related );

        String expected[] = new String[]{ "org.apache.maven/jars/testing-1.0.jar",
            "org.apache.maven/java-sources/testing-1.0-sources.jar",
            "org.apache.maven/jars/testing-1.0-20050611.112233-1.jar", "org.apache.maven/poms/testing-1.0.pom",
            "org.apache.maven/distributions/testing-1.0.tar.gz", "org.apache.maven/distributions/testing-1.0.zip",
            "org.apache.maven/javadoc.jars/testing-1.0-javadoc.jar" };

        StringBuffer relatedDebugString = new StringBuffer();
        relatedDebugString.append( "[" );
        for ( ArtifactReference ref : related )
        {
            String actualPath = repoContent.toPath( ref );
            relatedDebugString.append( actualPath ).append( ":" );
        }
        relatedDebugString.append( "]" );

        for ( String expectedPath : expected )
        {
            boolean found = false;
            for ( ArtifactReference actualRef : related )
            {
                String actualPath = repoContent.toPath( actualRef );
                if ( actualPath.endsWith( expectedPath ) )
                {
                    found = true;
                    break;
                }
            }
            if ( !found )
            {
                fail( "Unable to find expected artifact [" + expectedPath + "] in list of related artifacts. "
                          + "Related <" + relatedDebugString + ">" );
            }
        }
        assertEquals( "Related <" + relatedDebugString + ">:", expected.length, related.size() );
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
