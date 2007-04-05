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

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;
import org.apache.maven.archiva.repository.project.filters.EffectiveProjectModelFilter;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;
import org.apache.maven.archiva.repository.project.resolvers.RepositoryProjectResolver;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * EffectiveProjectModelFilterTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class EffectiveProjectModelFilterTest
    extends PlexusTestCase
{
    private static final String DEFAULT_REPOSITORY = "src/test/repositories/default-repository";
    
    private EffectiveProjectModelFilter lookupEffective() throws Exception
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

    private ProjectModelResolver createDefaultRepositoryResolver()
    {
        File defaultRepoDir = new File( getBasedir(), DEFAULT_REPOSITORY );

        ArchivaRepository repo = new ArchivaRepository( "defaultTestRepo", "Default Test Repo", "file://"
            + defaultRepoDir.getAbsolutePath() );

        RepositoryProjectResolver resolver = new RepositoryProjectResolver( repo );

        return resolver;
    }

    public void testBuildEffectiveProject()
        throws Exception
    {
        EffectiveProjectModelFilter filter = lookupEffective();
        
        filter.addProjectModelResolver( createDefaultRepositoryResolver() );

        ArchivaProjectModel startModel = createArchivaProjectModel( DEFAULT_REPOSITORY
            + "/org/apache/maven/archiva/archiva-model/1.0-SNAPSHOT/archiva-model-1.0-SNAPSHOT.pom" );

        ArchivaProjectModel effectiveModel = filter.filter( startModel );

        ArchivaProjectModel expectedModel = createArchivaProjectModel( "src/test/effective-poms/"
            + "/archiva-model-effective.pom" );

        assertModel( expectedModel, effectiveModel );
    }

    private void assertModel( ArchivaProjectModel expectedModel, ArchivaProjectModel effectiveModel )
    {
        assertEquals( "Equivalent Models", expectedModel, effectiveModel );

        assertContainsSame( "Individuals", expectedModel.getIndividuals(), effectiveModel.getIndividuals() );
        dumpDependencyList( "Expected", expectedModel.getDependencies() );
        dumpDependencyList( "Effective", effectiveModel.getDependencies() );
        assertContainsSame( "Dependencies", expectedModel.getDependencies(), effectiveModel.getDependencies() );
        assertContainsSame( "DependencyManagement", expectedModel.getDependencyManagement(), effectiveModel
            .getDependencyManagement() );
    }
    
    private void dumpDependencyList( String type, List deps )
    {
        System.out.println( ".\\ [" + type + "] Dependency List (size:" + deps.size() + ") \\.________________" );
        Iterator it = deps.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
            System.out.println( "  " + toDependencyKey( dep ) );
        }
        System.out.println( "" );
    }
    
    private String toDependencyKey( Dependency dep )
    {
        return "[" + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + ":" + dep.getClassifier()
            + ":" + dep.getType() + "]";
    }

    private void assertContainsSame( String listId, List expectedList, List effectiveList )
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

        Iterator it = expectedList.iterator();
        while ( it.hasNext() )
        {
            Object o = it.next();
            assertTrue( "Should exist in Effective [" + listId + "] list: " + o, effectiveList.contains( o ) );
        }
    }
}
