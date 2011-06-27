package org.apache.archiva.web.test;

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

import org.apache.archiva.web.test.parent.AbstractArchivaTest;
import org.testng.annotations.Test;

@Test( groups = { "findartifact" }, dependsOnGroups = {"about"}, sequential = true )
public class FindArtifactTest
    extends AbstractArchivaTest
{
    public void testFindArtifactNullValues()
    {
        goToFindArtifactPage();
        clickButtonWithValue( "Search" );
        assertTextPresent( "You must select a file, or enter the checksum. If the file was given and you receive this message, there may have been an error generating the checksum." );
    }

    public void testFindArtifactUsingChecksum()
    {
        goToFindArtifactPage();
        setFieldValue( "checksumSearch_q", "8e896baea663a45d7bd2737f8e464481" );
        clickButtonWithValue( "Search" );
        assertTextPresent( "No results found" );
    }

    // TODO: test using file upload on Firefox versions that support getAsBinary (ie, no applet)
}