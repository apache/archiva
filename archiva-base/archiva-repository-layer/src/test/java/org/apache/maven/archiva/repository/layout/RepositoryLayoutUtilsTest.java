package org.apache.maven.archiva.repository.layout;

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

import junit.framework.TestCase;

/**
 * RepositoryLayoutUtilsTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryLayoutUtilsTest extends TestCase
{
    public void testSplitFilenameBasic() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-2.1.jar", "commons-lang" ),
                             "commons-lang", "2.1", null, "jar" );
    }
    
    public void testSplitFilenameMavenTestPlugin() throws LayoutException
    {
        // Using maven 2 logic (artifactId is present in full path)
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "maven-test-plugin-1.8.2.jar", "maven-test-plugin" ),
                             "maven-test-plugin", "1.8.2", null, "jar" );

        // Using maven 1 logic (artifactId is unknown)
        // [MRM-519] fail to resolve artifactId for libs that contain versionKeyword in artifactId, like "maven-test-plugin"
        /*
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "maven-test-plugin-1.8.2.jar", null ),
                             "maven-test-plugin", "1.8.2", null, "jar" );
         */
    }

    public void testSplitFilenameAlphaVersion() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-2.0-alpha-1.jar", "commons-lang" ),
                             "commons-lang", "2.0-alpha-1", null, "jar" );
    }

    public void testSplitFilenameSnapshot() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "foo-2.0-SNAPSHOT.jar", "foo" ), "foo",
                             "2.0-SNAPSHOT", null, "jar" );
    }

    public void testSplitFilenameUniqueSnapshot() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "fletch-2.0-20060822-123456-35.tar.gz", "fletch" ),
                             "fletch", "2.0-20060822-123456-35", null, "tar.gz" );
    }

    public void testSplitFilenameBasicClassifier() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-2.1-sources.jar", "commons-lang" ),
                             "commons-lang", "2.1", "sources", "jar" );
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-2.1-javadoc.jar", "commons-lang" ),
                             "commons-lang", "2.1", "javadoc", "jar" );
    }

    public void testSplitFilenameAlphaClassifier() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-2.0-alpha-1-sources.jar",
                                                                  "commons-lang" ), "commons-lang", "2.0-alpha-1",
                             "sources", "jar" );
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-2.0-alpha-1-javadoc.jar",
                                                                  "commons-lang" ), "commons-lang", "2.0-alpha-1",
                             "javadoc", "jar" );
    }

    public void testSplitFilenameSnapshotClassifier() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-3.1-SNAPSHOT-sources.jar",
                                                                  "commons-lang" ), "commons-lang", "3.1-SNAPSHOT",
                             "sources", "jar" );
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-3.1-SNAPSHOT-javadoc.jar",
                                                                  "commons-lang" ), "commons-lang", "3.1-SNAPSHOT",
                             "javadoc", "jar" );
    }

    public void testSplitFilenameUniqueSnapshotClassifier() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-3.1-SNAPSHOT-sources.jar",
                                                                  "commons-lang" ), "commons-lang", "3.1-SNAPSHOT",
                             "sources", "jar" );
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commons-lang-3.1-SNAPSHOT-javadoc.jar",
                                                                  "commons-lang" ), "commons-lang", "3.1-SNAPSHOT",
                             "javadoc", "jar" );
    }

    public void testSplitFilenameApacheIncubator() throws LayoutException
    {
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "cxf-common-2.0-incubator-M1.pom", null ),
                             "cxf-common", "2.0-incubator-M1", null, "pom" );
        assertFilenameParts( RepositoryLayoutUtils.splitFilename( "commonj-api_r1.1-1.0-incubator-M2.jar", null ),
                             "commonj-api_r1.1", "1.0-incubator-M2", null, "jar" );
    }

    public void testSplitFilenameBlankInputs()
    {
        try
        {
            RepositoryLayoutUtils.splitFilename( null, null );
            fail( "Should have thrown an IllegalArgumentException." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
        catch ( LayoutException e )
        {
            fail( "Should have thrown an IllegalArgumentException." );
        }

        try
        {
            RepositoryLayoutUtils.splitFilename( "", null );
            fail( "Should have thrown an IllegalArgumentException." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
        catch ( LayoutException e )
        {
            fail( "Should have thrown an IllegalArgumentException." );
        }

        try
        {
            RepositoryLayoutUtils.splitFilename( "   ", null );
            fail( "Should have thrown an IllegalArgumentException." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
        catch ( LayoutException e )
        {
            fail( "Should have thrown an IllegalArgumentException." );
        }

        try
        {
            RepositoryLayoutUtils.splitFilename( " \t  \n  ", null );
            fail( "Should have thrown an IllegalArgumentException." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
        catch ( LayoutException e )
        {
            fail( "Should have thrown an IllegalArgumentException." );
        }
    }

    public void testSplitFilenameBadInputs()
    {
        try
        {
            RepositoryLayoutUtils.splitFilename( "commons-lang.jar", null );
            fail( "Should have thrown a LayoutException (No Version)." );
        }
        catch ( LayoutException e )
        {
            /* Expected Path */
        }

        try
        {
            RepositoryLayoutUtils.splitFilename( "geronimo-store", null );
            fail( "Should have thrown a LayoutException (No Extension)." );
        }
        catch ( LayoutException e )
        {
            /* Expected Path */
        }

        try
        {
            RepositoryLayoutUtils.splitFilename( "The Sixth Sick Sheiks Sixth Sheep is Sick.", null );
            fail( "Should have thrown a LayoutException (No Extension)." );
        }
        catch ( LayoutException e )
        {
            /* Expected Path */
        }

        try
        {
            RepositoryLayoutUtils.splitFilename( "1.0.jar", null );
            fail( "Should have thrown a LayoutException (No Artifact ID)." );
        }
        catch ( LayoutException e )
        {
            /* Expected Path */
        }
    }

    private void assertFilenameParts( FilenameParts actualParts, String artifactId, String version, String classifier,
                                      String extension )
    {
        assertEquals( "Split - artifactId", artifactId, actualParts.artifactId );
        assertEquals( "Split - version", version, actualParts.version );
        assertEquals( "Split - classifier", classifier, actualParts.classifier );
        assertEquals( "Split - extension", extension, actualParts.extension );
    }
}
