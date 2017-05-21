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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.web.test.parent.AbstractArchivaTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Based on LoginTest of Emmanuel Venisse test.
 *
 * @author skygo
 *
 */


public class RepositoryAdminTest
    extends AbstractArchivaTest
{

    @Test
    public void testManagedRepository()
    {
        login( getAdminUsername(), getAdminPassword() );
        clickLinkWithLocator( "menu-repositories-list-a", true );
        
        // add custom repo
        assertTextPresent( "Repositories Administration " );
        clickLinkWithXPath( "//a[@href='#remote-repositories-content']", true );
        
        clickLinkWithXPath( "//a[@href='#remote-repository-edit']", true );
        
        setFieldValue( "id", "myrepoid" );        
        setFieldValue( "name", "My repo name" );        
        setFieldValue( "url", "http://www.repo.org" );
        
        clickButtonWithLocator( "remote-repository-save-button", true );
       
        clickLinkWithLocator( "menu-proxy-connectors-list-a", true );
        
        // proxy connect
        assertTextPresent( "Proxy Connectors" );
        assertTextPresent( "central" );
        assertTextNotPresent( "myrepoid" );
        clickButtonWithLocator( "proxy-connectors-view-tabs-a-edit", true );
        getSelenium().select( "sourceRepoId", "internal" );
        getSelenium().select( "targetRepoId", "myrepoid" );
        clickButtonWithLocator( "proxy-connector-btn-save", true);
        assertTextPresent( "central" );
        assertTextPresent( "myrepoid" );
        clickLinkWithXPath( "//i[contains(concat(' ',normalize-space(@class),' '),' icon-resize-vertical ')]/../..", true );
        assertTextPresent( "internal" );
        // order test
        Assert.assertTrue( "First repo is myrepo",getSelenium().getText("xpath=//div[@id='proxy-connector-edit-order-div']/div[1]" ).contains( "myrepoid" ));
        Assert.assertTrue( "Second repo is central",getSelenium().getText("xpath=//div[@id='proxy-connector-edit-order-div']/div[2]" ).contains( "central" ));
             
        // works until this point
        /*getSelenium().mouseDown( "xpath=//div[@id='proxy-connector-edit-order-div']/div[1]" );
        getSelenium().mouseMove( "xpath=//div[@id='proxy-connector-edit-order-div']/div[2]" );
        getSelenium().mouseUp( "xpath=//div[@id='proxy-connector-edit-order-div']/div[last()]" );
        Assert.assertTrue( "Second repo is myrepo", getSelenium().getText("xpath=//div[@id='proxy-connector-edit-order-div']/div[2]" ).contains( "myrepoid" ));
        Assert.assertTrue( "First repo is central", getSelenium().getText("xpath=//div[@id='proxy-connector-edit-order-div']/div[1]" ).contains( "central" ));
        */
    }
    
}