package org.apache.maven.archiva.repository.project.filters;

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

import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.repository.AbstractRepositoryLayerTestCase;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;
import org.apache.maven.archiva.repository.project.ProjectModelResolverFactory;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;
import org.apache.maven.archiva.repository.project.resolvers.ManagedRepositoryProjectResolver;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * EffectiveProjectModelFilterTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class EffectiveProjectModelFilterTest
    extends AbstractRepositoryLayerTestCase
{
    private static final String DEFAULT_REPOSITORY = "src/test/repositories/default-repository";

    private EffectiveProjectModelFilter lookupEffective()
        throws Exception
    {
        return (EffectiveProjectModelFilter) lookup( ProjectModelFilter.class, "effective" );
    }

    private ArchivaProjectModel createArchivaProjectModel( String path )
        throws ProjectModelException
    {
        ProjectModelReader reader = new ProjectModel400Reader();

        File pomFile = new File( getBasedir(), path );

        return reader.read( pomFile );
    }

    private ProjectModelResolver createDefaultRepositoryResolver() throws Exception
    {
        File defaultRepoDir = new File( getBasedir(), DEFAULT_REPOSITORY );

        ManagedRepositoryContent repo = createManagedRepositoryContent( "defaultTestRepo", "Default Test Repo", defaultRepoDir, "default" );

        ProjectModelReader reader = new ProjectModel400Reader();
        ManagedRepositoryProjectResolver resolver = new ManagedRepositoryProjectResolver( repo, reader );

        return resolver;
    }

    public void testBuildEffectiveProject()
        throws Exception
    {
        initTestResolverFactory();
        EffectiveProjectModelFilter filter = lookupEffective();

        ArchivaProjectModel startModel = createArchivaProjectModel( DEFAULT_REPOSITORY
            + "/org/apache/maven/archiva/archiva-model/1.0-SNAPSHOT/archiva-model-1.0-SNAPSHOT.pom" );

        ArchivaProjectModel effectiveModel = filter.filter( startModel );

        ArchivaProjectModel expectedModel = createArchivaProjectModel( "src/test/expected-poms/"
            + "/archiva-model-effective.pom" );

        assertModel( expectedModel, effectiveModel );
    }

    /**
     * [MRM-510] In Repository Browse, the first unique snapshot version clicked is getting persisted in the 
     * request resulting to 'version does not match' error
     * 
     * The purpose of this test is ensure that timestamped SNAPSHOTS do not cache improperly, and each timestamped
     * pom can be loaded through the effective project filter correctly.
     */
    public void testBuildEffectiveSnapshotProject()
        throws Exception
    {
        initTestResolverFactory();
        EffectiveProjectModelFilter filter = lookupEffective();

        String axisVersions[] = new String[] {
            "1.3-20070725.210059-1",
            "1.3-20070725.232304-2",
            "1.3-20070726.053327-3",
            "1.3-20070726.173653-5",
            "1.3-20070727.113106-7",
            "1.3-20070728.053229-10",
            "1.3-20070728.112043-11",
            "1.3-20070729.171937-16",
            "1.3-20070730.232112-20",
            "1.3-20070731.113304-21",
            "1.3-20070731.172936-22",
            "1.3-20070802.113139-29" };

        for ( int i = 0; i < axisVersions.length; i++ )
        {
            assertTrue( "Version should be a unique snapshot.", VersionUtil.isUniqueSnapshot( axisVersions[i] ) );

            ArchivaProjectModel initialModel = createArchivaProjectModel( DEFAULT_REPOSITORY
                + "/org/apache/axis2/axis2/1.3-SNAPSHOT/axis2-" + axisVersions[i] + ".pom" );

            // This is the process that ProjectModelToDatabaseConsumer uses, so we mimic it here.
            // This logic is related to the MRM-510 jira.
            String baseVersion = VersionUtil.getBaseVersion( axisVersions[i] );

            assertEquals( "Base Version <" + baseVersion + "> of filename <" + axisVersions[i]
                + "> should be equal to what is in model.", initialModel.getVersion(), baseVersion );

            initialModel.setVersion( axisVersions[i] );

            assertEquals( "Unique snapshot versions of initial model should be equal.", axisVersions[i], initialModel
                .getVersion() );

            ArchivaProjectModel effectiveModel = filter.filter( initialModel );

            assertEquals( "Unique snapshot versions of initial model should be equal.", axisVersions[i], initialModel
                .getVersion() );
            assertEquals( "Unique snapshot versions of filtered/effective model should be equal.", axisVersions[i],
                          effectiveModel.getVersion() );
        }
    }

    private ProjectModelResolverFactory initTestResolverFactory()
        throws Exception
    {
        ProjectModelResolverFactory resolverFactory = (ProjectModelResolverFactory) lookup( ProjectModelResolverFactory.class );

        resolverFactory.getCurrentResolverStack().clearResolvers();
        resolverFactory.getCurrentResolverStack().addProjectModelResolver( createDefaultRepositoryResolver() );

        return resolverFactory;
    }

    private void assertModel( ArchivaProjectModel expectedModel, ArchivaProjectModel effectiveModel )
    {
        assertEquals( "Equivalent Models", expectedModel, effectiveModel );

        assertContainsSameIndividuals( "Individuals", expectedModel.getIndividuals(), effectiveModel.getIndividuals() );
        dumpDependencyList( "Expected", expectedModel.getDependencies() );
        dumpDependencyList( "Effective", effectiveModel.getDependencies() );
        assertContainsSameDependencies( "Dependencies", expectedModel.getDependencies(), effectiveModel
            .getDependencies() );
        assertContainsSameDependencies( "DependencyManagement", expectedModel.getDependencyManagement(), effectiveModel
            .getDependencyManagement() );
    }

    private void dumpDependencyList( String type, List<Dependency> deps )
    {
        if ( deps == null )
        {
            System.out.println( " Dependencies [" + type + "] is null." );
            return;
        }

        if ( deps.isEmpty() )
        {
            System.out.println( " Dependencies [" + type + "] dependency list is empty." );
            return;
        }

        System.out.println( ".\\ [" + type + "] Dependency List (size:" + deps.size() + ") \\.________________" );
        Iterator<Dependency> it = deps.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = it.next();
            System.out.println( "  " + Dependency.toKey( dep ) );
        }
        System.out.println( "" );
    }

    private void assertEquivalentLists( String listId, List<?> expectedList, List<?> effectiveList )
    {
        if ( ( expectedList == null ) && ( effectiveList == null ) )
        {
            return;
        }

        if ( ( expectedList == null ) && ( effectiveList != null ) )
        {
            fail( "Effective [" + listId + "] List is instantiated, while expected List is null." );
        }

        if ( ( expectedList != null ) && ( effectiveList == null ) )
        {
            fail( "Effective [" + listId + "] List is null, while expected List is instantiated." );
        }

        assertEquals( "[" + listId + "] List Size", expectedList.size(), expectedList.size() );
    }

    private void assertContainsSameIndividuals( String listId, List<Individual> expectedList,
                                                List<Individual> effectiveList )
    {
        assertEquivalentLists( listId, expectedList, effectiveList );

        Map<String, Individual> expectedMap = getIndividualsMap( expectedList );
        Map<String, Individual> effectiveMap = getIndividualsMap( effectiveList );

        Iterator<String> it = expectedMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = (String) it.next();

            assertTrue( "Should exist in Effective [" + listId + "] list: " + key, effectiveMap.containsKey( key ) );
        }
    }

    private void assertContainsSameDependencies( String listId, List<Dependency> expectedList,
                                                 List<Dependency> effectiveList )
    {
        assertEquivalentLists( listId, expectedList, effectiveList );

        Map<String, Dependency> expectedMap = getDependencyMap( expectedList );
        Map<String, Dependency> effectiveMap = getDependencyMap( effectiveList );

        Iterator<String> it = expectedMap.keySet().iterator();
        while ( it.hasNext() )
        {
            String key = it.next();

            assertTrue( "Should exist in Effective [" + listId + "] list: " + key, effectiveMap.containsKey( key ) );
        }
    }

    private Map<String, Individual> getIndividualsMap( List<Individual> individuals )
    {
        Map<String, Individual> map = new HashMap<String, Individual>();
        Iterator<Individual> it = individuals.iterator();
        while ( it.hasNext() )
        {
            Individual individual = it.next();
            String key = individual.getEmail();
            map.put( key, individual );
        }
        return map;
    }

    private Map<String, Dependency> getDependencyMap( List<Dependency> deps )
    {
        Map<String, Dependency> map = new HashMap<String, Dependency>();
        Iterator<Dependency> it = deps.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = it.next();
            String key = Dependency.toVersionlessKey( dep );
            map.put( key, dep );
        }
        return map;
    }
}
