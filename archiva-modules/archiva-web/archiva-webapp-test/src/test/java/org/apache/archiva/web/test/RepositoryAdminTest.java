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
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

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
        WebDriverWait wait = new WebDriverWait(getWebDriver(), 10);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("menu-repositories-list-a")));
        clickLinkWithLocator( "menu-repositories-list-a");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("managed-repositories-view-a")));
        clickLinkWithXPath( "//a[@href='#remote-repositories-content']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("remote-repositories-view-a")));
        clickLinkWithXPath( "//a[@href='#remote-repository-edit']", false );
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("remote-repository-save-button")));
        
        setFieldValue( "id", "myrepoid" );        
        setFieldValue( "name", "My repo name" );        
        setFieldValue( "url", "http://www.repo.org" );
        
        clickButtonWithLocator( "remote-repository-save-button");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("remote-repositories-view-a")));

        clickLinkWithLocator( "menu-proxy-connectors-list-a");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("proxy-connectors-view-tabs-a-network-proxies-grid")));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("main-content"), "Proxy Connectors"));
        // proxy connect
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("proxy-connectors-view"), "central" ));
        assertTextNotPresent( "myrepoid" );
        clickButtonWithLocator( "proxy-connectors-view-tabs-a-edit");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("proxy-connector-btn-save")));
        selectValue( "sourceRepoId", "internal" );
        // Workaround
        // TODO: Check after upgrade of htmlunit, bootstrap or jquery
        // TODO: Check whats wrong here
        ( (JavascriptExecutor) getWebDriver() ).executeScript( "$('#targetRepoId').show();" );
        // End of Workaround
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("targetRepoId")));
        selectValue( "targetRepoId", "myrepoid" );
        clickButtonWithLocator( "proxy-connector-btn-save");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("user-messages"),"ProxyConnector added"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("proxy-connectors-view"), "central" ));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("proxy-connectors-view"), "myrepoid" ));
        clickLinkWithXPath( "//i[contains(@class,'icon-resize-vertical')]//ancestor::a");
        // This is needed here for HTMLUnit Tests. Currently do not know why, wait is not working for the
        // list entries down
        waitPage();
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("proxy-connector-edit-order-div")));
        assertTextPresent( "internal" );
        List<WebElement> repos = el.findElements(By.xpath("./div"));
        Assert.assertTrue("First repo is myrepo", repos.get(0).getText().contains("myrepoid"));
        Assert.assertTrue("Second repo is central", repos.get(1).getText().contains("central"));

        // works until this point
        /*getSelenium().mouseDown( "xpath=//div[@id='proxy-connector-edit-order-div']/div[1]" );
        getSelenium().mouseMove( "xpath=//div[@id='proxy-connector-edit-order-div']/div[2]" );
        getSelenium().mouseUp( "xpath=//div[@id='proxy-connector-edit-order-div']/div[last()]" );
        Assert.assertTrue( "Second repo is myrepo", getSelenium().getText("xpath=//div[@id='proxy-connector-edit-order-div']/div[2]" ).contains( "myrepoid" ));
        Assert.assertTrue( "First repo is central", getSelenium().getText("xpath=//div[@id='proxy-connector-edit-order-div']/div[1]" ).contains( "central" ));
        */
    }
    
}