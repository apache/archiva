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

import com.gargoylesoftware.htmlunit.WebClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by martin_s on 04.06.17.
 */
public class WebdriverUtility
{

    static final Logger log = LoggerFactory.getLogger( WebdriverUtility.class );

    public static WebDriver newWebDriver() {
        String seleniumBrowser = System.getProperty("selenium.browser");
        String seleniumHost = System.getProperty("seleniumHost", "localhost");
        int seleniumPort = Integer.getInteger("seleniumPort", 4444);
        boolean seleniumRemote = Boolean.parseBoolean(System.getProperty("seleniumRemote","false"));
        return newWebDriver(seleniumBrowser,seleniumHost, seleniumPort,seleniumRemote);
    }

    public static WebDriver newWebDriver(String seleniumBrowser, String seleniumHost, int seleniumPort, boolean seleniumRemote) {
        log.info("WebDriver {}, {}, {}, {}", seleniumBrowser, seleniumHost, seleniumPort, seleniumRemote);
        if (seleniumRemote && StringUtils.isEmpty( seleniumHost )) {
            throw new IllegalArgumentException( "seleniumHost must be set, when seleniumRemote=true" );
        }
        try {

            if ( StringUtils.contains(seleniumBrowser, "chrome")) {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("start-maximized");
                if (seleniumRemote)
                {
                    DesiredCapabilities capabilities = DesiredCapabilities.chrome();
                    capabilities.setCapability( ChromeOptions.CAPABILITY, options );
                    return new RemoteWebDriver( new URL( "http://" + seleniumHost + ":" + seleniumPort + "/wd/hub" ),
                        capabilities
                    );
                } else {
                    return new ChromeDriver( options );
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

            DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit();
            capabilities.setJavascriptEnabled( true );
            capabilities.setVersion( "firefox-52" );
            WebDriver driver;
            if ( seleniumRemote )
            {
                driver = new RemoteWebDriver( new URL( "http://" + seleniumHost + ":" + seleniumPort + "/wd/hub" ),
                    capabilities
                );
            }
            else
            {
                driver = new HtmlUnitDriver( capabilities  ) {
                    @Override
                    protected WebClient modifyWebClient( WebClient client )
                    {
                        client.getOptions().setThrowExceptionOnFailingStatusCode( false );
                        client.getOptions().setThrowExceptionOnScriptError( false );
                        client.getOptions().setCssEnabled( true );
                        return client;
                    }
                };

            }
            return driver;

        } catch (MalformedURLException e) {
            throw new RuntimeException("Initializion of remote driver failed");
        }

    }

    public static String getBaseUrl() {

        if (System.getProperties().containsKey( "baseUrl" )) {
            return System.getProperty("baseUrl");
        }
        int containerPort = 7777;
        if (System.getProperties().containsKey("container.http.port")) {
            containerPort = Integer.parseInt(System.getProperty("container.http.port"));
        } else if (System.getProperties().containsKey("container.propertiesPortFilePath"))
        {
            Properties portProperties = new Properties();
            try (InputStream inputStream = Files.newInputStream(Paths.get(System.getProperty("container.propertiesPortFilePath"))))
            {
                portProperties.load(inputStream);
            }
            catch ( IOException e )
            {
                log.error("Error during property loading with containger.propertiesPortFilePath");
            }
            if ( portProperties.containsKey( "tomcat.maven.http.port" ) )
            {
                containerPort = Integer.parseInt( portProperties.getProperty( "tomcat.maven.http.port" ) );
            }
            else
            {
                containerPort = Integer.parseInt( portProperties.getProperty( "container.http.port" ) );
            }
        }
        return "http://localhost:" + containerPort+"/archiva";
    }

    public static Path takeScreenShot( String fileName, WebDriver driver) {
        Path result = null;
        try
        {
            Path snapDir = Paths.get( "target", "errorshtmlsnap" );
            Path screenShotDir = Paths.get("target","screenshots");
            if ( !Files.exists( snapDir ) )
            {
                Files.createDirectories( snapDir );
            }
            Path htmlFile = snapDir.resolve( fileName + ".html" );
            Path screenShotFile = screenShotDir.resolve( fileName );
            String pageSource=null;
            String encoding="ISO-8859-1";
            try
            {
                pageSource = ( (JavascriptExecutor) driver ).executeScript( "return document.documentElement.outerHTML;" ).toString();
            } catch (Exception e) {
                log.info("Could not create html source by javascript");
                pageSource = driver.getPageSource();
            }
            if (pageSource.contains("encoding=\"")) {
                encoding = pageSource.replaceFirst( ".*encoding=\"([^\"]+)\".*", "$1" );
            }
            FileUtils.writeStringToFile( htmlFile.toFile(), pageSource, encoding);
            try
            {
                Path scrs = ((TakesScreenshot)driver).getScreenshotAs( OutputType.FILE ).toPath();
                Files.copy(scrs, screenShotFile);
            }
            catch ( Exception e )
            {
                log.info( "Could not create screenshot: " + e.getMessage() );
            }

        }
        catch ( IOException e )
        {
            log.info( "Creating screenshot failed " + e.getMessage() );
        }
        return result;
    }
}
