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
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


/**
 * @author Olivier Lamy
 */
public class RolesManagementTest
    extends AbstractArchivaTest
{

    @Test
    public void testReadRolesAndUpdateDescription()
        throws Exception
    {
        login( getAdminUsername(), getAdminPassword() );
        WebDriverWait wait = new WebDriverWait(getWebDriver(), 10);
        WebElement link = wait.until(ExpectedConditions.elementToBeClickable( By.id("menu-roles-list-a") ));
        tryClick( link, ExpectedConditions.textToBePresentInElementLocated(By.id("roles-view"),"Archiva System Administrator"),
            "Roles view not available");
        Assert.assertTrue( StringUtils.isEmpty( getText( "role-description-Guest" ) ) );
        clickLinkWithLocator( "edit-role-Guest" );
        wait.until(ExpectedConditions.elementToBeClickable(By.id("role-edit-description-save")));
        String desc = "The guest description";
        setFieldValue( "role-edit-description", desc );
        clickButtonWithLocator( "role-edit-description-save" );
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("user-messages"), "Role Guest updated."));
        clickLinkWithLocator( "roles-view-tabs-a-roles-grid" );
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("role-description-Guest"), desc));
    }
}
