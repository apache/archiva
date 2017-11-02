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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
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
        // login( getAdminUsername(), getAdminPassword() );
        WebDriverWait wait = new WebDriverWait(getWebDriver(), 20);
        WebElement el;
        el = wait.until(ExpectedConditions.elementToBeClickable(By.id("menu-repositories-list-a")));
        tryClick( el,  ExpectedConditions.presenceOfElementLocated( By.id( "managed-repositories-view-a" ) ),
            "Managed Repositories not activated");
        el = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='#remote-repositories-content']")));
        tryClick(el,ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@id='remote-repositories-table']//td[contains(text(),'central')]")),
            "Remote Repositories View not available");
        el = wait.until(ExpectedConditions.elementToBeClickable( By.xpath("//a[@href='#remote-repository-edit']") ));
        el = tryClick(el, ExpectedConditions.visibilityOfElementLocated(By.id("remote-repository-save-button")),
            "Repository Save Button not available");
        
        setFieldValue( "id", "myrepoid" );        
        setFieldValue( "name", "My repo name" );        
        setFieldValue( "url", "http://www.repo.org" );

        el = wait.until( ExpectedConditions.elementToBeClickable(By.id("remote-repository-save-button") ));
        Actions actions = new Actions(getWebDriver());
        actions.moveToElement(el);
        actions.perform();
        ((JavascriptExecutor)getWebDriver()).executeScript("arguments[0].scrollIntoView();", el);
        el.click();
        el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("remote-repositories-view-a")));
        ((JavascriptExecutor)getWebDriver()).executeScript("arguments[0].scrollIntoView();", el);
        tryClick(By.id("menu-proxy-connectors-list-a"),
            ExpectedConditions.visibilityOfElementLocated(By.id("proxy-connectors-view-tabs-a-network-proxies-grid")),
            "Network proxies not available",
            3,10
            );
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("main-content"), "Proxy Connectors"));
        // proxy connect
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("proxy-connectors-view"), "central" ));
        assertTextNotPresent( "myrepoid" );
        el = wait.until(ExpectedConditions.elementToBeClickable( By.id("proxy-connectors-view-tabs-a-edit") ));
        el.click();
        el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("proxy-connector-btn-save")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("remote-repository-edit-fieldset")));
        // Another hack, don't know why the normal selectValue() does not work here
        ((JavascriptExecutor)getWebDriver()).executeScript("jQuery('#sourceRepoId').css('display','block')");
        Select select = new Select(getWebDriver().findElement(By.xpath(".//select[@id='sourceRepoId']")));
        select.selectByVisibleText("internal");
        // selectValue( "sourceRepoId", "internal", true );
        // Workaround
        // TODO: Check after upgrade of htmlunit, bootstrap or jquery
        // TODO: Check whats wrong here
        ( (JavascriptExecutor) getWebDriver() ).executeScript( "$('#targetRepoId').show();" );
        // End of Workaround
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("targetRepoId")));
        selectValue( "targetRepoId", "myrepoid" );
        el.click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("user-messages"),"ProxyConnector added"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("proxy-connectors-view"), "central" ));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("proxy-connectors-view"), "myrepoid" ));

        tryClick(By.xpath("//i[contains(@class,'icon-resize-vertical')]//ancestor::a" ),
            ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("proxy-connector-edit-order-div")),
            "Edit order view not visible", 3, 10);
        // clickLinkWithXPath( "//i[contains(@class,'icon-resize-vertical')]//ancestor::a");
        // This is needed here for HTMLUnit Tests. Currently do not know why, wait is not working for the
        // list entries down
        // waitPage();
        // el = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("proxy-connector-edit-order-div")));
        assertTextPresent( "internal" );
        List<WebElement> repos = wait.until(ExpectedConditions.numberOfElementsToBe( By.xpath("//div[@id='proxy-connector-edit-order-div']/div"), 2));
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