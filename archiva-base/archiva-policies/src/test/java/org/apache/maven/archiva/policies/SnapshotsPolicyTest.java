package org.apache.maven.archiva.policies;

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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Properties;

/**
 * SnapshotsPolicyTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class SnapshotsPolicyTest
    extends PlexusTestCase
{
    private static final String PATH_VERSION_METADATA = "org/apache/archiva/archiva-testable/1.0-SNAPSHOT/maven-metadata.xml";

    private static final String PATH_PROJECT_METADATA = "org/apache/archiva/archiva-testable/maven-metadata.xml";

    private static final String PATH_SNAPSHOT_ARTIFACT = "org/apache/archiva/archiva-testable/1.0-SNAPSHOT/archiva-testable-1.0-SNAPSHOT.jar";

    private static final String PATH_RELEASE_ARTIFACT = "org/apache/archiva/archiva-testable/2.0/archiva-testable-2.0.jar";

    private static final boolean WITH_LOCAL = true;

    private static final boolean NO_LOCAL = false;

    protected static final long ONE_SECOND = ( 1000 /* milliseconds */);

    protected static final long ONE_MINUTE = ( ONE_SECOND * 60 );

    protected static final long ONE_HOUR = ( ONE_MINUTE * 60 );

    protected static final long ONE_DAY = ( ONE_HOUR * 24 );

    protected static final long OVER_ONE_HOUR = ( ONE_HOUR + ONE_MINUTE );

    protected static final long OVER_ONE_DAY = ( ONE_DAY + ONE_HOUR );

    protected static final long OLDER = ( -1 );

    protected static final long NEWER = 0;

    private long generatedLocalFileUpdateDelta = 0;

    public void testSnapshotPolicyDailyProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyDailyReleaseArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyDailySnapshotArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicyViolation( SnapshotsPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicyViolation( SnapshotsPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyDailyVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DAILY, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyDisabledProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.DISABLED, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DISABLED, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyDisabledReleaseArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.DISABLED, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DISABLED, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyDisabledSnapshotArtifact()
        throws Exception
    {
        assertSnapshotPolicyViolation( SnapshotsPolicy.DISABLED, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicyViolation( SnapshotsPolicy.DISABLED, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyDisabledVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.DISABLED, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.DISABLED, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyHourlyProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyHourlyReleaseArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyHourlySnapshotArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicyViolation( SnapshotsPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicyViolation( SnapshotsPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyHourlyVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.HOURLY, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyIgnoredProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyIgnoredReleaseArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyIgnoredSnapshotArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyIgnoredVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.IGNORED, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyOnceProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.ONCE, PATH_PROJECT_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.ONCE, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testSnapshotPolicyOnceReleaseArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.ONCE, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.ONCE, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyOnceSnapshotArtifact()
        throws Exception
    {
        assertSnapshotPolicy( SnapshotsPolicy.ONCE, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertSnapshotPolicyViolation( SnapshotsPolicy.ONCE, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testSnapshotPolicyOnceVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertSnapshotPolicy( SnapshotsPolicy.ONCE, PATH_VERSION_METADATA, NO_LOCAL );
        assertSnapshotPolicy( SnapshotsPolicy.ONCE, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    private void assertSnapshotPolicy( String setting, String path, boolean createLocalFile )
        throws Exception
    {
        PreDownloadPolicy policy = lookupPolicy();
        Properties request = new Properties();
        request.setProperty( "filetype", path.endsWith( "/maven-metadata.xml" ) ? "metadata" : "artifact" );

        if ( path.contains( "1.0-SNAPSHOT" ) )
        {
            request.setProperty( "version", "1.0-SNAPSHOT" );
        }

        if ( path.contains( "2.0" ) )
        {
            request.setProperty( "version", "2.0" );
        }

        File targetDir = getTestFile( "target/test-policy/" );
        File localFile = new File( targetDir, path );

        if ( localFile.exists() )
        {
            localFile.delete();
        }

        if ( createLocalFile )
        {
            localFile.getParentFile().mkdirs();
            FileUtils.fileWrite( localFile.getAbsolutePath(), "random-junk" );
            localFile.setLastModified( localFile.lastModified() - generatedLocalFileUpdateDelta );
        }

        policy.applyPolicy( setting, request, localFile );
    }

    private void assertSnapshotPolicyViolation( String setting, String path, boolean createLocalFile )
        throws Exception
    {
        try
        {
            assertSnapshotPolicy( setting, path, createLocalFile );
            fail( "Expected a PolicyViolationException." );
        }
        catch ( PolicyViolationException e )
        {
            // expected path.
        }
    }

    private PreDownloadPolicy lookupPolicy()
        throws Exception
    {
        PreDownloadPolicy policy = (PreDownloadPolicy) lookup( PreDownloadPolicy.class.getName(), "snapshots" );
        assertNotNull( policy );
        return policy;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        // reset delta to 0.
        generatedLocalFileUpdateDelta = 0;
    }
}
