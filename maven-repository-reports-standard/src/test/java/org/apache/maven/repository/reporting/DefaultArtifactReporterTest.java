package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.lang.System;

/**
 * Test the artifact reporter.
 *
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class DefaultArtifactReporterTest
    extends AbstractRepositoryReportsTestCase
{
    private static final String[] testRepoStructure = { "valid-poms/", "invalid-poms/" };

    private ArtifactReporter reporter;

    public DefaultArtifactReporterTest()
    {
        super( System.getProperty( "basedir" ) + "/src/test/repository/", testRepoStructure );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = (ArtifactReporter) lookup( ArtifactReporter.ROLE, "default" );
    }

    public void testAddSuccess() throws Exception
    {
        assertTrue( writeTestArtifact( "valid-poms/", "test" ) );
    }

    protected void tearDown()
        throws Exception
    {
        reporter = null;
        super.tearDown();
    }
}
