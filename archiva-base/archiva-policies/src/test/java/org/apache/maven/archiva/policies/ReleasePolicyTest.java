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

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Properties;

/**
 * ReleasePolicyTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ReleasePolicyTest
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

    public void testReleasePolicyDailyProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyDailyReleaseArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicyViolation( ReleasesPolicy.DAILY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicyViolation( ReleasesPolicy.DAILY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyDailySnapshotArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyDailyVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_DAY;
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_HOUR * 22 );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.DAILY, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyRejectProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.NEVER, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.NEVER, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyRejectReleaseArtifact()
        throws Exception
    {
        assertReleasesPolicyViolation( ReleasesPolicy.NEVER, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicyViolation( ReleasesPolicy.NEVER, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyRejectSnapshotArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.NEVER, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.NEVER, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyRejectVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.NEVER, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.NEVER, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyHourlyProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_PROJECT_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyHourlyReleaseArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicyViolation( ReleasesPolicy.HOURLY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicyViolation( ReleasesPolicy.HOURLY, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyHourlySnapshotArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyHourlyVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = OVER_ONE_HOUR;
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_VERSION_METADATA, WITH_LOCAL );

        generatedLocalFileUpdateDelta = ( ONE_MINUTE * 45 );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.HOURLY, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyAlwaysProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyAlwaysReleaseArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyAlwaysSnapshotArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyAlwaysVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.ALWAYS, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyOnceProjectMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.ONCE, PATH_PROJECT_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.ONCE, PATH_PROJECT_METADATA, WITH_LOCAL );
    }

    public void testReleasePolicyOnceReleaseArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.ONCE, PATH_RELEASE_ARTIFACT, NO_LOCAL );
        assertReleasesPolicyViolation( ReleasesPolicy.ONCE, PATH_RELEASE_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyOnceSnapshotArtifact()
        throws Exception
    {
        assertReleasesPolicy( ReleasesPolicy.ONCE, PATH_SNAPSHOT_ARTIFACT, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.ONCE, PATH_SNAPSHOT_ARTIFACT, WITH_LOCAL );
    }

    public void testReleasePolicyOnceVersionedMetadata()
        throws Exception
    {
        // Pass the policy when working with metadata, no matter what.
        assertReleasesPolicy( ReleasesPolicy.ONCE, PATH_VERSION_METADATA, NO_LOCAL );
        assertReleasesPolicy( ReleasesPolicy.ONCE, PATH_VERSION_METADATA, WITH_LOCAL );
    }

    private void assertReleasesPolicy( String setting, String path, boolean createLocalFile )
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
            FileUtils.writeStringToFile( localFile, "random-junk" );
            localFile.setLastModified( localFile.lastModified() - generatedLocalFileUpdateDelta );
        }

        policy.applyPolicy( setting, request, localFile );
    }

    private void assertReleasesPolicyViolation( String setting, String path, boolean createLocalFile )
        throws Exception
    {
        try
        {
            assertReleasesPolicy( setting, path, createLocalFile );
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
        PreDownloadPolicy policy = (PreDownloadPolicy) lookup( PreDownloadPolicy.class.getName(), "releases" );
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
