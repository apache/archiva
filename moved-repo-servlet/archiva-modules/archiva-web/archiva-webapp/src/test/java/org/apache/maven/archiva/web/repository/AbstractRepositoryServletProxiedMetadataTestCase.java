package org.apache.maven.archiva.web.repository;

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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;

/**
 * Abstract TestCase for RepositoryServlet Tests, Proxied, Get of Metadata. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractRepositoryServletProxiedMetadataTestCase
    extends AbstractRepositoryServletProxiedTestCase
{
    protected RemoteRepoInfo remotePrivateSnapshots;

    protected void assertExpectedMetadata( String expectedMetadata, String actualMetadata )
        throws Exception
    {
        DetailedDiff detailedDiff = new DetailedDiff( new Diff( expectedMetadata, actualMetadata ) );
        if ( !detailedDiff.similar() )
        {
            // If it isn't similar, dump the difference.
            assertEquals( expectedMetadata, actualMetadata );
        }
        // XMLAssert.assertXMLEqual( "Expected Metadata:", expectedMetadata, actualMetadata );
    }

    protected String requestMetadataOK( String path )
        throws Exception
    {
        // process the response code later, not via an exception.
        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + path );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );
        return response.getText();
    }

    protected String createVersionMetadata( String groupId, String artifactId, String version )
    {
        return createVersionMetadata( groupId, artifactId, version, null, null, null );
    }

    protected String createVersionMetadata( String groupId, String artifactId, String version, String timestamp,
                                          String buildNumber, String lastUpdated )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" );
        buf.append( "<metadata>\n" );
        buf.append( "  <groupId>" ).append( groupId ).append( "</groupId>\n" );
        buf.append( "  <artifactId>" ).append( artifactId ).append( "</artifactId>\n" );
        buf.append( "  <version>" ).append( version ).append( "</version>\n" );

        boolean hasSnapshot = StringUtils.isNotBlank( timestamp ) || StringUtils.isNotBlank( buildNumber );
        boolean hasLastUpdated = StringUtils.isNotBlank( lastUpdated );

        if ( hasSnapshot || hasLastUpdated )
        {
            buf.append( "  <versioning>\n" );
            if ( hasSnapshot )
            {
                buf.append( "    <snapshot>\n" );
                buf.append( "      <buildNumber>" ).append( buildNumber ).append( "</buildNumber>\n" );
                buf.append( "      <timestamp>" ).append( timestamp ).append( "</timestamp>\n" );
                buf.append( "    </snapshot>\n" );
            }
            if ( hasLastUpdated )
            {
                buf.append( "    <lastUpdated>" ).append( lastUpdated ).append( "</lastUpdated>\n" );
            }
            buf.append( "  </versioning>\n" );
        }
        buf.append( "</metadata>" );

        return buf.toString();
    }

    protected String createProjectMetadata( String groupId, String artifactId, String latest, String release,
                                          String[] versions )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" );
        buf.append( "<metadata>\n" );
        buf.append( "  <groupId>" ).append( groupId ).append( "</groupId>\n" );
        buf.append( "  <artifactId>" ).append( artifactId ).append( "</artifactId>\n" );

        boolean hasLatest = StringUtils.isNotBlank( latest );
        boolean hasRelease = StringUtils.isNotBlank( release );
        boolean hasVersions = !ArrayUtils.isEmpty( versions );

        if ( hasLatest || hasRelease || hasVersions )
        {
            buf.append( "  <versioning>\n" );
            if ( hasLatest )
            {
                buf.append( "    <latest>" ).append( latest ).append( "</latest>\n" );
            }
            if ( hasRelease )
            {
                buf.append( "    <release>" ).append( release ).append( "</release>\n" );
            }
            if ( hasVersions )
            {
                buf.append( "    <versions>\n" );
                for ( String availVersion : versions )
                {
                    buf.append( "      <version>" ).append( availVersion ).append( "</version>\n" );
                }
                buf.append( "    </versions>\n" );
            }
            buf.append( "  </versioning>\n" );
        }
        buf.append( "</metadata>" );

        return buf.toString();
    }

    protected String createGroupMetadata( String groupId, String[] plugins )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" );
        buf.append( "<metadata>\n" );
        buf.append( "  <groupId>" ).append( groupId ).append( "</groupId>\n" );

        boolean hasPlugins = !ArrayUtils.isEmpty( plugins );

        if ( hasPlugins )
        {
            buf.append( "  <plugins>\n" );
            for ( String plugin : plugins )
            {
                buf.append( "    <plugin>\n" );
                buf.append( "      <prefix>" ).append( plugin ).append( "</prefix>\n" );
                buf.append( "      <artifactId>" ).append( plugin + "-maven-plugin" ).append( "</artifactId>\n" );
                buf.append( "      <name>" ).append( "The " + plugin + " Plugin" ).append( "</name>\n" );
                buf.append( "    </plugin>\n" );
            }
            buf.append( "  </plugins>\n" );
        }
        buf.append( "</metadata>" );

        return buf.toString();
    }

    protected void setupPrivateSnapshotsRemoteRepo()
        throws Exception
    {
        remotePrivateSnapshots = createServer( "private-snapshots" );

        assertServerSetupCorrectly( remotePrivateSnapshots );
        archivaConfiguration.getConfiguration().addRemoteRepository( remotePrivateSnapshots.config );
        setupCleanRepo( remotePrivateSnapshots.root );
    }

//    private void assertGetProxiedSnapshotMetadata( int expectation, boolean hasManagedCopy,
//                                                   long deltaManagedToRemoteTimestamp )
//        throws Exception
//    {
//        // --- Setup
//        setupSnapshotsRemoteRepo();
//        setupCleanInternalRepo();
//
//        String resourcePath = "org/apache/archiva/archivatest-maven-plugin/4.0-alpha-1-SNAPSHOT/maven-metadata.xml";
//        String expectedRemoteContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<metadata>\n"
//            + "  <groupId>org.apache.maven.plugins</groupId>\n" + "  <artifactId>maven-assembly-plugin</artifactId>\n"
//            + "  <version>2.2-beta-2-SNAPSHOT</version>\n" + "  <versioning>\n" + "    <snapshot>\n"
//            + "      <timestamp>20071017.162810</timestamp>\n" + "      <buildNumber>20</buildNumber>\n"
//            + "    </snapshot>\n" + "    <lastUpdated>20071017162814</lastUpdated>\n" + "  </versioning>\n"
//            + "</metadata>";
//        String expectedManagedContents = null;
//        File remoteFile = populateRepo( remoteSnapshots, resourcePath, expectedRemoteContents );
//
//        if ( hasManagedCopy )
//        {
//            expectedManagedContents = "<metadata>\n" + "  <groupId>org.apache.maven.plugins</groupId>\n"
//                + "  <artifactId>maven-assembly-plugin</artifactId>\n" + "  <version>2.2-beta-2-SNAPSHOT</version>\n"
//                + "</metadata>";
//
//            File managedFile = populateRepo( repoRootInternal, resourcePath, expectedManagedContents );
//            managedFile.setLastModified( remoteFile.lastModified() + deltaManagedToRemoteTimestamp );
//        }
//
//        setupConnector( REPOID_INTERNAL, remoteSnapshots );
//        saveConfiguration();
//
//        // --- Execution
//        // process the response code later, not via an exception.
//        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );
//
//        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + resourcePath );
//        WebResponse response = sc.getResponse( request );
//
//        // --- Verification
//
//        switch ( expectation )
//        {
//            case EXPECT_MANAGED_CONTENTS:
//                assertResponseOK( response );
//                assertTrue( "Invalid Test Case: Can't expect managed contents with "
//                    + "test that doesn't have a managed copy in the first place.", hasManagedCopy );
//                String actualContents = response.getText();
//                XMLAssert.assertXMLEqual( expectedManagedContents, actualContents );
//                // assertEquals( "Expected managed file contents", expectedManagedContents, response.getText() );
//                break;
//            case EXPECT_REMOTE_CONTENTS:
//                assertResponseOK( response );
//                assertEquals( "Expected remote file contents", expectedRemoteContents, response.getText() );
//                break;
//            case EXPECT_NOT_FOUND:
//                assertResponseNotFound( response );
//                assertManagedFileNotExists( repoRootInternal, resourcePath );
//                break;
//        }
//    }

    protected void tearDown()
        throws Exception
    {
        shutdownServer( remotePrivateSnapshots );

        super.tearDown();
    }
}
