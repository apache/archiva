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

@Test( groups = { "appearance" }, dependsOnMethods = { "testWithCorrectUsernamePassword" }, sequential = true )
public class AppearanceTest
    extends AbstractArchivaTest
{
    public void testAddAppearanceNullValues()
    {
        goToAppearancePage();
        clickLinkWithText( "Edit" );
        addEditAppearance( "", "", "" );
        assertTextPresent( "You must enter a name" );
    }

    @Test( dependsOnMethods = { "testAddAppearanceNullValues" } )
    public void testAddAppearanceValidValues()
    {
        addEditAppearance( "The Apache Software Foundation", "http://www.apache.org/",
                           "http://www.apache.org/images/asf_logo_wide.gifs" );
        assertTextPresent( "The Apache Software Foundation" );
    }

    @Test( dependsOnMethods = { "testAddAppearanceValidValues" } )
    public void testEditAppearance()
    {
        clickLinkWithText( "Edit" );
        addEditAppearance( "Apache Software Foundation", "http://www.apache.org/",
                           "http://www.apache.org/images/asf_logo_wide.gifs" );
        assertTextPresent( "Apache Software Foundation" );
    }

}