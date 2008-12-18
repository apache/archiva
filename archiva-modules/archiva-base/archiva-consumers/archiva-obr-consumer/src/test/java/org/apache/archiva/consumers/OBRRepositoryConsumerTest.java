/*
 *  Copyright 2008 jdumay.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.apache.archiva.consumers;

import java.io.File;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.spring.PlexusToSpringUtils;

/**
 *
 * @author jdumay
 */
public class OBRRepositoryConsumerTest extends PlexusInSpringTestCase
{
    private OBRRepositoryConsumer consumer;

    private File testRepo;

    private ManagedRepositoryConfiguration configuration;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        File testRepoData = new File("src/test/resources/repository");
        testRepo = new File("target/obrtestrepo").getAbsoluteFile();
        testRepo.mkdirs();
        consumer = (OBRRepositoryConsumer)lookup(PlexusToSpringUtils.buildSpringId(OBRRepositoryConsumer.class));
        FileUtils.copyDirectory(testRepoData, testRepo);
        configuration = new ManagedRepositoryConfiguration();
        configuration.setName("My Test OSGi repository");
        configuration.setId("test-obr-osgi-repo");
        configuration.setLocation(testRepo.getAbsolutePath());
        consumer.beginScan(configuration, new Date());
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        FileUtils.deleteDirectory(testRepo);
    }

    public void testCreatesRepositoryXml() throws Exception
    {
        File repositoryXml = new File(configuration.getLocation(), "repository.xml");
        assertFalse("repository.xml should not exist", repositoryXml.exists());
        consumer.processFile(new File(testRepo, "./commons-codec/commons-codec/1.3.0/commons-codec-1.3.0.jar").getAbsolutePath());
        assertTrue("repository.xml should exist", repositoryXml.exists());
    }
}
