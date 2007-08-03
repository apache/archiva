package org.apache.maven.archiva.dependency.graph.tasks;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.PotentialCyclicEdgeProducer;
import org.apache.maven.archiva.dependency.graph.functors.ToArtifactReferenceTransformer;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * RefineConflictsTask 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.dependency.graph.GraphTask"
 *      role-hint="refine-conflicts"
 *      instantiation-strategy="per-lookup"
 */
public class RefineConflictsTask
    implements GraphTask, PotentialCyclicEdgeProducer
{

    public void executeTask( DependencyGraph graph )
    {
        Iterator it;
        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        RefineConflictsVisitor refineConflictsVisitor = new RefineConflictsVisitor();
        
        MultiValueMap depMap = new MultiValueMap();

        // Identify deps that need to be resolved.
        it = graph.getNodes().iterator();
        while ( it.hasNext() )
        {
            DependencyGraphNode node = (DependencyGraphNode) it.next();
            String key = DependencyGraphKeys.toManagementKey( node.getArtifact() );
            // This will add this node to the specified key, not replace a previous one.
            depMap.put( key, node );
        }

        // Process those depMap entries with more than 1 value. 
        ToArtifactReferenceTransformer nodeToArtifact = new ToArtifactReferenceTransformer();

        it = depMap.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            Collection nodes = (Collection) entry.getValue();
            if ( nodes.size() > 1 )
            {
                List conflictingArtifacts = new ArrayList();
                conflictingArtifacts.addAll( nodes );
                CollectionUtils.transform( conflictingArtifacts, nodeToArtifact );

                refineConflictsVisitor.resetConflictingArtifacts();
                refineConflictsVisitor.addAllConflictingArtifacts( conflictingArtifacts );
                walker.visit( graph, refineConflictsVisitor );
            }
        }
    }

    public String getTaskId()
    {
        return "refine-conflicts";
    }
}
