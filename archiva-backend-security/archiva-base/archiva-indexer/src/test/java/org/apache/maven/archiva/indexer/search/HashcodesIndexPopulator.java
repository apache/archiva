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

import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecordLoader;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.AssertionFailedError;

/**
 * HashcodesIndexPopulator 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class HashcodesIndexPopulator
    implements IndexPopulator
{

    public Map<String, ArchivaArtifact> getObjectMap()
    {
        Map<String, ArchivaArtifact> dumps = new HashMap<String, ArchivaArtifact>();

        // archiva-common-1.0.jar.txt
        dumps.put( "archiva-common",
                   createArchivaArtifact( "org.apache.maven.archiva", "archiva-common", "1.0", "", "jar" ) );

        // continuum-webapp-1.0.3-SNAPSHOT.war.txt
        dumps.put( "continuum-webapp", createArchivaArtifact( "org.apache.maven.continuum", "continuum-webapp",
                                                              "1.0.3-SNAPSHOT", "", "war" ) );

        // daytrader-ear-1.1.ear.txt
        dumps.put( "daytrader-ear", createArchivaArtifact( "org.apache.geronimo", "daytrader-ear", "1.1", "", "ear" ) );

        // maven-archetype-simple-1.0-alpha-4.jar.txt
        dumps.put( "maven-archetype-simple", createArchivaArtifact( "org.apache.maven", "maven-archetype-simple",
                                                                    "1.0-alpha-4", "", "maven-archetype" ) );

        // maven-help-plugin-2.0.2-20070119.121239-2.jar.txt
        dumps.put( "maven-help-plugin", createArchivaArtifact( "org.apache.maven.plugins", "maven-help-plugin",
                                                               "2.0.2-20070119.121239-2", "", "maven-plugin" ) );

        // redback-authorization-open-1.0-alpha-1-SNAPSHOT.jar.txt
        dumps.put( "redback-authorization-open", createArchivaArtifact( "org.codehaus.plexus.redback",
                                                                        "redback-authorization-open",
                                                                        "1.0-alpha-1-SNAPSHOT", "", "jar" ) );

        // testng-5.1-jdk15.jar.txt
        dumps.put( "testng", createArchivaArtifact( "org.testng", "testng", "5.1", "jdk15", "jar" ) );

        // wagon-provider-api-1.0-beta-3-20070209.213958-2.jar.txt
        dumps.put( "wagon-provider-api", createArchivaArtifact( "org.apache.maven.wagon", "wagon-provider-api",
                                                                "1.0-beta-3-20070209.213958-2", "", "jar" ) );

        return dumps;
    }

    public Map<String, HashcodesRecord> populate( File basedir )
    {
        Map<String, HashcodesRecord> records = new HashMap<String, HashcodesRecord>();

        for ( Entry<String, ArchivaArtifact> entry : getObjectMap().entrySet() )
        {
            ArchivaArtifact artifact = entry.getValue();
            File dumpFile = getDumpFile( basedir, artifact );
            HashcodesRecord record = HashcodesRecordLoader.loadRecord( dumpFile, artifact );
            record.setRepositoryId( "test-repo" );
            records.put( entry.getKey(), record );
        }

        return records;
    }

    protected File getDumpFile( File basedir, ArchivaArtifact artifact )
    {
        File dumpDir = new File( basedir, "src/test/artifact-dumps" );
        StringBuffer filename = new StringBuffer();

        filename.append( artifact.getArtifactId() ).append( "-" ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            filename.append( "-" ).append( artifact.getClassifier() );
        }

        filename.append( "." );

        // TODO: use the ArtifactExtensionMapping object
        if ( "maven-plugin".equals( artifact.getType() ) || "maven-archetype".equals( artifact.getType() ) )
        {
            filename.append( "jar" );
        }
        else
        {
            filename.append( artifact.getType() );
        }
        filename.append( ".txt" );

        File dumpFile = new File( dumpDir, filename.toString() );

        if ( !dumpFile.exists() )
        {
            throw new AssertionFailedError(
                "Dump file " + dumpFile.getAbsolutePath() + " does not exist (should it?)." );
        }

        return dumpFile;
    }

    private ArchivaArtifact createArchivaArtifact( String groupId, String artifactId, String version, String classifier,
                                                   String type )
    {
        ArchivaArtifact artifact = new ArchivaArtifact( groupId, artifactId, version, classifier, type );
        return artifact;
    }
}
