package org.apache.maven.archiva.indexer.search;

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

import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.content.DefaultPathParser;
import org.apache.maven.archiva.repository.content.PathParser;
import org.apache.maven.archiva.repository.layout.LayoutException;

/**
 * FileContentIndexPopulator 
 *
 * @version $Id$
 */
public class FileContentIndexPopulator
    implements IndexPopulator
{
    public Map<String, ArchivaArtifact> getObjectMap()
    {
        return null;
    }

    public Map<String, FileContentRecord> populate( File basedir )
    {
        Map<String, FileContentRecord> map = new HashMap<String, FileContentRecord>();

        File repoDir = new File( basedir, "src/test/managed-repository" );

        String prefix = "org/apache/maven/archiva/record/";

        map.put( "parent-pom-1", createFileContentRecord( repoDir, prefix + "parent-pom/1/parent-pom-1.pom" ) );
        map.put( "child-pom-1.0-SNAPSHOT", createFileContentRecord( repoDir, prefix
            + "test-child-pom/1.0-SNAPSHOT/test-child-pom-1.0-20060728.121314-1.pom" ) );
        map.put( "test-archetype-1.0", createFileContentRecord( repoDir, prefix
            + "test-archetype/1.0/test-archetype-1.0.pom" ) );
        map.put( "test-jar-and-pom-1.0-alpha-1", createFileContentRecord( repoDir, prefix
            + "test-jar-and-pom/1.0-alpha-1/test-jar-and-pom-1.0-alpha-1.pom" ) );
        map.put( "test-plugin-1.0", createFileContentRecord( repoDir, prefix + "test-plugin/1.0/test-plugin-1.0.pom" ) );
        map.put( "test-pom-1.0", createFileContentRecord( repoDir, prefix + "test-pom/1.0/test-pom-1.0.pom" ) );
        map.put( "test-skin-1.0", createFileContentRecord( repoDir, prefix + "test-skin/1.0/test-skin-1.0.pom" ) );

        map.put("ant-1.5.pom", createFileContentRecord(repoDir, "ant/ant/1.5/ant-1.5.pom"));
        map.put("ant-1.5.1.pom", createFileContentRecord(repoDir, "ant/ant/1.5.1/ant-1.5.1.pom"));
        map.put("ant-junit-1.6.5.pom", createFileContentRecord(repoDir, "ant/ant-junit/1.6.5/ant-junit-1.6.5.pom"));
        map.put("ant-optional-1.5.1.pom", createFileContentRecord(repoDir, "ant/ant-optional/1.5.1/ant-optional-1.5.1.pom"));
        
        return map;
    }

    private FileContentRecord createFileContentRecord( File repoDir, String path )
    {
        File pathToFile = new File( repoDir, path );

        if ( !pathToFile.exists() )
        {
            throw new AssertionFailedError( "Can't find test file: " + pathToFile.getAbsolutePath() );
        }

        FileContentRecord record = new FileContentRecord();
        record.setRepositoryId( "test-repo" );
        record.setFilename( path );

        PathParser pathParser = new DefaultPathParser();
        try
        {
            ArtifactReference reference = pathParser.toArtifactReference(path);
            ArchivaArtifact artifact = new ArchivaArtifact( reference );
            record.setArtifact(artifact);
        }
        catch (LayoutException e)
        {
            throw new RuntimeException(e);
        }

        return record;
    }
}
