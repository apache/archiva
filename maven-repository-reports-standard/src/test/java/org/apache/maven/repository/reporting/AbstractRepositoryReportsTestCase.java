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

import java.io.File;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public abstract class AbstractRepositoryReportsTestCase
    extends PlexusTestCase
{
    private static String JAR = ".jar";

    private static String basedir;

    private static String[] directoryStructure;

    public AbstractRepositoryReportsTestCase( String basedir, String[] directoryStructure )
    {
        this.basedir = basedir;
        this.directoryStructure = directoryStructure;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        buildTestRepoPath();
    }

    private void buildTestRepoPath()
    {
        for ( int i = 0; i < directoryStructure.length; i++ )
        {
            File dir = new File( basedir + directoryStructure[i] );
            if ( !dir.exists() )
            {
                dir.mkdirs();
            }
        }
    }

    private void deleteTestRepoPath() throws Exception
    {
        FileUtils.deleteDirectory( basedir );
    }

    protected boolean writeTestArtifact( String relativePath, String artifactId )
        throws Exception
    {
        File artifact = new File( basedir + relativePath + artifactId + JAR );
        System.out.println( "" + basedir + relativePath + artifactId );
        return artifact.createNewFile();
    }

    protected void tearDown()
        throws Exception
    {
        deleteTestRepoPath();
        super.tearDown();
    }
}
