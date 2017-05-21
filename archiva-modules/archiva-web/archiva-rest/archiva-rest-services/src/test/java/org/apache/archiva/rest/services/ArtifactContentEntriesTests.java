package org.apache.archiva.rest.services;
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

import junit.framework.TestCase;
import org.apache.archiva.rest.api.model.ArtifactContentEntry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
@RunWith( JUnit4.class )
public class ArtifactContentEntriesTests
    extends TestCase
{

    protected Logger log = LoggerFactory.getLogger( getClass() );


    DefaultBrowseService browseService = new DefaultBrowseService();


    public String getBasedir()
    {
        return System.getProperty( "basedir" );
    }

    @Test
    public void readArtifactContentEntriesRootPathNull()
        throws Exception
    {

        File file = new File( getBasedir(),
                              "src/test/repo-with-osgi/commons-logging/commons-logging/1.1/commons-logging-1.1.jar" );

        List<ArtifactContentEntry> artifactContentEntries = browseService.readFileEntries( file, null, "foo" );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 2 ).contains(
            new ArtifactContentEntry( "org", false, 0, "foo" ),
            new ArtifactContentEntry( "META-INF", false, 0, "foo" ) );

    }

    @Test
    public void readArtifactContentEntriesRootPathEmpty()
        throws Exception
    {

        File file = new File( getBasedir(),
                              "src/test/repo-with-osgi/commons-logging/commons-logging/1.1/commons-logging-1.1.jar" );

        List<ArtifactContentEntry> artifactContentEntries = browseService.readFileEntries( file, "", "foo" );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 2 ).contains(
            new ArtifactContentEntry( "org", false, 0, "foo" ),
            new ArtifactContentEntry( "META-INF", false, 0, "foo" ) );

    }

    @Test
    public void readArtifactContentEntriesRootSlash()
        throws Exception
    {

        File file = new File( getBasedir(),
                              "src/test/repo-with-osgi/commons-logging/commons-logging/1.1/commons-logging-1.1.jar" );

        List<ArtifactContentEntry> artifactContentEntries = browseService.readFileEntries( file, "/", "foo" );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 2 ).contains(
            new ArtifactContentEntry( "org", false, 0, "foo" ),
            new ArtifactContentEntry( "META-INF", false, 0, "foo" ) );

    }

    @Test
    public void readArtifactContentEntriesSecondDepthOnlyOneDirectory()
        throws Exception
    {

        File file = new File( getBasedir(),
                              "src/test/repo-with-osgi/commons-logging/commons-logging/1.1/commons-logging-1.1.jar" );

        List<ArtifactContentEntry> artifactContentEntries = browseService.readFileEntries( file, "org", "foo" );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 1 ).contains(
            new ArtifactContentEntry( "org/apache", false, 1, "foo" ) );

    }

    @Test
    public void readArtifactContentEntriesOnlyFiles()
        throws Exception
    {

        File file = new File( getBasedir(),
                              "src/test/repo-with-osgi/commons-logging/commons-logging/1.1/commons-logging-1.1.jar" );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.readFileEntries( file, "org/apache/commons/logging/impl/", "foo" );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 16 ).contains(
            new ArtifactContentEntry( "org/apache/commons/logging/impl/AvalonLogger.class", true, 5, "foo" ) );

    }

    @Test
    public void readArtifactContentEntriesDirectoryAndFiles()
        throws Exception
    {

        File file = new File( getBasedir(),
                              "src/test/repo-with-osgi/commons-logging/commons-logging/1.1/commons-logging-1.1.jar" );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.readFileEntries( file, "org/apache/commons/logging/", "foo" );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 10 ).contains(
            new ArtifactContentEntry( "org/apache/commons/logging/impl", false, 4, "foo" ),
            new ArtifactContentEntry( "org/apache/commons/logging/LogSource.class", true, 4, "foo" ) );

    }


}
