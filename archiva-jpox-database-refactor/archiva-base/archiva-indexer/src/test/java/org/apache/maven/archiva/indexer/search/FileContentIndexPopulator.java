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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;

/**
 * FileContentIndexPopulator 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FileContentIndexPopulator
    implements IndexPopulator
{
    public Map getObjectMap()
    {
        return null;
    }

    public Map populate( File basedir )
    {
        Map map = new HashMap();

        File repoDir = new File( basedir, "src/test/managed-repository" );

        map.put( "parent-pom-1",
                 createFileContentRecord( repoDir, "org/apache/maven/archiva/record/parent-pom/1/parent-pom-1.pom" ) );

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
        record.setFile( pathToFile );

        try
        {
            record.setContents( FileUtils.readFileToString( pathToFile, null ) );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new AssertionFailedError( "Can't load test file contents: " + pathToFile.getAbsolutePath() );
        }

        return record;
    }
}
