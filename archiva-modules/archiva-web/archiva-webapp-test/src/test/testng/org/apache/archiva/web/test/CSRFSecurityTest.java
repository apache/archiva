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

/**
 * Test all actions affected with CSRF security issue.
 */
@Test( groups = { "csrf" }, dependsOnMethods = { "testWithCorrectUsernamePassword" }, sequential = true )
public class CSRFSecurityTest
    extends AbstractArchivaTest
{
    public void testCSRFDeleteRepository()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/deleteRepository.action?repoid=test&method%3AdeleteContents=Delete+Configuration+and+Contents" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testCSRFDeleteArtifact()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/deleteArtifact!doDelete.action?groupId=1&artifactId=1&version=1&repositoryId=snapshots" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testCSRFAddRepositoryGroup()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/addRepositoryGroup.action?repositoryGroup.id=csrfgrp" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );    
    }

    public void testCSRFDeleteRepositoryGroup()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/deleteRepositoryGroup.action?repoGroupId=test&method%3Adelete=Confirm" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testCSRFDisableProxyConnector()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/disableProxyConnector!disable.action?target=maven2-repository.dev.java.net&source=internal" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testCSRFDeleteProxyConnector()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/deleteProxyConnector!delete.action?target=maven2-repository.dev.java.net&source=snapshots" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testCSRFDeleteLegacyArtifactPath()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/deleteLegacyArtifactPath.action?path=jaxen%2Fjars%2Fjaxen-1.0-FCS-full.jar" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );      
    }

    public void testCSRFSaveNetworkProxy()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/saveNetworkProxy.action?mode=add&proxy.id=ntwrk&proxy.protocol=http&" +
            "proxy.host=test&proxy.port=8080&proxy.username=&proxy.password=" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testCSRFDeleteNetworkProxy()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/deleteNetworkProxy!delete.action?proxyid=myproxy" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );    
    }

    public void testCSRFAddFileTypePattern()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/repositoryScanning!addFiletypePattern.action?pattern=**%2F*.rum&fileTypeId=artifacts" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }

    public void testCSRFRemoveFileTypePattern()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/repositoryScanning!removeFiletypePattern.action?pattern=**%2F*.rum&fileTypeId=artifacts" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );    
    }

    public void testCSRFUpdateKnownConsumers()
    {
        getSelenium().open( baseUrl );
        getSelenium().open( baseUrl + "/admin/repositoryScanning!updateKnownConsumers.action?enabledKnownContentConsumers=auto-remove&" +
            "enabledKnownContentConsumers=auto-rename&enabledKnownContentConsumers=create-missing-checksums&" +
            "enabledKnownContentConsumers=index-content&enabledKnownContentConsumers=metadata-updater&" +
            "enabledKnownContentConsumers=repository-purge&enabledKnownContentConsumers=update-db-artifact&" +
            "enabledKnownContentConsumers=validate-checksums" );
        assertTextPresent( "Security Alert - Invalid Token Found" );
        assertTextPresent( "Possible CSRF attack detected! Invalid token found in the request." );
    }
}
