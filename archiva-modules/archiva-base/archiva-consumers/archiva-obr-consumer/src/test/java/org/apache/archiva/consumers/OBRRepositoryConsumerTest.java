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
import java.io.IOException;
import java.util.Date;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;

/**
 *
 * @author jdumay
 */
public class OBRRepositoryConsumerTest extends TestCase
{
    private OBRRepositoryConsumer consumer;

    private File testRepo;

    private ManagedRepositoryConfiguration configuration;

    private void createRepository(String name) throws ConsumerException, IOException
    {
        testRepo = new File("target/" + name + "_repo").getAbsoluteFile();
        if (testRepo.exists())
        {
            FileUtils.deleteDirectory(testRepo);
        }
        testRepo.mkdirs();
        consumer = new OBRRepositoryConsumer();
        configuration = new ManagedRepositoryConfiguration();
        configuration.setName("My Test OSGi repository");
        configuration.setId("test-obr-osgi-repo");
        configuration.setLocation(testRepo.getAbsolutePath());
        FileUtils.copyDirectory(new File("src/test/resources/repository"), testRepo);
        consumer.beginScan(configuration, new Date());
    }

    public void testCreatesRepositoryXml() throws Exception
    {
        createRepository("testCreatesRepositoryXml");
        File repositoryZip = new File(configuration.getLocation(), "repository.zip");
        assertFalse("repository.xml should not exist", repositoryZip.exists());
        consumer.processFile(new File(testRepo, "commons-codec/commons-codec/1.3.0/commons-codec-1.3.0.jar").getAbsolutePath());
        consumer.processFile(new File(testRepo, "commons-io/commons-io/1.4.0/commons-io-1.4.0.jar").getAbsolutePath());
        assertTrue("repository.xml should exist", repositoryZip.exists());

        RepositoryImpl repository = new RepositoryImpl(repositoryZip.toURL());
        repository.refresh();

        assertEquals(configuration.getName(), repository.getName());
        assertEquals(2, repository.getResources().length);

        assertEquals("com.springsource.org.apache.commons.io/1.4.0", repository.getResources()[0].getId());
        assertEquals("Apache Commons IO", repository.getResources()[0].getPresentationName());
        assertEquals("com.springsource.org.apache.commons.io", repository.getResources()[0].getSymbolicName());
        assertEquals(new File(testRepo, "commons-io/commons-io/1.4.0/commons-io-1.4.0.jar").toURL().toString(), repository.getResources()[0].getURL().toString());
        assertEquals("1.4.0", repository.getResources()[0].getVersion().toString());

        assertEquals("com.springsource.org.apache.commons.codec/1.3.0", repository.getResources()[1].getId());
        assertEquals("Apache Commons Codec", repository.getResources()[1].getPresentationName());
        assertEquals("com.springsource.org.apache.commons.codec", repository.getResources()[1].getSymbolicName());
        assertEquals(new File(testRepo, "commons-codec/commons-codec/1.3.0/commons-codec-1.3.0.jar").toURL().toString(), repository.getResources()[1].getURL().toString());
        assertEquals("1.3.0", repository.getResources()[1].getVersion().toString());
    }
}
