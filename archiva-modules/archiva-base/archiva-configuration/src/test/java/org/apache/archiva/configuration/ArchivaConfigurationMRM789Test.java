package org.apache.archiva.configuration;

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

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import javax.inject.Inject;
import org.apache.archiva.common.utils.FileUtil;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test the configuration store.
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class ArchivaConfigurationMRM789Test
{

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    FileTypes filetypes;

    public static File getTestFile( String path )
    {
        return new File( FileUtil.getBasedir(), path );
    }

    protected <T> T lookup( Class<T> clazz, String hint )
    {
        return (T) applicationContext.getBean( "archivaConfiguration#" + hint, ArchivaConfiguration.class );
    }
   
    // test for [MRM-789]
    @Test
    public void testGetConfigurationFromDefaultsWithDefaultRepoLocationAlreadyExisting()
        throws Exception
    {
        File repo = new File( FileUtil.getBasedir(), "target/test-classes/existing_snapshots" );
        repo.mkdirs();

        repo = new File( FileUtil.getBasedir(), "target/test-classes/existing_internal" );
        repo.mkdirs();

        String existingTestDefaultArchivaConfigFile = FileUtils.readFileToString(
            getTestFile( "target/test-classes/org/apache/archiva/configuration/test-default-archiva.xml" ) );
        existingTestDefaultArchivaConfigFile =
            StringUtils.replace( existingTestDefaultArchivaConfigFile, "${appserver.base}", FileUtil.getBasedir() );

        File generatedTestDefaultArchivaConfigFile = new File( FileUtil.getBasedir(),
                                                               "target/test-classes/org/apache/archiva/configuration/default-archiva.xml" );

        FileUtils.writeStringToFile( generatedTestDefaultArchivaConfigFile, existingTestDefaultArchivaConfigFile,
                                     Charset.forName("UTF-8") );

        ArchivaConfiguration archivaConfiguration =
            lookup( ArchivaConfiguration.class, "test-defaults-default-repo-location-exists" );
        Configuration configuration = archivaConfiguration.getConfiguration();
        assertConfiguration( configuration, 2, 2, 2 );

        ManagedRepositoryConfiguration repository = configuration.getManagedRepositories().get( 0 );
        assertTrue( "check managed repositories", repository.getLocation().endsWith( "data/repositories/internal" ) );

        generatedTestDefaultArchivaConfigFile.delete();
        assertFalse( generatedTestDefaultArchivaConfigFile.exists() );
    }


    /**
     * Ensures that the provided configuration matches the details present in the archiva-default.xml file.
     */
    private void assertConfiguration( Configuration configuration, int managedExpected, int remoteExpected,
                                      int proxyConnectorExpected )
        throws Exception
    {

        assertEquals( "check managed repositories: " + configuration.getManagedRepositories(), managedExpected,
                      configuration.getManagedRepositories().size() );
        assertEquals( "check remote repositories: " + configuration.getRemoteRepositories(), remoteExpected,
                      configuration.getRemoteRepositories().size() );
        assertEquals( "check proxy connectors:" + configuration.getProxyConnectors(), proxyConnectorExpected,
                      configuration.getProxyConnectors().size() );

        RepositoryScanningConfiguration repoScanning = configuration.getRepositoryScanning();
        assertNotNull( "check repository scanning", repoScanning );
        assertEquals( "check file types", 4, repoScanning.getFileTypes().size() );
        assertEquals( "check known consumers", 9, repoScanning.getKnownContentConsumers().size() );
        assertEquals( "check invalid consumers", 1, repoScanning.getInvalidContentConsumers().size() );

        List<String> patterns = filetypes.getFileTypePatterns( "artifacts" );
        assertNotNull( "check 'artifacts' file type", patterns );
        assertEquals( "check 'artifacts' patterns", 13, patterns.size() );

        WebappConfiguration webapp = configuration.getWebapp();
        assertNotNull( "check webapp", webapp );

        UserInterfaceOptions ui = webapp.getUi();
        assertNotNull( "check webapp ui", ui );
        assertTrue( "check showFindArtifacts", ui.isShowFindArtifacts() );
        assertTrue( "check appletFindEnabled", ui.isAppletFindEnabled() );
    }

   
}
