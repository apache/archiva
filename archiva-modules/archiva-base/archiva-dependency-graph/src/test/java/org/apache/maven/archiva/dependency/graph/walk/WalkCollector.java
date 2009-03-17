package org.apache.maven.archiva.dependency.graph.walk;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.model.ArtifactReference;

import java.util.ArrayList;
import java.util.List;

class WalkCollector
    implements DependencyGraphVisitor
{
    private List<String> walkPath = new ArrayList<String>();

    private int countDiscoverGraph = 0;

    private int countFinishGraph = 0;

    private int countDiscoverNode = 0;

    private int countFinishNode = 0;

    private int countDiscoverEdge = 0;

    private int countFinishEdge = 0;

    public void discoverEdge( DependencyGraphEdge edge )
    {
        countDiscoverEdge++;
    }

    public void discoverGraph( DependencyGraph graph )
    {
        countDiscoverGraph++;
    }

    public void discoverNode( DependencyGraphNode node )
    {
        countDiscoverNode++;
        walkPath.add( ArtifactReference.toKey( node.getArtifact() ) );
    }

    public void finishEdge( DependencyGraphEdge edge )
    {
        countFinishEdge++;
    }

    public void finishGraph( DependencyGraph graph )
    {
        countFinishGraph++;
    }

    public void finishNode( DependencyGraphNode node )
    {
        countFinishNode++;
    }

    public List<String> getCollectedPath()
    {
        return walkPath;
    }

    public int getCountDiscoverEdge()
    {
        return countDiscoverEdge;
    }

    public int getCountDiscoverGraph()
    {
        return countDiscoverGraph;
    }

    public int getCountDiscoverNode()
    {
        return countDiscoverNode;
    }

    public int getCountFinishEdge()
    {
        return countFinishEdge;
    }

    public int getCountFinishGraph()
    {
        return countFinishGraph;
    }

    public int getCountFinishNode()
    {
        return countFinishNode;
    }

}