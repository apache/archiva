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
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelWriter;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;
import org.apache.maven.archiva.repository.project.writers.ProjectModel400Writer;
import org.apache.maven.archiva.xml.XMLException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ProjectModelExpressionExpanderTest 
 *
 * @version $Id$
 */
public class ProjectModelExpressionExpanderTest
    extends PlexusInSpringTestCase
{
    private static final String DEFAULT_REPOSITORY = "src/test/repositories/default-repository";

    private ProjectModelExpressionFilter lookupExpression()
        throws Exception
    {
        return (ProjectModelExpressionFilter) lookup( ProjectModelFilter.class, "expression" );
    }

    public void testExpressionEvaluation()
        throws Exception
    {
        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( "org.apache.maven.archiva" );
        model.setArtifactId( "archiva-test-project" );
        model.setVersion( "1.0-SNAPSHOT" );

        List<Dependency> deps = new ArrayList<Dependency>();

        deps.add( createDependency( "org.apache.maven.archiva", "archiva-model", "${archiva.version}" ) );
        deps.add( createDependency( "org.apache.maven.archiva", "archiva-common", "${archiva.version}" ) );
        deps.add( createDependency( "org.apache.maven.archiva", "archiva-indexer", "${archiva.version}" ) );

        model.setDependencies( deps );

        model.addProperty( "archiva.version", "1.0-SNAPSHOT" );

        ProjectModelExpressionFilter filter = lookupExpression();

        model = filter.filter( model );

        assertNotNull( model );
        assertEquals( "Group ID", "org.apache.maven.archiva", model.getGroupId() );
        assertEquals( "Artifact ID", "archiva-test-project", model.getArtifactId() );
        assertEquals( "Version", "1.0-SNAPSHOT", model.getVersion() );
        assertNotNull( "Dependencies", model.getDependencies() );
        assertEquals( "Dependencies Size", 3, model.getDependencies().size() );

        Iterator<Dependency> it = model.getDependencies().iterator();
        while ( it.hasNext() )
        {
            Dependency dep = it.next();
            assertEquals( "Dependency [" + dep.getArtifactId() + "] Group ID", "org.apache.maven.archiva", dep
                .getGroupId() );
            assertEquals( "Dependency [" + dep.getArtifactId() + "] Version", "1.0-SNAPSHOT", dep.getVersion() );
        }
    }
    
    /**
     * [MRM-487] pom version is not resolved
     * [MRM-488] properties in pom are not resolved (at least while browsing)
     * 
     * This is to ensure that any expression within the pom is evaluated properly.
     */
    public void testExpressionHell()
        throws Exception
    {
        ProjectModelExpressionFilter filter = lookupExpression();

        ArchivaProjectModel initialModel = createArchivaProjectModel( DEFAULT_REPOSITORY
            + "/org/apache/maven/test/2.0.4-SNAPSHOT/test-2.0.4-SNAPSHOT.pom" );

        ArchivaProjectModel filteredModel = filter.filter( initialModel );

        // Dump the evaluated model to xml
        String evaluatedModelText = toModelText( filteredModel );

        // Test xml buffer for the existance of an unevaluated expression.
        boolean foundUnevaluated = false;
        if ( evaluatedModelText.indexOf( "${" ) != ( -1 ) )
        {
            System.err.println( "Found Expression:\n" + evaluatedModelText );
            foundUnevaluated = true;
        }
        
        if ( foundUnevaluated )
        {
            fail( "Found Unevaluated Expression. (see System.err for details)" );
        }
    }

    private String toModelText( ArchivaProjectModel model )
        throws ProjectModelException, IOException
    {
        StringWriter strWriter = new StringWriter();

        ProjectModelWriter modelWriter = new ProjectModel400Writer();
        modelWriter.write( model, strWriter );

        return strWriter.toString();
    }

    private ArchivaProjectModel createArchivaProjectModel( String path )
        throws XMLException
    {
        ProjectModelReader reader = new ProjectModel400Reader();

        File pomFile = new File( getBasedir(), path );

        return reader.read( pomFile );
    }

    private Dependency createDependency( String groupId, String artifactId, String version )
    {
        Dependency dep = new Dependency();

        dep.setGroupId( groupId );
        dep.setArtifactId( artifactId );
        dep.setVersion( version );
        dep.setTransitive( false );

        return dep;
    }

}
