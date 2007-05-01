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
import org.apache.maven.archiva.model.DependencyTree;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Iterator;

/**
 * ProjectModelExpressionExpanderTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModelExpressionExpanderTest
    extends PlexusTestCase
{
    private ProjectModelExpressionFilter lookupExpression() throws Exception
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

        DependencyTree depTree = new DependencyTree();
        
        depTree.addDependencyEdge( model.asDependency(), createDependency( "org.apache.maven.archiva", "archiva-model", "${archiva.version}" ) );
        depTree.addDependencyEdge( model.asDependency(), createDependency( "org.apache.maven.archiva", "archiva-common", "${archiva.version}" ) );
        depTree.addDependencyEdge( model.asDependency(), createDependency( "org.apache.maven.archiva", "archiva-indexer", "${archiva.version}" ) );

        model.setDependencyTree( depTree );
        
        model.addProperty( "archiva.version", "1.0-SNAPSHOT" );

        ProjectModelExpressionFilter filter = lookupExpression();
        
        model = filter.filter( model );

        assertNotNull( model );
        assertEquals( "Group ID", "org.apache.maven.archiva", model.getGroupId() );
        assertEquals( "Artifact ID", "archiva-test-project", model.getArtifactId() );
        assertEquals( "Version", "1.0-SNAPSHOT", model.getVersion() );
        assertNotNull( "DependencyTree", model.getDependencyTree() );
        assertNotNull( "DependencyTree.dependencies", model.getDependencyTree().getDependencyNodes() );
        assertEquals( "Dependencies Size", 4, model.getDependencyTree().getDependencyNodes().size() );

        Iterator it = model.getDependencyTree().getDependencyNodes().iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
            assertEquals( "Dependency [" + dep.getArtifactId() + "] Group ID", "org.apache.maven.archiva", dep
                .getGroupId() );
            assertEquals( "Dependency [" + dep.getArtifactId() + "] Version", "1.0-SNAPSHOT", dep.getVersion() );
        }
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
