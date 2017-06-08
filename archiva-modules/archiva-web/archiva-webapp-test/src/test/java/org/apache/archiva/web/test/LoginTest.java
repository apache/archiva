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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Based on LoginTest of Emmanuel Venisse test.
 *
 * @author José Morales Martínez
 *
 */


public class LoginTest
    extends AbstractArchivaTest
{

    @Test
    public void testWithBadUsername()
    {
        goToLoginPage();
        setFieldValue( "user-login-form-username", "badUsername" );
        clickLinkWithLocator( "modal-login-ok", false );
        WebDriverWait wait = new WebDriverWait(getWebDriver(), 2);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("modal-login"), "This field is required."));

    }

    @Test
    public void testWithBadPassword()
    {
        goToLoginPage();
        setFieldValue( "user-login-form-username", getProperty( "ADMIN_USERNAME" ) );
        setFieldValue( "user-login-form-password", "badPassword" );
        clickLinkWithLocator( "modal-login-ok", false );
        WebDriverWait wait = new WebDriverWait(getWebDriver(), 2);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("modal-login"), "You have entered an incorrect username and/or password"));
    }

    @Test
    public void testWithEmptyUsername()
    {
        goToLoginPage();
        setFieldValue( "user-login-form-password", "password" );
        clickLinkWithLocator( "modal-login-ok", false);
        WebDriverWait wait = new WebDriverWait(getWebDriver(), 2);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("modal-login"), "This field is required."));
    }

    @Test
    public void testWithEmptyPassword()
    {
        goToLoginPage();
        setFieldValue( "user-login-form-username", getProperty( "ADMIN_USERNAME" ) );
        Assert.assertTrue(getWebDriver().findElement( By.id("modal-login-ok") ).isDisplayed());
        Assert.assertTrue(getWebDriver().findElement( By.id("modal-login-ok") ).isEnabled());
        clickLinkWithLocator( "modal-login-ok", false );
        WebDriverWait wait = new WebDriverWait(getWebDriver(), 2);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("modal-login"), "This field is required."));
    }

    @Test
    public void testWithCorrectUsernamePassword()
    {
        goToLoginPage();
        setFieldValue( "user-login-form-username", getProperty( "ADMIN_USERNAME" ) );
        setFieldValue( "user-login-form-password", getProperty( "ADMIN_PASSWORD" ) );
        clickLinkWithLocator( "modal-login-ok");

        assertUserLoggedIn( getProperty( "ADMIN_USERNAME" ) );
    }


}