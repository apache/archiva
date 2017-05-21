package org.apache.archiva.repository.metadata;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.policies.CachedFailuresPolicy;
import org.apache.archiva.policies.ChecksumPolicy;
import org.apache.archiva.policies.ReleasesPolicy;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.apache.archiva.repository.AbstractRepositoryLayerTestCase;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.metadata.repository.storage.maven2.conf.MockConfiguration;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * MetadataToolsTest
 */
@ContextConfiguration (
    { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-metadata-tools-test.xml" } )
public class MetadataToolsTest
    extends AbstractRepositoryLayerTestCase
{
    @Inject
    @Named ( "metadataTools#test" )
    private MetadataTools tools;

    @Inject
    @Named ( "archivaConfiguration#mock" )
    protected MockConfiguration config;

    @Test
    public void testGatherSnapshotVersionsA()
        throws Exception
    {
        removeProxyConnector( "test-repo", "apache-snapshots" );
        removeProxyConnector( "test-repo", "internal-snapshots" );
        removeProxyConnector( "test-repo", "snapshots.codehaus.org" );

        assertSnapshotVersions( "snap_shots_a", "1.0-alpha-11-SNAPSHOT",
                                new String[]{ "1.0-alpha-11-SNAPSHOT", "1.0-alpha-11-20070221.194724-2",
                                    "1.0-alpha-11-20070302.212723-3", "1.0-alpha-11-20070303.152828-4",
                                    "1.0-alpha-11-20070305.215149-5", "1.0-alpha-11-20070307.170909-6",
                                    "1.0-alpha-11-20070314.211405-9", "1.0-alpha-11-20070316.175232-11" } );
    }

    @Test
    public void testGatherSnapshotVersionsAWithProxies()
        throws Exception
    {
        // These proxied repositories do not need to exist for the purposes of this unit test,
        // just the repository ids are important.
        createProxyConnector( "test-repo", "apache-snapshots" );
        createProxyConnector( "test-repo", "internal-snapshots" );
        createProxyConnector( "test-repo", "snapshots.codehaus.org" );

        assertSnapshotVersions( "snap_shots_a", "1.0-alpha-11-SNAPSHOT",
                                new String[]{ "1.0-alpha-11-SNAPSHOT", "1.0-alpha-11-20070221.194724-2",
                                    "1.0-alpha-11-20070302.212723-3", "1.0-alpha-11-20070303.152828-4",
                                    "1.0-alpha-11-20070305.215149-5", "1.0-alpha-11-20070307.170909-6",
                                    "1.0-alpha-11-20070314.211405-9", "1.0-alpha-11-20070315.033030-10"
                                    /* Arrives in via snapshots.codehaus.org proxy */,
                                    "1.0-alpha-11-20070316.175232-11" } );
    }

    @Test
    public void testGetRepositorySpecificName()
        throws Exception
    {
        RemoteRepositoryContent repoJavaNet =
            createRemoteRepositoryContent( "maven2-repository.dev.java.net", "Java.net Repository for Maven 2",
                                           "http://download.java.net/maven/2/", "default" );
        RemoteRepositoryContent repoCentral =
            createRemoteRepositoryContent( "central", "Central Global Repository", "http://repo1.maven.org/maven2/",
                                           "default" );

        String convertedName =
            tools.getRepositorySpecificName( repoJavaNet, "commons-lang/commons-lang/maven-metadata.xml" );
        assertMetadataPath( "commons-lang/commons-lang/maven-metadata-maven2-repository.dev.java.net.xml",
                            convertedName );

        convertedName = tools.getRepositorySpecificName( repoCentral, "commons-lang/commons-lang/maven-metadata.xml" );
        assertMetadataPath( "commons-lang/commons-lang/maven-metadata-central.xml", convertedName );
    }

    // TODO: replace with group tests
//    public void testUpdateProjectBadArtifact()
//        throws Exception
//    {
//        try
//        {
//            assertUpdatedProjectMetadata( "bad_artifact", null );
//            fail( "Should have thrown an IOException on a bad artifact." );
//        }
//        catch ( IOException e )
//        {
//            // Expected path
//        }
//    }

    @Test
    public void testUpdateProjectNonExistingVersion()
        throws Exception
    {
        ManagedRepositoryContent testRepo = createTestRepoContent();
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( "missing_artifact" );

        prepTestRepo( testRepo, reference );

        // check metadata prior to update -- should contain the non-existing artifact version
        assertProjectMetadata( testRepo, reference, "missing_artifact",
                               new String[]{ "1.0-SNAPSHOT", "1.1-SNAPSHOT", "1.2-SNAPSHOT" }, "1.2-SNAPSHOT", null );

        tools.updateMetadata( testRepo, reference );

        // metadata should not contain the non-existing artifact version -- 1.1-SNAPSHOT
        assertProjectMetadata( testRepo, reference, "missing_artifact", new String[]{ "1.0-SNAPSHOT", "1.2-SNAPSHOT" },
                               "1.2-SNAPSHOT", null );
    }

    @Test
    public void testUpdateProjectMissingMultipleVersions()
        throws Exception
    {
        assertUpdatedProjectMetadata( "missing_metadata_b",
                                      new String[]{ "1.0", "1.0.1", "2.0", "2.0.1", "2.0-20070821-dev" },
                                      "2.0-20070821-dev", "2.0-20070821-dev" );
    }

    @Test
    public void testUpdateProjectMissingMultipleVersionsWithProxies()
        throws Exception
    {
        // Attach the (bogus) proxies to the managed repo.
        // These proxied repositories do not need to exist for the purposes of this unit test,
        // just the repository ids are important.
        createProxyConnector( "test-repo", "central" );
        createProxyConnector( "test-repo", "java.net" );

        assertUpdatedProjectMetadata( "proxied_multi",
                                      new String[]{ "1.0-spec" /* in java.net */, "1.0" /* in managed, and central */,
                                          "1.0.1" /* in central */, "1.1" /* in managed */, "2.0-proposal-beta"
                                          /* in java.net */, "2.0-spec" /* in java.net */, "2.0"
                                          /* in central, and java.net */, "2.0.1" /* in java.net */, "2.1"
                                          /* in managed */, "3.0" /* in central */, "3.1" /* in central */ }, "3.1",
                                      "3.1" );
    }

    @Test
    public void testUpdateProjectSimpleYetIncomplete()
        throws Exception
    {
        assertUpdatedProjectMetadata( "incomplete_metadata_a", new String[]{ "1.0" }, "1.0", "1.0" );
    }

    @Test
    public void testUpdateProjectSimpleYetMissing()
        throws Exception
    {
        assertUpdatedProjectMetadata( "missing_metadata_a", new String[]{ "1.0" }, "1.0", "1.0" );
    }

    @Test
    public void testUpdateVersionSimple10()
        throws Exception
    {
        assertUpdatedReleaseVersionMetadata( "missing_metadata_a", "1.0" );
    }

    @Test
    public void testUpdateVersionSimple20()
        throws Exception
    {
        assertUpdatedReleaseVersionMetadata( "missing_metadata_b", "2.0" );
    }

    @Test
    public void testUpdateVersionSimple20NotSnapshot()
        throws Exception
    {
        assertUpdatedReleaseVersionMetadata( "missing_metadata_b", "2.0-20070821-dev" );
    }

    @Test
    public void testUpdateVersionSnapshotA()
        throws Exception
    {
        assertUpdatedSnapshotVersionMetadata( "snap_shots_a", "1.0-alpha-11-SNAPSHOT", "20070316", "175232", "11" );
    }

    @Test
    public void testToPathFromVersionReference()
    {
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "com.foo" );
        reference.setArtifactId( "foo-tool" );
        reference.setVersion( "1.0" );

        assertEquals( "com/foo/foo-tool/1.0/maven-metadata.xml", tools.toPath( reference ) );
    }

    @Test
    public void testToPathFromProjectReference()
    {
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( "com.foo" );
        reference.setArtifactId( "foo-tool" );

        assertEquals( "com/foo/foo-tool/maven-metadata.xml", tools.toPath( reference ) );
    }

    @Test
    public void testToProjectReferenceFooTools()
        throws RepositoryMetadataException
    {
        assertProjectReference( "com.foo", "foo-tools", "com/foo/foo-tools/maven-metadata.xml" );
    }

    @Test
    public void testToProjectReferenceAReallyLongPath()
        throws RepositoryMetadataException
    {
        String groupId = "net.i.have.a.really.long.path.just.for.the.hell.of.it";
        String artifactId = "a";
        String path = "net/i/have/a/really/long/path/just/for/the/hell/of/it/a/maven-metadata.xml";

        assertProjectReference( groupId, artifactId, path );
    }

    @Test
    public void testToProjectReferenceCommonsLang()
        throws RepositoryMetadataException
    {
        String groupId = "commons-lang";
        String artifactId = "commons-lang";
        String path = "commons-lang/commons-lang/maven-metadata.xml";

        assertProjectReference( groupId, artifactId, path );
    }

    private void assertProjectReference( String groupId, String artifactId, String path )
        throws RepositoryMetadataException
    {
        ProjectReference reference = tools.toProjectReference( path );

        assertNotNull( "Reference should not be null.", reference );
        assertEquals( "ProjectReference.groupId", groupId, reference.getGroupId() );
        assertEquals( "ProjectReference.artifactId", artifactId, reference.getArtifactId() );
    }

    @Test
    public void testToVersionedReferenceFooTool()
        throws RepositoryMetadataException
    {
        String groupId = "com.foo";
        String artifactId = "foo-tool";
        String version = "1.0";
        String path = "com/foo/foo-tool/1.0/maven-metadata.xml";

        assertVersionedReference( groupId, artifactId, version, path );
    }

    @Test
    public void testToVersionedReferenceAReallyLongPath()
        throws RepositoryMetadataException
    {
        String groupId = "net.i.have.a.really.long.path.just.for.the.hell.of.it";
        String artifactId = "a";
        String version = "1.1-alpha-1";
        String path = "net/i/have/a/really/long/path/just/for/the/hell/of/it/a/1.1-alpha-1/maven-metadata.xml";

        assertVersionedReference( groupId, artifactId, version, path );
    }

    @Test
    public void testToVersionedReferenceCommonsLang()
        throws RepositoryMetadataException
    {
        String groupId = "commons-lang";
        String artifactId = "commons-lang";
        String version = "2.1";
        String path = "commons-lang/commons-lang/2.1/maven-metadata.xml";

        assertVersionedReference( groupId, artifactId, version, path );
    }

    @Test
    public void testToVersionedReferenceSnapshot()
        throws RepositoryMetadataException
    {
        String groupId = "com.foo";
        String artifactId = "foo-connector";
        String version = "2.1-SNAPSHOT";
        String path = "com/foo/foo-connector/2.1-SNAPSHOT/maven-metadata.xml";

        assertVersionedReference( groupId, artifactId, version, path );
    }

    private void assertVersionedReference( String groupId, String artifactId, String version, String path )
        throws RepositoryMetadataException
    {
        VersionedReference reference = tools.toVersionedReference( path );
        assertNotNull( "Reference should not be null.", reference );

        assertEquals( "VersionedReference.groupId", groupId, reference.getGroupId() );
        assertEquals( "VersionedReference.artifactId", artifactId, reference.getArtifactId() );
        assertEquals( "VersionedReference.version", version, reference.getVersion() );
    }

    private void assertSnapshotVersions( String artifactId, String version, String[] expectedVersions )
        throws Exception
    {
        File repoRootDir = new File( "src/test/repositories/metadata-repository" );

        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );
        reference.setVersion( version );

        ManagedRepository repo =
            createRepository( "test-repo", "Test Repository: " + name.getMethodName(), repoRootDir );
        ManagedRepositoryContent repoContent =
            applicationContext.getBean( "managedRepositoryContent#default", ManagedRepositoryContent.class );
        repoContent.setRepository( repo );

        Set<String> testedVersionSet = tools.gatherSnapshotVersions( repoContent, reference );

        // Sort the list (for asserts)
        List<String> testedVersions = new ArrayList<>();
        testedVersions.addAll( testedVersionSet );
        Collections.sort( testedVersions, new VersionComparator() );

        // Test the expected array of versions, to the actual tested versions
        assertEquals( "Assert Snapshot Versions: length/size", expectedVersions.length, testedVersions.size() );

        for ( int i = 0; i < expectedVersions.length; i++ )
        {
            String actualVersion = testedVersions.get( i );
            assertEquals( "Snapshot Versions[" + i + "]", expectedVersions[i], actualVersion );
        }
    }

    private void assertMetadata( String expectedMetadata, ManagedRepositoryContent repository,
                                 ProjectReference reference )
        throws LayoutException, IOException, SAXException, ParserConfigurationException
    {
        File metadataFile = new File( repository.getRepoRoot(), tools.toPath( reference ) );
        String actualMetadata = FileUtils.readFileToString( metadataFile, Charset.defaultCharset() );

        DetailedDiff detailedDiff = new DetailedDiff( new Diff( expectedMetadata, actualMetadata ) );
        if ( !detailedDiff.similar() )
        {
            // If it isn't similar, dump the difference.
            assertEquals( expectedMetadata, actualMetadata );
        }
    }

    private void assertMetadata( String expectedMetadata, ManagedRepositoryContent repository,
                                 VersionedReference reference )
        throws LayoutException, IOException, SAXException, ParserConfigurationException
    {
        File metadataFile = new File( repository.getRepoRoot(), tools.toPath( reference ) );
        String actualMetadata = FileUtils.readFileToString( metadataFile, Charset.defaultCharset() );

        DetailedDiff detailedDiff = new DetailedDiff( new Diff( expectedMetadata, actualMetadata ) );
        if ( !detailedDiff.similar() )
        {
            // If it isn't similar, dump the difference.
            assertEquals( expectedMetadata, actualMetadata );
        }
    }

    private void assertMetadataPath( String expected, String actual )
    {
        assertEquals( "Repository Specific Metadata Path", expected, actual );
    }

    private void assertUpdatedProjectMetadata( String artifactId, String[] expectedVersions, String latestVersion,
                                               String releaseVersion )
        throws Exception
    {
        ManagedRepositoryContent testRepo = createTestRepoContent();
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );

        prepTestRepo( testRepo, reference );

        tools.updateMetadata( testRepo, reference );

        StringBuilder buf = new StringBuilder();
        buf.append( "<metadata>\n" );
        buf.append( "  <groupId>" ).append( reference.getGroupId() ).append( "</groupId>\n" );
        buf.append( "  <artifactId>" ).append( reference.getArtifactId() ).append( "</artifactId>\n" );
        // buf.append( "  <version>1.0</version>\n" );

        if ( expectedVersions != null )
        {
            buf.append( "  <versioning>\n" );
            if ( latestVersion != null )
            {
                buf.append( "    <latest>" ).append( latestVersion ).append( "</latest>\n" );
            }
            if ( releaseVersion != null )
            {
                buf.append( "    <release>" ).append( releaseVersion ).append( "</release>\n" );
            }

            buf.append( "    <versions>\n" );
            for ( int i = 0; i < expectedVersions.length; i++ )
            {
                buf.append( "      <version>" ).append( expectedVersions[i] ).append( "</version>\n" );
            }
            buf.append( "    </versions>\n" );
            buf.append( "  </versioning>\n" );
        }
        buf.append( "</metadata>" );

        assertMetadata( buf.toString(), testRepo, reference );
    }

    private void assertProjectMetadata( ManagedRepositoryContent testRepo, ProjectReference reference,
                                        String artifactId, String[] expectedVersions, String latestVersion,
                                        String releaseVersion )
        throws Exception
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "<metadata>\n" );
        buf.append( "  <groupId>" ).append( reference.getGroupId() ).append( "</groupId>\n" );
        buf.append( "  <artifactId>" ).append( reference.getArtifactId() ).append( "</artifactId>\n" );

        if ( expectedVersions != null )
        {
            buf.append( "  <versioning>\n" );
            if ( latestVersion != null )
            {
                buf.append( "    <latest>" ).append( latestVersion ).append( "</latest>\n" );
            }
            if ( releaseVersion != null )
            {
                buf.append( "    <release>" ).append( releaseVersion ).append( "</release>\n" );
            }

            buf.append( "    <versions>\n" );
            for ( int i = 0; i < expectedVersions.length; i++ )
            {
                buf.append( "      <version>" ).append( expectedVersions[i] ).append( "</version>\n" );
            }
            buf.append( "    </versions>\n" );
            buf.append( "  </versioning>\n" );
        }
        buf.append( "</metadata>" );

        assertMetadata( buf.toString(), testRepo, reference );
    }

    private void assertUpdatedReleaseVersionMetadata( String artifactId, String version )
        throws Exception
    {
        ManagedRepositoryContent testRepo = createTestRepoContent();
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );
        reference.setVersion( version );

        prepTestRepo( testRepo, reference );

        tools.updateMetadata( testRepo, reference );

        StringBuilder buf = new StringBuilder();
        buf.append( "<metadata>\n" );
        buf.append( "  <groupId>" ).append( reference.getGroupId() ).append( "</groupId>\n" );
        buf.append( "  <artifactId>" ).append( reference.getArtifactId() ).append( "</artifactId>\n" );
        buf.append( "  <version>" ).append( reference.getVersion() ).append( "</version>\n" );
        buf.append( "</metadata>" );

        assertMetadata( buf.toString(), testRepo, reference );
    }

    private void assertUpdatedSnapshotVersionMetadata( String artifactId, String version, String expectedDate,
                                                       String expectedTime, String expectedBuildNumber )
        throws Exception
    {
        ManagedRepositoryContent testRepo = createTestRepoContent();
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );
        reference.setVersion( version );

        prepTestRepo( testRepo, reference );

        tools.updateMetadata( testRepo, reference );

        StringBuilder buf = new StringBuilder();
        buf.append( "<metadata>\n" );
        buf.append( "  <groupId>" ).append( reference.getGroupId() ).append( "</groupId>\n" );
        buf.append( "  <artifactId>" ).append( reference.getArtifactId() ).append( "</artifactId>\n" );
        buf.append( "  <version>" ).append( reference.getVersion() ).append( "</version>\n" );
        buf.append( "  <versioning>\n" );
        buf.append( "    <snapshot>\n" );
        buf.append( "      <buildNumber>" ).append( expectedBuildNumber ).append( "</buildNumber>\n" );
        buf.append( "      <timestamp>" );
        buf.append( expectedDate ).append( "." ).append( expectedTime );
        buf.append( "</timestamp>\n" );
        buf.append( "    </snapshot>\n" );
        buf.append( "    <lastUpdated>" ).append( expectedDate ).append( expectedTime ).append( "</lastUpdated>\n" );
        buf.append( "  </versioning>\n" );
        buf.append( "</metadata>" );

        assertMetadata( buf.toString(), testRepo, reference );
    }

    private void removeProxyConnector( String sourceRepoId, String targetRepoId )
    {
        ProxyConnectorConfiguration toRemove = null;
        for ( ProxyConnectorConfiguration pcc : config.getConfiguration().getProxyConnectors() )
        {
            if ( pcc.getTargetRepoId().equals( targetRepoId ) && pcc.getSourceRepoId().equals( sourceRepoId ) )
            {
                toRemove = pcc;
            }
        }
        if ( toRemove != null )
        {
            config.getConfiguration().removeProxyConnector( toRemove );
            String prefix = "proxyConnectors.proxyConnector(" + "1" + ")";  // XXX 
            config.triggerChange( prefix + ".sourceRepoId", toRemove.getSourceRepoId() );
            config.triggerChange( prefix + ".targetRepoId", toRemove.getTargetRepoId() );
            config.triggerChange( prefix + ".proxyId", toRemove.getProxyId() );
            config.triggerChange( prefix + ".policies.releases", toRemove.getPolicy( "releases", "" ) );
            config.triggerChange( prefix + ".policies.checksum", toRemove.getPolicy( "checksum", "" ) );
            config.triggerChange( prefix + ".policies.snapshots", toRemove.getPolicy( "snapshots", "" ) );
            config.triggerChange( prefix + ".policies.cache-failures", toRemove.getPolicy( "cache-failures", "" ) );
        }
    }

    private void createProxyConnector( String sourceRepoId, String targetRepoId )
    {
        ProxyConnectorConfiguration connectorConfig = new ProxyConnectorConfiguration();
        connectorConfig.setSourceRepoId( sourceRepoId );
        connectorConfig.setTargetRepoId( targetRepoId );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CHECKSUM, ChecksumPolicy.IGNORE );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, ReleasesPolicy.ALWAYS );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, SnapshotsPolicy.ALWAYS );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CACHE_FAILURES, CachedFailuresPolicy.NO );

        int count = config.getConfiguration().getProxyConnectors().size();
        config.getConfiguration().addProxyConnector( connectorConfig );

        // Proper Triggering ...
        String prefix = "proxyConnectors.proxyConnector(" + count + ")";
        config.triggerChange( prefix + ".sourceRepoId", connectorConfig.getSourceRepoId() );
        config.triggerChange( prefix + ".targetRepoId", connectorConfig.getTargetRepoId() );
        config.triggerChange( prefix + ".proxyId", connectorConfig.getProxyId() );
        config.triggerChange( prefix + ".policies.releases", connectorConfig.getPolicy( "releases", "" ) );
        config.triggerChange( prefix + ".policies.checksum", connectorConfig.getPolicy( "checksum", "" ) );
        config.triggerChange( prefix + ".policies.snapshots", connectorConfig.getPolicy( "snapshots", "" ) );
        config.triggerChange( prefix + ".policies.cache-failures", connectorConfig.getPolicy( "cache-failures", "" ) );
    }

    private ManagedRepositoryContent createTestRepoContent()
        throws Exception
    {
        File repoRoot = new File( "target/metadata-tests/" + name.getMethodName() );
        if ( repoRoot.exists() )
        {
            FileUtils.deleteDirectory( repoRoot );
        }

        repoRoot.mkdirs();

        ManagedRepository repoConfig =
            createRepository( "test-repo", "Test Repository: " + name.getMethodName(), repoRoot );

        ManagedRepositoryContent repoContent =
            applicationContext.getBean( "managedRepositoryContent#default", ManagedRepositoryContent.class );
        repoContent.setRepository( repoConfig );
        return repoContent;
    }

    private void prepTestRepo( ManagedRepositoryContent repo, ProjectReference reference )
        throws IOException
    {
        String groupDir = StringUtils.replaceChars( reference.getGroupId(), '.', '/' );
        String path = groupDir + "/" + reference.getArtifactId();

        File srcRepoDir = new File( "src/test/repositories/metadata-repository" );
        File srcDir = new File( srcRepoDir, path );
        File destDir = new File( repo.getRepoRoot(), path );

        assertTrue( "Source Dir exists: " + srcDir, srcDir.exists() );
        destDir.mkdirs();

        FileUtils.copyDirectory( srcDir, destDir );
    }

    private void prepTestRepo( ManagedRepositoryContent repo, VersionedReference reference )
        throws IOException
    {
        ProjectReference projectRef = new ProjectReference();
        projectRef.setGroupId( reference.getGroupId() );
        projectRef.setArtifactId( reference.getArtifactId() );

        prepTestRepo( repo, projectRef );
    }


}
