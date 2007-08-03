package org.apache.maven.archiva.indexer.bytecode;

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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.maven.archiva.indexer.AbstractSearchTestCase;
import org.apache.maven.archiva.indexer.ArtifactKeys;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.lucene.LuceneIndexHandlers;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepository;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * BytecodeSearchTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BytecodeSearchTest extends AbstractSearchTestCase
{
    public String getIndexName()
    {
        return "bytecode";
    }

    public LuceneIndexHandlers getIndexHandler()
    {
        return new BytecodeHandlers();
    }

    public RepositoryContentIndex createIndex( RepositoryContentIndexFactory indexFactory, ArchivaRepository repository )
    {
        return indexFactory.createBytecodeIndex( repository );
    }

    protected Map createSampleRecordsMap()
    {
        Map records = new HashMap();

        Map artifactDumps = getArchivaArtifactDumpMap();
        for ( Iterator iter = artifactDumps.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            ArchivaArtifact artifact = (ArchivaArtifact) entry.getValue();
            File dumpFile = getDumpFile( artifact );
            BytecodeRecord record = BytecodeRecordLoader.loadRecord( dumpFile, artifact );
            record.setRepositoryId( "test-repo" );
            records.put( entry.getKey(), record );
        }

        return records;
    }

    public void testExactMatchVersionSimple() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.VERSION_EXACT, new String[] { "archiva-common" }, "1.0" );
    }

    public void testExactMatchVersionSnapshot() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.VERSION_EXACT, new String[] { "continuum-webapp" }, "1.0.3-SNAPSHOT" );
    }

    public void testExactMatchVersionAlphaSnapshot() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.VERSION_EXACT, new String[] { "redback-authorization-open" },
                               "1.0-alpha-1-SNAPSHOT" );
    }

    public void testExactMatchVersionTimestampedSnapshot() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.VERSION_EXACT, new String[] { "wagon-provider-api" },
                               "1.0-beta-3-20070209.213958-2" );
    }

    public void testExactMatchVersionInvalid() throws Exception
    {
        assertQueryExactMatchNoResults( ArtifactKeys.VERSION_EXACT, "foo" );
    }

    public void testExactMatchGroupIdOrgApacheMavenArchiva() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.GROUPID_EXACT, new String[] { "archiva-common" },
                               "org.apache.maven.archiva" );
    }

    public void testExactMatchGroupIdOrgApacheMaven() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.GROUPID_EXACT, new String[] { "maven-archetype-simple" },
                               "org.apache.maven" );
    }

    public void testExactMatchGroupIdInvalid() throws Exception
    {
        assertQueryExactMatchNoResults( ArtifactKeys.GROUPID_EXACT, "foo" );
    }

    public void testExactMatchArtifactIdArchivaCommon() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.ARTIFACTID_EXACT, new String[] { "archiva-common" }, "archiva-common" );
    }

    public void testExactMatchArtifactIdTestNg() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.ARTIFACTID_EXACT, new String[] { "testng" }, "testng" );
    }

    public void testExactMatchArtifactIdInvalid() throws Exception
    {
        assertQueryExactMatchNoResults( ArtifactKeys.ARTIFACTID_EXACT, "foo" );
    }

    public void testExactMatchTypeJar() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.TYPE, ( new String[] { "archiva-common", "redback-authorization-open",
            "testng", "wagon-provider-api" } ), "jar" );
    }

    public void testExactMatchTypeWar() throws Exception
    {
        assertQueryExactMatch( ArtifactKeys.TYPE, ( new String[] { "continuum-webapp" } ), "war" );
    }

    /* TODO: Fix 'maven-plugin' type
     public void testExactMatchTypePlugin() throws Exception
     {
     assertQueryExactMatch( ArtifactKeys.TYPE, ( new String[] { "maven-help-plugin" } ), "maven-plugin" );
     } */

    /* TODO: Fix 'maven-archetype' type
     public void testExactMatchTypeArchetype() throws Exception
     {
     assertQueryExactMatch( ArtifactKeys.TYPE, ( new String[] { "maven-archetype-simple" } ), "maven-archetype" );
     }
     */

    public void testExactMatchTypeInvalid() throws Exception
    {
        assertQueryExactMatchNoResults( ArtifactKeys.TYPE, "foo" );
    }

    public void testMatchGroupIdOrgApacheMaven() throws Exception
    {
        assertQueryMatch( ArtifactKeys.GROUPID, new String[] { "archiva-common", "continuum-webapp",
            "maven-archetype-simple", "maven-help-plugin", "wagon-provider-api" }, "org.apache.maven" );
    }

    public void testMatchGroupIdMaven() throws Exception
    {
        assertQueryMatch( ArtifactKeys.GROUPID, new String[] { "archiva-common", "continuum-webapp",
            "maven-archetype-simple", "maven-help-plugin", "wagon-provider-api" }, "maven" );
    }

    public void testMatchGroupIdMavenMixed() throws Exception
    {
        assertQueryMatch( ArtifactKeys.GROUPID, new String[] { "archiva-common", "continuum-webapp",
            "maven-archetype-simple", "maven-help-plugin", "wagon-provider-api" }, "Maven" );
    }

    public void testMatchGroupIdInvalid() throws Exception
    {
        assertQueryMatchNoResults( ArtifactKeys.GROUPID, "foo" );
    }

    public void testMatchArtifactIdPlugin() throws Exception
    {
        assertQueryMatch( ArtifactKeys.ARTIFACTID, new String[] { "maven-help-plugin" }, "plugin" );
    }

    public void testMatchArtifactIdMaven() throws Exception
    {
        assertQueryMatch( ArtifactKeys.ARTIFACTID, new String[] { "maven-help-plugin", "maven-archetype-simple" },
                          "maven" );
    }

    public void testMatchArtifactIdHelp() throws Exception
    {
        assertQueryMatch( ArtifactKeys.ARTIFACTID, new String[] { "maven-help-plugin" }, "help" );
    }

    public void testMatchVersionOne() throws Exception
    {
        assertQueryMatch( ArtifactKeys.VERSION, new String[] { "daytrader-ear", "testng", "archiva-common",
            "redback-authorization-open", "maven-archetype-simple", "continuum-webapp", "wagon-provider-api" }, "1" );
    }

    public void testMatchVersionOneOh() throws Exception
    {
        assertQueryMatch( ArtifactKeys.VERSION, new String[] { "archiva-common", "continuum-webapp",
            "maven-archetype-simple", "redback-authorization-open", "wagon-provider-api" }, "1.0" );
    }

    public void testMatchVersionSnapshotLower() throws Exception
    {
        assertQueryMatch( ArtifactKeys.VERSION, new String[] { "continuum-webapp", "redback-authorization-open" },
                          "snapshot" );
    }

    public void testMatchVersionSnapshotUpper() throws Exception
    {
        assertQueryMatch( ArtifactKeys.VERSION, new String[] { "continuum-webapp", "redback-authorization-open" },
                          "SNAPSHOT" );
    }

    public void testMatchVersionAlpha() throws Exception
    {
        assertQueryMatch( ArtifactKeys.VERSION,
                          new String[] { "maven-archetype-simple", "redback-authorization-open" }, "alpha" );
    }

    public void testMatchVersionOneAlpha() throws Exception
    {
        assertQueryMatch( ArtifactKeys.VERSION, new String[] { "redback-authorization-open" }, "1.0-alpha-1" );
    }

    public void testMatchVersionInvalid() throws Exception
    {
        assertQueryMatchNoResults( ArtifactKeys.VERSION, "255" );
    }

    public void testMatchClassifierNotJdk15() throws Exception
    {
        BooleanQuery bQuery = new BooleanQuery();
        bQuery.add( new MatchAllDocsQuery(), BooleanClause.Occur.MUST );
        bQuery.add( createMatchQuery( ArtifactKeys.CLASSIFIER, "jdk15" ), BooleanClause.Occur.MUST_NOT );
        List results = search( bQuery );

        assertResults( new String[] { "archiva-common", "continuum-webapp", "redback-authorization-open",
            "daytrader-ear", "maven-archetype-simple", "maven-help-plugin", "wagon-provider-api" }, results );
    }

    public void testMatchClassifierJdk15() throws Exception
    {
        assertQueryMatch( ArtifactKeys.CLASSIFIER, new String[] { "testng" }, "jdk15" );
    }

    public void testMatchClassifierInvalid() throws Exception
    {
        assertQueryMatchNoResults( ArtifactKeys.CLASSIFIER, "redo" );
    }

    public void testMatchClassSessionListener() throws Exception
    {
        assertQueryMatch( BytecodeKeys.CLASSES, new String[] { "wagon-provider-api" }, "wagon.events.SessionListener" );
    }

    /* TODO: Suffix searching does not seem to work.
    public void testMatchClassUtil() throws Exception
    {
        assertQueryMatch( BytecodeKeys.CLASSES, new String[] { "archiva-common", "continuum-webapp", "testng",
            "wagon-provider-api" }, "Util" );
    }
    */

    public void testMatchClassWagon() throws Exception
    {
        assertQueryMatch( BytecodeKeys.CLASSES, new String[] { "wagon-provider-api" }, "Wagon" );
    }

    /* TODO: Suffix searching does not seem to work.
    public void testMatchClassMojoAllUpper() throws Exception
    {
        assertQueryMatch( BytecodeKeys.CLASSES, new String[] { "maven-help-plugin" }, "MOJO" );
    }
    */

    /* TODO: Suffix searching does not seem to work.
    public void testMatchClassMojo() throws Exception
    {
        assertQueryMatch( BytecodeKeys.CLASSES, new String[] { "maven-help-plugin" }, "Mojo" );
    }
    */

    public void testMatchClassInvalid() throws Exception
    {
        assertQueryMatchNoResults( BytecodeKeys.CLASSES, "Destruct|Button" );
    }

    public void testMatchFilesManifestMf() throws Exception
    {
        assertQueryMatch( BytecodeKeys.FILES, new String[] { "daytrader-ear", "maven-archetype-simple",
            "redback-authorization-open", "maven-help-plugin", "archiva-common", "wagon-provider-api",
            "continuum-webapp", "testng" }, "MANIFEST.MF" );
    }

    public void testMatchFilesMetaInf() throws Exception
    {
        assertQueryMatch( BytecodeKeys.FILES, new String[] { "daytrader-ear", "maven-archetype-simple",
            "redback-authorization-open", "maven-help-plugin", "archiva-common", "wagon-provider-api",
            "continuum-webapp", "testng" }, "META-INF" );
    }

    public void testMatchFilesPluginXml() throws Exception
    {
        assertQueryMatch( BytecodeKeys.FILES, new String[] { "maven-help-plugin" }, "plugin.xml" );
    }

    public void testMatchFilesInvalid() throws Exception
    {
        assertQueryMatchNoResults( BytecodeKeys.FILES, "Veni Vidi Castratavi Illegitimos" );
    }

}
