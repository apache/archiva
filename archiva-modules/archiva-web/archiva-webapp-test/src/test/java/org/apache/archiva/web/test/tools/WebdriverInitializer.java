package org.apache.archiva.web.test.tools;
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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by martin_s on 04.06.17.
 */
public class WebdriverInitializer
{

    public static WebDriver newWebDriver() {
        String seleniumBrowser = System.getProperty("selenium.browser");
        String seleniumHost = System.getProperty("seleniumHost", "localhost");
        int seleniumPort = Integer.getInteger("seleniumPort", 4444);
        boolean seleniumRemote = Boolean.parseBoolean(System.getProperty("seleniumRemote","false"));
        return newWebDriver(seleniumBrowser,seleniumHost, seleniumPort,seleniumRemote);
    }

    public static WebDriver newWebDriver(String seleniumBrowser, String seleniumHost, int seleniumPort, boolean seleniumRemote) {
        try {

            if ( StringUtils.contains(seleniumBrowser, "chrome")) {
                if (seleniumRemote)
                {
                    return new RemoteWebDriver( new URL( "http://" + seleniumHost + ":" + seleniumPort + "/wd/hub" ),
                        DesiredCapabilities.chrome()
                    );
                } else {
                    return new ChromeDriver(  );
                }
            }

            if (StringUtils.contains(seleniumBrowser, "safari")) {
                if (seleniumRemote)
                {
                    return new RemoteWebDriver( new URL( "http://" + seleniumHost + ":" + seleniumPort + "/wd/hub" ),
                        DesiredCapabilities.safari()
                    );
                } else {
                    return new SafariDriver();
                }
            }

            if (StringUtils.contains(seleniumBrowser, "iexplore")) {
                if (seleniumRemote)
                {
                    return new RemoteWebDriver( new URL( "http://" + seleniumHost + ":" + seleniumPort + "/wd/hub" ),
                        DesiredCapabilities.internetExplorer()
                    );
                } else {
                    new InternetExplorerDriver(  );
                }
            }

            if (StringUtils.contains( seleniumBrowser, "firefox" ))
            {
                if ( seleniumRemote )
                {
                    return new RemoteWebDriver( new URL( "http://" + seleniumHost + ":" + seleniumPort + "/wd/hub" ),
                        DesiredCapabilities.firefox()
                    );
                }
                else
                {
                    return new FirefoxDriver();
                }
            }

            if ( seleniumRemote )
            {
                return new RemoteWebDriver( new URL( "http://" + seleniumHost + ":" + seleniumPort + "/wd/hub" ),
                    DesiredCapabilities.htmlUnit()
                );
            }
            else
            {
                DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
                capabilities.setJavascriptEnabled( true );
                capabilities.setVersion( "firefox-51" );
                return new HtmlUnitDriver( capabilities  );
            }

        } catch (MalformedURLException e) {
            throw new RuntimeException("Initializion of remote driver failed");
        }

    }
}
