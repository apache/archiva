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

import org.apache.commons.collections.Predicate;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.functors.EdgeFromPredicate;
import org.apache.maven.archiva.dependency.graph.walk.BaseVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;
import org.apache.maven.archiva.model.DependencyScope;

import java.util.Stack;

/**
 * UpdateScopesVisitor 
 *
 * @version $Id$
 */
public class UpdateScopesVisitor
    extends BaseVisitor
    implements DependencyGraphVisitor
{
    private Stack scopeStack;

    private Predicate rootEdgePredicate;

    public UpdateScopesVisitor()
    {
        scopeStack = new Stack();
        // Default setting.
        scopeStack.add( DependencyScope.COMPILE );
    }

    public void discoverGraph( DependencyGraph graph )
    {
        super.discoverGraph( graph );
        rootEdgePredicate = new EdgeFromPredicate( graph.getRootNode() );
    }

    public void discoverEdge( DependencyGraphEdge edge )
    {
        super.discoverEdge( edge );
        
        String scope = edge.getScope();

        if ( !rootEdgePredicate.evaluate( edge ) )
        {
            // Not a root edge.  Set the scope.
            scope = (String) scopeStack.peek();
            edge.setScope( scope );
        }
        
        // Push the scope used onto the stack.
        scopeStack.push( scope );
    }

    public void finishEdge( DependencyGraphEdge edge )
    {
        super.finishEdge( edge );

        scopeStack.pop();
    }
}
