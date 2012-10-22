package org.apache.archiva.common.utils;

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

import junit.framework.TestCase;

public class VersionUtilTest
    extends TestCase
{

    public void testIsVersion()
    {
        // 0%
        assertFalse( VersionUtil.isVersion( "project" ) );

        // 0%
        assertFalse( VersionUtil.isVersion( "project-not-version" ) );

        // 50%
        assertFalse( VersionUtil.isVersion( "project-ver-1.0-dev" ) );

        // > 75%
        assertTrue( VersionUtil.isVersion( "project-1.0-alpha" ) );

        // 75%
        assertTrue( VersionUtil.isVersion( "project-1.0-latest-nightly" ) );

        // >75%
        assertTrue( VersionUtil.isVersion( "1.0-project-unofficial-nightly-alpha-release" ) );

        //only first token matches
        assertTrue( VersionUtil.isVersion( "1.0-project-my-own-version" ) );

    }

    public void testIsSimpleVersionKeyword()
    {
        assertTrue( VersionUtil.isSimpleVersionKeyword( "rc4.34" ) );

        assertTrue( VersionUtil.isSimpleVersionKeyword( "beta" ) );

        assertFalse( VersionUtil.isSimpleVersionKeyword( "1.0-SNAPSHOT" ) );
    }

    public void testIsSnapshot()
    {
        assertTrue( VersionUtil.isSnapshot( "1.0-20070113.163208-99" ) );

        assertTrue( VersionUtil.isSnapshot( "1.0-SNAPSHOT" ) );

        assertFalse( VersionUtil.isSnapshot( "1.0-beta1" ) );
    }

    public void testGetBaseVersion()
    {
        assertEquals( VersionUtil.getBaseVersion( "1.3.2-20090420.083501-3" ), "1.3.2-SNAPSHOT" );
    }

    public void testGetReleaseVersion()
    {
        assertEquals( VersionUtil.getReleaseVersion( "1.3.2-20090420.083501-3" ), "1.3.2" );
    }

    public void testIsUniqueSnapshot()
    {
        assertTrue( VersionUtil.isUniqueSnapshot( "1.3.2-20090420.083501-3" ) );

        assertFalse( VersionUtil.isUniqueSnapshot( "1.3.2" ) );
    }

    public void testIsGenericSnapshot()
    {
        assertFalse( VersionUtil.isGenericSnapshot( "1.3.2-20090420.083501-3" ) );

        assertTrue( VersionUtil.isGenericSnapshot( "1.3.2-SNAPSHOT" ) );
    }

    public void testGetVersionFromGenericSnapshot()
    {
        assertEquals( "3.0", VersionUtil.getVersionFromGenericSnapshot( "3.0-SNAPSHOT" ) );

    }

}
