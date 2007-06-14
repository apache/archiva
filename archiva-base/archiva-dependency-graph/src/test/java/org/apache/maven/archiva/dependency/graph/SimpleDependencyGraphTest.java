package org.apache.maven.archiva.dependency.graph;

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

import org.apache.maven.archiva.dependency.DependencyGraphFactory;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.VersionedReference;

import java.util.ArrayList;
import java.util.List;

/**
 * SimpleDependencyGraphTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class SimpleDependencyGraphTest
    extends AbstractDependencyGraphFactoryTestCase
{
    public void testResolveDependenciesBasic() throws GraphTaskException
    {
        MemoryRepositoryDependencyGraphBuilder graphBuilder = new MemoryRepositoryDependencyGraphBuilder();
        MemoryRepository repository = new SimpleMemoryRepository();
        graphBuilder.setMemoryRepository( repository );

        // Create the factory, and add the test resolver.
        DependencyGraphFactory factory = new DependencyGraphFactory();
        factory.setGraphBuilder( graphBuilder );
        factory.setDesiredScope( DependencyScope.TEST );

        // Get the model to resolve from
        VersionedReference rootRef = toVersionedReference( "org.apache.maven.archiva:archiva-commons:1.0" );

        // Perform the resolution.
        DependencyGraph graph = factory.getGraph( rootRef );

        // Test the results.
        assertNotNull( "Graph shouldn't be null.", graph );

        List expectedNodes = new ArrayList();
        expectedNodes.add( "org.apache.maven.archiva:archiva-commons:1.0::pom" );
        expectedNodes.add( "org.codehaus.plexus:plexus-digest:1.0::jar" );
        expectedNodes.add( "junit:junit:3.8.1::jar" );
        assertNodes( graph, expectedNodes );

        List expectedEdges = new ArrayList();
        expectedEdges.add( new ExpectedEdge( "org.apache.maven.archiva:archiva-commons:1.0::pom",
                                             "org.codehaus.plexus:plexus-digest:1.0::jar" ) );
        expectedEdges.add( new ExpectedEdge( "org.codehaus.plexus:plexus-digest:1.0::jar", "junit:junit:3.8.1::jar" ) );

        assertEdges( graph, expectedEdges );
    }
}
