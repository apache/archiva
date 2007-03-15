package org.apache.maven.archiva.consumers;

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

import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AbstractConsumerTestCase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AbstractConsumerTestCase extends PlexusTestCase
{
    protected ArchivaRepository getLegacyRepository() throws Exception
    {
        File repoBaseDir = new File( getBasedir(), "src/test/legacy-repository" );
        ArchivaRepository repository = createRepository( repoBaseDir, "legacy" );
        resetRepositoryState( repository );
        return repository;
    }

    protected ArchivaRepository getDefaultRepository() throws Exception
    {
        File repoBaseDir = new File( getBasedir(), "src/test/repository" );
        ArchivaRepository repository = createRepository( repoBaseDir, "default" );
        resetRepositoryState( repository );
        return repository;
    }

    private void resetRepositoryState( ArchivaRepository repository ) throws IOException
    {
        File repoBaseDir = new File( repository.getRepositoryURL().getPath() );

        List tmpfiles = FileUtils.getFiles( repoBaseDir, ".*", "" );
        for ( Iterator it = tmpfiles.iterator(); it.hasNext(); )
        {
            File hit = (File) it.next();
            if ( hit.exists() )
            {
                if ( hit.isFile() )
                {
                    hit.delete();
                }

                if ( hit.isDirectory() )
                {
                    FileUtils.deleteDirectory( hit );
                }
            }
        }
    }

    protected ArchivaRepository createRepository( File basedir, String layout ) throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, layout );

        ArchivaRepository repo = new ArchivaRepository();
        repo.setId( "discoveryRepo" );
        repo.setUrl( "file://" + basedir );
        repo.setLayout( repoLayout );

        return repo;
    }

    public List getLegacyLayoutArtifactPaths()
    {
        List files = new ArrayList();

        files.add( "invalid/jars/1.0/invalid-1.0.jar" );
        files.add( "invalid/jars/invalid-1.0.rar" );
        files.add( "invalid/jars/invalid.jar" );
        files.add( "invalid/invalid-1.0.jar" );
        files.add( "javax.sql/jars/jdbc-2.0.jar" );
        files.add( "org.apache.maven/jars/some-ejb-1.0-client.jar" );
        files.add( "org.apache.maven/jars/testing-1.0.jar" );
        files.add( "org.apache.maven/jars/testing-1.0-sources.jar" );
        files.add( "org.apache.maven/jars/testing-UNKNOWN.jar" );
        files.add( "org.apache.maven/jars/testing-1.0.zip" );
        files.add( "org.apache.maven/jars/testing-1.0-20050611.112233-1.jar" );
        files.add( "org.apache.maven/jars/testing-1.0.tar.gz" );
        files.add( "org.apache.maven.update/jars/test-not-updated-1.0.jar" );
        files.add( "org.apache.maven.update/jars/test-updated-1.0.jar" );

        return files;
    }
    
    public List getDefaultLayoutArtifactPaths()
    {
        List files = new ArrayList();

        files.add( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar" );
        files.add( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar" );
        files.add( "invalid/invalid/1.0/invalid-1.0b.jar" );
        files.add( "invalid/invalid/1.0/invalid-2.0.jar" );
        files.add( "invalid/invalid-1.0.jar" );
        files.add( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar" );
        files.add( "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1-javadoc.jar" );
        files.add( "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1.jar" );
        files.add( "org/apache/maven/A/1.0/A-1.0.war" );
        files.add( "org/apache/maven/A/1.0/A-1.0.pom" );
        files.add( "org/apache/maven/B/2.0/B-2.0.pom" );
        files.add( "org/apache/maven/B/1.0/B-1.0.pom" );
        files.add( "org/apache/maven/some-ejb/1.0/some-ejb-1.0-client.jar" );
        files.add( "org/apache/maven/C/1.0/C-1.0.war" );
        files.add( "org/apache/maven/C/1.0/C-1.0.pom" );
        files.add( "org/apache/maven/update/test-not-updated/1.0/test-not-updated-1.0.pom" );
        files.add( "org/apache/maven/update/test-not-updated/1.0/test-not-updated-1.0.jar" );
        files.add( "org/apache/maven/update/test-updated/1.0/test-updated-1.0.pom" );
        files.add( "org/apache/maven/update/test-updated/1.0/test-updated-1.0.jar" );
        files.add( "org/apache/maven/discovery/1.0/discovery-1.0.pom" );
        files.add( "org/apache/maven/testing/1.0/testing-1.0-test-sources.jar" );
        files.add( "org/apache/maven/testing/1.0/testing-1.0.jar" );
        files.add( "org/apache/maven/testing/1.0/testing-1.0-sources.jar" );
        files.add( "org/apache/maven/testing/1.0/testing-1.0.zip" );
        files.add( "org/apache/maven/testing/1.0/testing-1.0.tar.gz" );
        files.add( "org/apache/maven/samplejar/2.0/samplejar-2.0.pom" );
        files.add( "org/apache/maven/samplejar/2.0/samplejar-2.0.jar" );
        files.add( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" );
        files.add( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" );
        files.add( "org/apache/testgroup/discovery/1.0/discovery-1.0.pom" );
        files.add( "javax/sql/jdbc/2.0/jdbc-2.0.jar" );

        return files;
    }
}
