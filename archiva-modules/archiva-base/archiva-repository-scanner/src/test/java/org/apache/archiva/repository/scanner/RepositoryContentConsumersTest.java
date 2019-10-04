package org.apache.archiva.repository.scanner;

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

import junit.framework.TestCase;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.base.BasicRemoteRepository;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.lang3.SystemUtils;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static org.easymock.EasyMock.*;

/**
 * RepositoryContentConsumersTest
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class RepositoryContentConsumersTest
    extends TestCase
{

    @Inject
    ApplicationContext applicationContext;

    protected ManagedRepository createRepository( String id, String name, Path location ) throws IOException {
        BasicManagedRepository repo = BasicManagedRepository.newFilesystemInstance(id, name, location.getParent().resolve(id));
        repo.setLocation( location.toAbsolutePath().toUri() );
        return repo;
    }

    protected RemoteRepository createRemoteRepository( String id, String name, String url ) throws URISyntaxException, IOException {
        BasicRemoteRepository repo = BasicRemoteRepository.newFilesystemInstance(id, name, Paths.get("remotes"));
        repo.setLocation( new URI( url ) );
        return repo;
    }

    private RepositoryContentConsumers lookupRepositoryConsumers()
        throws Exception
    {

        ArchivaConfiguration configuration =
            applicationContext.getBean( "archivaConfiguration#test-conf", ArchivaConfiguration.class );

        ArchivaAdministrationStub administrationStub = new ArchivaAdministrationStub( configuration );

        RepositoryContentConsumers consumerUtilStub = new RepositoryContentConsumersStub( administrationStub );

        RepositoryContentConsumers consumerUtil =
            applicationContext.getBean( "repositoryContentConsumers#test", RepositoryContentConsumers.class );
        ApplicationContext context = new MockApplicationContext( consumerUtil.getAvailableKnownConsumers(), //
                                                                 consumerUtil.getAvailableInvalidConsumers() );

        consumerUtilStub.setApplicationContext( context );
        consumerUtilStub.setSelectedInvalidConsumers( consumerUtil.getSelectedInvalidConsumers() );
        consumerUtilStub.setSelectedKnownConsumers( consumerUtil.getSelectedKnownConsumers() );
        consumerUtilStub.setArchivaAdministration( administrationStub );

        assertNotNull( "RepositoryContentConsumers should not be null.", consumerUtilStub );

        return consumerUtilStub;
    }

    @Test
    public void testGetSelectedKnownIds()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedKnownIds[] =
            new String[]{ "create-missing-checksums", "validate-checksum", "validate-signature", "index-content",
                "auto-remove", "auto-rename", "create-archiva-metadata", "duplicate-artifacts" };
//update-db-artifact, create-missing-checksums, update-db-repository-metadata,
//validate-checksum, validate-signature, index-content, auto-remove, auto-rename,
//metadata-updater
        List<String> knownConsumers = consumerutil.getSelectedKnownConsumerIds();
        assertNotNull( "Known Consumer IDs should not be null", knownConsumers );
        assertEquals( "Known Consumer IDs.size " + knownConsumers, expectedKnownIds.length, knownConsumers.size() );

        for ( String expectedId : expectedKnownIds )
        {
            assertTrue( "Known id [" + expectedId + "] exists.", knownConsumers.contains( expectedId ) );
        }
    }

    @Test
    public void testGetSelectedInvalidIds()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedInvalidIds[] = new String[]{ "update-db-bad-content" };

        List<String> invalidConsumers = consumerutil.getSelectedInvalidConsumerIds();
        assertNotNull( "Invalid Consumer IDs should not be null", invalidConsumers );
        assertEquals( "Invalid Consumer IDs.size", expectedInvalidIds.length, invalidConsumers.size() );

        for ( String expectedId : expectedInvalidIds )
        {
            assertTrue( "Invalid id [" + expectedId + "] exists.", invalidConsumers.contains( expectedId ) );
        }
    }

    @Test
    public void testGetSelectedKnownConsumerMap()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedSelectedKnownIds[] =
            new String[]{ "create-missing-checksums", "validate-checksum", "index-content", "auto-remove",
                "auto-rename" };

        Map<String, KnownRepositoryContentConsumer> knownConsumerMap = consumerutil.getSelectedKnownConsumersMap();
        assertNotNull( "Known Consumer Map should not be null", knownConsumerMap );
        assertEquals( "Known Consumer Map.size but " + knownConsumerMap, expectedSelectedKnownIds.length,
                      knownConsumerMap.size() );

        for ( String expectedId : expectedSelectedKnownIds )
        {
            KnownRepositoryContentConsumer consumer = knownConsumerMap.get( expectedId );
            assertNotNull( "Known[" + expectedId + "] should not be null.", consumer );
            assertEquals( "Known[" + expectedId + "].id", expectedId, consumer.getId() );
        }
    }

    @Test
    public void testGetSelectedInvalidConsumerMap()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedSelectedInvalidIds[] = new String[]{ "update-db-bad-content" };

        Map<String, InvalidRepositoryContentConsumer> invalidConsumerMap =
            consumerutil.getSelectedInvalidConsumersMap();
        assertNotNull( "Invalid Consumer Map should not be null", invalidConsumerMap );
        assertEquals( "Invalid Consumer Map.size", expectedSelectedInvalidIds.length, invalidConsumerMap.size() );

        for ( String expectedId : expectedSelectedInvalidIds )
        {
            InvalidRepositoryContentConsumer consumer = invalidConsumerMap.get( expectedId );
            assertNotNull( "Known[" + expectedId + "] should not be null.", consumer );
            assertEquals( "Known[" + expectedId + "].id", expectedId, consumer.getId() );
        }
    }

    @Test
    public void testGetAvailableKnownList()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedKnownIds[] =
            new String[]{ "update-db-artifact", "create-missing-checksums", "update-db-repository-metadata",
                "validate-checksum", "index-content", "auto-remove", "auto-rename", "available-but-unselected" };

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

    @Test
    public void testGetAvailableInvalidList()
        throws Exception
    {
        RepositoryContentConsumers consumerutil = lookupRepositoryConsumers();

        String expectedInvalidIds[] = new String[]{ "update-db-bad-content", "move-to-trash-then-notify" };

        List<InvalidRepositoryContentConsumer> invalidConsumers = consumerutil.getAvailableInvalidConsumers();
        assertNotNull( "invalid consumers should not be null.", invalidConsumers );
        assertEquals( "invalid consumers", expectedInvalidIds.length, invalidConsumers.size() );

        List<String> expectedIds = Arrays.asList( expectedInvalidIds );
        for ( InvalidRepositoryContentConsumer consumer : invalidConsumers )
        {
            assertTrue(
                "Consumer [" + consumer.getId() + "] returned by .getAvailableInvalidConsumers() is unexpected.",
                expectedIds.contains( consumer.getId() ) );
        }
    }

    @Test
    public void testExecution()
        throws Exception
    {
        IMocksControl knownControl = createNiceControl();

        RepositoryContentConsumers consumers = lookupRepositoryConsumers();
        KnownRepositoryContentConsumer selectedKnownConsumer =
            knownControl.createMock( KnownRepositoryContentConsumer.class );

        KnownRepositoryContentConsumer unselectedKnownConsumer =
            createNiceControl().createMock( KnownRepositoryContentConsumer.class );

        consumers.setApplicationContext(
            new MockApplicationContext( Arrays.asList( selectedKnownConsumer, unselectedKnownConsumer ), null ) );

        consumers.setSelectedKnownConsumers( Collections.singletonList( selectedKnownConsumer ) );

        IMocksControl invalidControl = createControl();

        InvalidRepositoryContentConsumer selectedInvalidConsumer =
            invalidControl.createMock( InvalidRepositoryContentConsumer.class );

        InvalidRepositoryContentConsumer unselectedInvalidConsumer =
            createControl().createMock( InvalidRepositoryContentConsumer.class );

        consumers.setApplicationContext(
            new MockApplicationContext( null, Arrays.asList( selectedInvalidConsumer, unselectedInvalidConsumer ) ) );

        consumers.setSelectedInvalidConsumers( Collections.singletonList( selectedInvalidConsumer ) );

        ManagedRepository repo = createRepository( "id", "name", Paths.get( "target/test-repo" ) );
        Path testFile = Paths.get( "target/test-repo/path/to/test-file.txt" );

        Date startTime = new Date( System.currentTimeMillis() );
        startTime.setTime( 12345678 );

        selectedKnownConsumer.beginScan( repo, startTime, false );
        expect( selectedKnownConsumer.getIncludes() ).andReturn( Collections.singletonList( "**/*.txt" ) );
        selectedKnownConsumer.processFile( _OS( "path/to/test-file.txt" ), false );

        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo, startTime, false );
        invalidControl.replay();

        consumers.executeConsumers( repo, testFile, true );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        Path notIncludedTestFile = Paths.get( "target/test-repo/path/to/test-file.xml" );

        selectedKnownConsumer.beginScan( repo, startTime, false );
        expect( selectedKnownConsumer.getExcludes() ).andReturn( Collections.<String>emptyList() );

        expect( selectedKnownConsumer.getIncludes() ).andReturn( Collections.singletonList( "**/*.txt" ) );

        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo, startTime, false );
        selectedInvalidConsumer.processFile( _OS( "path/to/test-file.xml" ), false );
        expect( selectedInvalidConsumer.getId() ).andReturn( "invalid" );
        invalidControl.replay();

        consumers.executeConsumers( repo, notIncludedTestFile, true );

        knownControl.verify();
        invalidControl.verify();

        knownControl.reset();
        invalidControl.reset();

        Path excludedTestFile = Paths.get( "target/test-repo/path/to/test-file.txt" );

        selectedKnownConsumer.beginScan( repo, startTime, false );
        expect( selectedKnownConsumer.getExcludes() ).andReturn( Collections.singletonList( "**/test-file.txt" ) );
        knownControl.replay();

        selectedInvalidConsumer.beginScan( repo, startTime, false );
        selectedInvalidConsumer.processFile( _OS( "path/to/test-file.txt" ), false );
        expect( selectedInvalidConsumer.getId() ).andReturn( "invalid" );
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

    private static <T> Map<String, T> convertToMap( List<T> objects)
    {
        HashMap<String,T> map = new HashMap<>();
        for ( T o : objects )
        {
            map.put( o.toString(), o );
        }
        return map;
    }

    private static <T> Function<List<T>,Map<String,T>> getConversionFunction(Class<T> type) {
        return ts -> convertToMap( ts );
    }

    public class MockApplicationContext
        implements ApplicationContext
    {
        private List<KnownRepositoryContentConsumer> knownRepositoryContentConsumer;

        private List<InvalidRepositoryContentConsumer> invalidRepositoryContentConsumers;

        public MockApplicationContext( List<KnownRepositoryContentConsumer> knownRepositoryContentConsumer,
                                       List<InvalidRepositoryContentConsumer> invalidRepositoryContentConsumers )
        {
            this.knownRepositoryContentConsumer = knownRepositoryContentConsumer;
            this.invalidRepositoryContentConsumers = invalidRepositoryContentConsumers;
        }

        @Override
        public String getApplicationName()
        {
            return "foo";
        }

        @Override
        public AutowireCapableBeanFactory getAutowireCapableBeanFactory()
            throws IllegalStateException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getDisplayName()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getId()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public ApplicationContext getParent()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public long getStartupDate()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean containsBeanDefinition( String beanName )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public int getBeanDefinitionCount()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String[] getBeanDefinitionNames()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String[] getBeanNamesForType( Class type )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String[] getBeanNamesForType( Class type, boolean includeNonSingletons, boolean allowEagerInit )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public <T> T getBean( Class<T> aClass, Object... objects )
            throws BeansException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public <T> Map<String, T> getBeansOfType( Class<T> type )
            throws BeansException
        {
            List<T> list = null;
            if (type == KnownRepositoryContentConsumer.class) {
                list = (List<T>) knownRepositoryContentConsumer;
            } else if (type == InvalidRepositoryContentConsumer.class) {
                list = (List<T>) invalidRepositoryContentConsumers;
            }
            if (list!=null) {
                return getConversionFunction( type ).apply( list );
            }
            throw new UnsupportedOperationException( "Should not have been called" );
        }

        @Override
        public <T> Map<String, T> getBeansOfType( Class<T> type, boolean includeNonSingletons, boolean allowEagerInit )
            throws BeansException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean containsBean( String name )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String[] getAliases( String name )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Object getBean( String name )
            throws BeansException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public <T> T getBean( String name, Class<T> requiredType )
            throws BeansException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Object getBean( String name, Object[] args )
            throws BeansException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Class getType( String name )
            throws NoSuchBeanDefinitionException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isPrototype( String name )
            throws NoSuchBeanDefinitionException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isSingleton( String name )
            throws NoSuchBeanDefinitionException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean isTypeMatch( String name, Class targetType )
            throws NoSuchBeanDefinitionException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public boolean containsLocalBean( String name )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public BeanFactory getParentBeanFactory()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getMessage( String code, Object[] args, String defaultMessage, Locale locale )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getMessage( String code, Object[] args, Locale locale )
            throws NoSuchMessageException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public String getMessage( MessageSourceResolvable resolvable, Locale locale )
            throws NoSuchMessageException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public void publishEvent( ApplicationEvent event )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Resource[] getResources( String locationPattern )
            throws IOException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public ClassLoader getClassLoader()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Resource getResource( String location )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public <T> T getBean( Class<T> tClass )
            throws BeansException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Map<String, Object> getBeansWithAnnotation( Class<? extends Annotation> aClass )
            throws BeansException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public <A extends Annotation> A findAnnotationOnBean( String s, Class<A> aClass )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        @Override
        public Environment getEnvironment()
        {
            return null;
        }

        @Override
        public String[] getBeanNamesForAnnotation( Class<? extends Annotation> aClass )
        {
            return new String[0];
        }

        @Override
        public void publishEvent( Object o )
        {
            // no op
        }

        @Override
        public String[] getBeanNamesForType( ResolvableType resolvableType )
        {
            return new String[0];
        }

        @Override
        public boolean isTypeMatch( String s, ResolvableType resolvableType )
            throws NoSuchBeanDefinitionException
        {
            return false;
        }
    }
}
