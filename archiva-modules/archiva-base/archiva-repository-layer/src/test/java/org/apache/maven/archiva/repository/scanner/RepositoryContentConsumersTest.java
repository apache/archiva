package org.apache.maven.archiva.repository.scanner;

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

import java.io.IOException;
import java.util.Locale;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.AbstractRepositoryLayerTestCase;
import org.easymock.MockControl;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.io.Resource;

/**
 * RepositoryContentConsumersTest
 *
 * @version $Id$
 */
public class RepositoryContentConsumersTest
    extends AbstractRepositoryLayerTestCase
{
    private RepositoryContentConsumers lookupRepositoryConsumers()
        throws Exception
    {
        ArchivaConfiguration configuration = (ArchivaConfiguration)lookup(ArchivaConfiguration.class);

        RepositoryContentConsumers consumerUtilStub = new RepositoryContentConsumersStub(configuration);
               
        RepositoryContentConsumers consumerUtil = (RepositoryContentConsumers) lookup( RepositoryContentConsumers.class
                                                                                           .getName() );
        ApplicationContext context = new MockApplicationContext(consumerUtil.getAvailableKnownConsumers(), consumerUtil.getAvailableInvalidConsumers());

        consumerUtilStub.setApplicationContext(context);
        consumerUtilStub.setSelectedInvalidConsumers( consumerUtil.getSelectedInvalidConsumers() );
        consumerUtilStub.setSelectedKnownConsumers( consumerUtil.getSelectedKnownConsumers() );        
        consumerUtilStub.setArchivaConfiguration( configuration );
        
        assertNotNull( "RepositoryContentConsumers should not be null.", consumerUtilStub );
        
        return consumerUtilStub;
    }

    public void testGetSelectedKnownIds()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedKnownIds[] = new String[] {
            "update-db-artifact",
            "create-missing-checksums",
            "update-db-repository-metadata",
            "validate-checksum",
            "validate-signature",
            "index-content",
            "auto-remove",
            "auto-rename" };

        List<String> knownConsumers = consumerutil.getSelectedKnownConsumerIds();
        assertNotNull( "Known Consumer IDs should not be null", knownConsumers );
        assertEquals( "Known Consumer IDs.size", expectedKnownIds.length, knownConsumers.size() );

        for ( String expectedId : expectedKnownIds )
        {
            assertTrue( "Known id [" + expectedId + "] exists.", knownConsumers.contains( expectedId ) );
        }
    }

    public void testGetSelectedInvalidIds()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedInvalidIds[] = new String[] { "update-db-bad-content" };

        List<String> invalidConsumers = consumerutil.getSelectedInvalidConsumerIds();
        assertNotNull( "Invalid Consumer IDs should not be null", invalidConsumers );
        assertEquals( "Invalid Consumer IDs.size", expectedInvalidIds.length, invalidConsumers.size() );

        for ( String expectedId : expectedInvalidIds )
        {
            assertTrue( "Invalid id [" + expectedId + "] exists.", invalidConsumers.contains( expectedId ) );
        }
    }

    public void testGetSelectedKnownConsumerMap()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedSelectedKnownIds[] = new String[] {
            "update-db-artifact",
            "create-missing-checksums",
            "update-db-repository-metadata",
            "validate-checksum",
            "index-content",
            "auto-remove",
            "auto-rename" };

        Map<String, KnownRepositoryContentConsumer> knownConsumerMap = consumerutil.getSelectedKnownConsumersMap();
        assertNotNull( "Known Consumer Map should not be null", knownConsumerMap );
        assertEquals( "Known Consumer Map.size", expectedSelectedKnownIds.length, knownConsumerMap.size() );

        for ( String expectedId : expectedSelectedKnownIds )
        {
            KnownRepositoryContentConsumer consumer = knownConsumerMap.get( expectedId );
            assertNotNull( "Known[" + expectedId + "] should not be null.", consumer );
            assertEquals( "Known[" + expectedId + "].id", expectedId, consumer.getId() );
        }
    }

    public void testGetSelectedInvalidConsumerMap()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedSelectedInvalidIds[] = new String[] { "update-db-bad-content" };

        Map<String, InvalidRepositoryContentConsumer> invalidConsumerMap = consumerutil
            .getSelectedInvalidConsumersMap();
        assertNotNull( "Invalid Consumer Map should not be null", invalidConsumerMap );
        assertEquals( "Invalid Consumer Map.size", expectedSelectedInvalidIds.length, invalidConsumerMap.size() );

        for ( String expectedId : expectedSelectedInvalidIds )
        {
            InvalidRepositoryContentConsumer consumer = invalidConsumerMap.get( expectedId );
            assertNotNull( "Known[" + expectedId + "] should not be null.", consumer );
            assertEquals( "Known[" + expectedId + "].id", expectedId, consumer.getId() );
        }
    }

    public void testGetAvailableKnownList()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedKnownIds[] = new String[] {
            "update-db-artifact",
            "create-missing-checksums",
            "update-db-repository-metadata",
            "validate-checksum",
            "index-content",
            "auto-remove",
            "auto-rename",
            "available-but-unselected" };

        List<KnownRepositoryContentConsumer> knownConsumers = consumerutil.getAvailableKnownConsumers();
        assertNotNull( "known consumers should not be null.", knownConsumers );
        assertEquals( "known consumers", expectedKnownIds.length, knownConsumers.size() );

        List<String> expectedIds = Arrays.asList( expectedKnownIds );
        for ( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            assertTrue( "Consumer [" + consumer.getId() + "] returned by .getAvailableKnownConsumers() is unexpected.",
                        expectedIds.contains( consumer.getId() ) );
        }
    }

    public void testGetAvailableInvalidList()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedInvalidIds[] = new String[] { "update-db-bad-content", "move-to-trash-then-notify" };

        List<InvalidRepositoryContentConsumer> invalidConsumers = consumerutil.getAvailableInvalidConsumers();
        assertNotNull( "invalid consumers should not be null.", invalidConsumers );
        assertEquals( "invalid consumers", expectedInvalidIds.length, invalidConsumers.size() );

        List<String> expectedIds = Arrays.asList( expectedInvalidIds );
        for ( InvalidRepositoryContentConsumer consumer : invalidConsumers )
        {
            assertTrue( "Consumer [" + consumer.getId()
                + "] returned by .getAvailableInvalidConsumers() is unexpected.", expectedIds.contains( consumer
                .getId() ) );
        }
    }

    public void testExecution()
        throws Exception
    {
        MockControl knownControl = MockControl.createNiceControl( KnownRepositoryContentConsumer.class );
        RepositoryContentConsumers consumers = lookupRepositoryConsumers();
        KnownRepositoryContentConsumer selectedKnownConsumer = (KnownRepositoryContentConsumer) knownControl.getMock();
        KnownRepositoryContentConsumer unselectedKnownConsumer =
            (KnownRepositoryContentConsumer) MockControl.createNiceControl(
                KnownRepositoryContentConsumer.class ).getMock();

        consumers.setApplicationContext(new MockApplicationContext(Arrays.asList( selectedKnownConsumer, unselectedKnownConsumer ), null));

        consumers.setSelectedKnownConsumers( Collections.singletonList( selectedKnownConsumer ) );

        MockControl invalidControl = MockControl.createControl( InvalidRepositoryContentConsumer.class );
        InvalidRepositoryContentConsumer selectedInvalidConsumer =
            (InvalidRepositoryContentConsumer) invalidControl.getMock();
        InvalidRepositoryContentConsumer unselectedInvalidConsumer =
            (InvalidRepositoryContentConsumer) MockControl.createControl(
                InvalidRepositoryContentConsumer.class ).getMock();

        consumers.setApplicationContext( new MockApplicationContext(null, Arrays.asList( selectedInvalidConsumer, unselectedInvalidConsumer )));

        consumers.setSelectedInvalidConsumers( Collections.singletonList( selectedInvalidConsumer ) );

        ManagedRepositoryConfiguration repo = createRepository( "id", "name", getTestFile( "target/test-repo" ) );
        File testFile = getTestFile( "target/test-repo/path/to/test-file.txt" );

        Date startTime = new Date( System.currentTimeMillis() );
        startTime.setTime( 12345678 );

        selectedKnownConsumer.beginScan( repo, startTime, false );
        selectedKnownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.EMPTY_LIST );
        selectedKnownConsumer.getIncludes();
        knownControl.setReturnValue( Collections.singletonList( "**/*.txt" ) );
        selectedKnownConsumer.processFile( _OS( "path/to/test-file.txt" ), false );
        //        knownConsumer.completeScan();
        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo, startTime, false );
        //        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, testFile, true );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        File notIncludedTestFile = getTestFile( "target/test-repo/path/to/test-file.xml" );

        selectedKnownConsumer.beginScan( repo, startTime, false );
        selectedKnownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.EMPTY_LIST );
        selectedKnownConsumer.getIncludes();
        knownControl.setReturnValue( Collections.singletonList( "**/*.txt" ) );
        //        knownConsumer.completeScan();
        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo, startTime, false );
        selectedInvalidConsumer.processFile( _OS( "path/to/test-file.xml" ), false );
        selectedInvalidConsumer.getId();
        invalidControl.setReturnValue( "invalid" );
        //        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, notIncludedTestFile, true );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        File excludedTestFile = getTestFile( "target/test-repo/path/to/test-file.txt" );

        selectedKnownConsumer.beginScan( repo, startTime, false );
        selectedKnownConsumer.getExcludes();
        knownControl.setReturnValue( Collections.singletonList( "**/test-file.txt" ) );
        //        knownConsumer.completeScan();
        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo, startTime, false );
        selectedInvalidConsumer.processFile( _OS( "path/to/test-file.txt" ), false );
        selectedInvalidConsumer.getId();
        invalidControl.setReturnValue( "invalid" );
        //        invalidConsumer.completeScan();
        invalidControl.replay();

        consumers.executeConsumers( repo, excludedTestFile, true );

        knownControl.verify();
        invalidControl.verify();
    }
    
    /**
     * Create an OS specific version of the filepath.
     * Provide path in unix "/" format.
     */
    private String _OS( String path )
    {
        if ( SystemUtils.IS_OS_WINDOWS )
        {
            return path.replace( '/', '\\' );
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    public class MockApplicationContext implements ApplicationContext
    {
        private Map convertToMap(List objects)
        {
            HashMap map = new HashMap();
            for (Object o : objects)
            {
                map.put(o, o);
            }
            return map;
        }

        private List<KnownRepositoryContentConsumer> knownRepositoryContentConsumer;

        private List<InvalidRepositoryContentConsumer> invalidRepositoryContentConsumers;

        public MockApplicationContext(List<KnownRepositoryContentConsumer> knownRepositoryContentConsumer, List<InvalidRepositoryContentConsumer> invalidRepositoryContentConsumers)
        {
            this.knownRepositoryContentConsumer = knownRepositoryContentConsumer;
            this.invalidRepositoryContentConsumers = invalidRepositoryContentConsumers;
        }

        public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getDisplayName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getId() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ApplicationContext getParent() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public long getStartupDate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean containsBeanDefinition(String beanName) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getBeanDefinitionCount() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String[] getBeanDefinitionNames() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String[] getBeanNamesForType(Class type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String[] getBeanNamesForType(Class type, boolean includeNonSingletons, boolean allowEagerInit) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Map getBeansOfType(Class type) throws BeansException {
            if (type == KnownRepositoryContentConsumer.class)
            {
                return convertToMap(knownRepositoryContentConsumer);
            }
            if (type == InvalidRepositoryContentConsumer.class)
            {
                return convertToMap(invalidRepositoryContentConsumers);
            }
            throw new UnsupportedOperationException("Should not have been called");
        }

        public Map getBeansOfType(Class type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean containsBean(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String[] getAliases(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object getBean(String name) throws BeansException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object getBean(String name, Class requiredType) throws BeansException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object getBean(String name, Object[] args) throws BeansException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Class getType(String name) throws NoSuchBeanDefinitionException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isTypeMatch(String name, Class targetType) throws NoSuchBeanDefinitionException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean containsLocalBean(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public BeanFactory getParentBeanFactory() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void publishEvent(ApplicationEvent event) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Resource[] getResources(String locationPattern) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public ClassLoader getClassLoader() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Resource getResource(String location) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
