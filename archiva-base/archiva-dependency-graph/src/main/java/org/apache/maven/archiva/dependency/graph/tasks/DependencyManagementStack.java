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

import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Exclusion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * DependencyManagementStack 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyManagementStack
{
    public class Rules
    {
        public ArtifactReference artifact;

        public String scope;

        public Set exclusions = new HashSet();

        public void addAllExclusions( List depExclusions )
        {
            Iterator it = depExclusions.iterator();
            while ( it.hasNext() )
            {
                Exclusion ref = (Exclusion) it.next();
                String key = DependencyGraphKeys.toManagementKey( ref );
                exclusions.add( key );
            }
        }
    }

    private Stack depmanStack = new Stack();

    private Map depMap = new HashMap();

    private void generateDepMap()
    {
        depMap.clear();

        // Using a reverse iterator to ensure that we read the
        // stack from last in to first in
        ReverseListIterator it = new ReverseListIterator( depmanStack );
        while ( it.hasNext() )
        {
            DependencyGraphNode node = (DependencyGraphNode) it.next();

            addDependencies( node.getDependencyManagement() );
        }
    }

    private void addDependencies( List dependencies )
    {
        Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
            String key = DependencyGraphKeys.toManagementKey( dep );

            Rules merged = (Rules) depMap.get( key );
            if ( merged == null )
            {
                // New map entry.
                merged = new Rules();
                merged.artifact = new ArtifactReference();
                merged.artifact.setGroupId( dep.getGroupId() );
                merged.artifact.setArtifactId( dep.getArtifactId() );
                merged.artifact.setClassifier( dep.getClassifier() );
                merged.artifact.setType( dep.getType() );
            }

            merged.artifact.setVersion( dep.getVersion() );
            if ( StringUtils.isNotBlank( dep.getScope() ) )
            {
                merged.scope = dep.getScope();
            }

            merged.addAllExclusions( dep.getExclusions() );

            depMap.put( key, merged );
        }
    }

    public Rules getRules( DependencyGraphEdge edge )
    {
        return getRules( edge.getNodeTo() );
    }

    public Rules getRules( DependencyGraphNode node )
    {
        return getRules( node.getArtifact() );
    }

    public Rules getRules( ArtifactReference ref )
    {
        String key = DependencyGraphKeys.toManagementKey( ref );
        return (Rules) depMap.get( key );
    }

    public void push( DependencyGraphNode node )
    {
        depmanStack.push( node );
        generateDepMap();
    }

    public DependencyGraphNode pop()
    {
        DependencyGraphNode node = (DependencyGraphNode) depmanStack.pop();
        generateDepMap();
        return node;
    }

    public void reset()
    {
        depmanStack.clear();
        depMap.clear();
    }
}
