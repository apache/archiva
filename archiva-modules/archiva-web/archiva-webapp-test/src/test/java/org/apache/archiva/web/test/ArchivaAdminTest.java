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
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ArchivaAdminTest
    extends AbstractArchivaTest
{

    @Test
    public void testHome()
    {
        loadPage( baseUrl, 30 );
        WebDriverWait wait = new WebDriverWait( getWebDriver(), 30 );
        wait.until( ExpectedConditions.titleContains( "Apache Archiva" ) );
    }

    @Test
    public void testInitialRepositories()
    {
        WebDriverWait wait = new WebDriverWait( getWebDriver(), 20 );
        WebElement el;
        el = wait.until( ExpectedConditions.elementToBeClickable( By.id( "menu-repositories-list-a" ) ) );
        tryClick( el, ExpectedConditions.presenceOfElementLocated( By.xpath( "//table[@id='managed-repositories-table']//td[contains(text(),'internal')]" ) ),
            "Managed Repositories not activated" );
        wait.until( ExpectedConditions.visibilityOfElementLocated( By.xpath( "//table[@id='managed-repositories-table']//td[contains(text(),'snapshots')]" ) ) );
        el = wait.until( ExpectedConditions.elementToBeClickable( By.xpath( "//a[@href='#remote-repositories-content']" ) ) );
        tryClick( el, ExpectedConditions.visibilityOfElementLocated( By.xpath( "//table[@id='remote-repositories-table']//td[contains(text(),'central')]" ) ),
            "Remote Repositories View not available" );

    }
}