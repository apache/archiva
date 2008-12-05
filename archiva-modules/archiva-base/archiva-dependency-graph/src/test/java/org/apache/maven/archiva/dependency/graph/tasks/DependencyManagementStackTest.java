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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.tasks.DependencyManagementStack.Rules;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;

import junit.framework.TestCase;

/**
 * DependencyManagementStackTest 
 *
 * @version $Id$
 */
public class DependencyManagementStackTest
    extends TestCase
{
    public DependencyGraphNode toNode( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ":" );
        assertEquals( "toNode(" + key + ") requires 5 parts", 5, parts.length );

        ArtifactReference ref = new ArtifactReference();
        ref.setGroupId( parts[0] );
        ref.setArtifactId( parts[1] );
        ref.setVersion( parts[2] );
        ref.setClassifier( parts[3] );
        ref.setType( parts[4] );

        return new DependencyGraphNode( ref );
    }

    protected Dependency toDependency( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );

        assertEquals( "Dependency key [" + key + "] should be 5 parts.", 5, parts.length );

        Dependency dep = new Dependency();

        dep.setGroupId( parts[0] );
        dep.setArtifactId( parts[1] );
        dep.setVersion( parts[2] );
        dep.setClassifier( parts[3] );
        dep.setType( parts[4] );

        return dep;
    }

    public void testPushPopSimple()
    {
        DependencyGraphNode node = toNode( "org.apache.maven.archiva:depmanstack-testcase:1.0::jar" );
        Dependency dep = toDependency( "junit:junit:3.8.1::jar" );
        dep.setScope( "test" );
        node.addDependencyManagement( dep );

        DependencyManagementStack stack = new DependencyManagementStack();
        stack.push( node );
        DependencyGraphNode oldnode = stack.pop();
        assertEquals( "added node to old node", node, oldnode );
    }

    public void testPushPopTwoDeep()
    {
        DependencyManagementStack stack = new DependencyManagementStack();
        Dependency dep;

        // top node.
        DependencyGraphNode projectNode = toNode( "org.apache.maven.archiva:depmanstack-testcase:1.0::jar" );
        dep = toDependency( "junit:junit:3.8.1::jar" );
        dep.setScope( "test" );
        projectNode.addDependencyManagement( dep );
        stack.push( projectNode );

        // direct node.
        DependencyGraphNode directNode = toNode( "org.apache.maven.archiva:depmanstack-common:1.0::jar" );
        dep = toDependency( "junit:junit:3.7::jar" );
        dep.setScope( "test" );
        directNode.addDependencyManagement( dep );
        stack.push( directNode );

        // transitive node.
        DependencyGraphNode transNode = toNode( "org.apache.maven.archiva:depmanstack-model:1.0::jar" );
        dep = toDependency( "junit:junit:3.7::jar" );
        transNode.addDependencyManagement( dep );
        stack.push( transNode );

        // Test it
        assertEquals( "popped node is trans node", transNode, stack.pop() );
        assertEquals( "popped node is direct node", directNode, stack.pop() );
        assertEquals( "popped node is project node", projectNode, stack.pop() );
    }

    public void testApplyNodeVersionParentWins()
    {
        DependencyManagementStack stack = new DependencyManagementStack();
        Dependency dep;

        // top node.
        DependencyGraphNode projectNode = toNode( "org.apache.maven.archiva:depmanstack-testcase:1.0::jar" );
        dep = toDependency( "junit:junit:3.8.1::jar" );
        dep.setScope( "test" );
        projectNode.addDependencyManagement( dep );
        stack.push( projectNode );

        // direct node.
        DependencyGraphNode directNode = toNode( "org.apache.maven.archiva:depmanstack-common:1.0::jar" );
        dep = toDependency( "junit:junit:3.7::jar" );
        dep.setScope( "test" );
        directNode.addDependencyManagement( dep );
        stack.push( directNode );

        // transitive node.
        DependencyGraphNode transNode = toNode( "org.apache.maven.archiva:depmanstack-model:1.0::jar" );
        dep = toDependency( "junit:junit:3.7.1::jar" );
        transNode.addDependencyManagement( dep );
        stack.push( transNode );

        // Test it
        DependencyGraphNode junitNode = toNode( "junit:junit:1.0::jar" );

        assertRules( "junit (lvl:trans)", stack, junitNode, "3.8.1", "test", null );
        stack.pop();
        assertRules( "junit (lvl:direct)", stack, junitNode, "3.8.1", "test", null );
        stack.pop();
        assertRules( "junit (lvl:project)", stack, junitNode, "3.8.1", "test", null );
    }

    /**
     * This test is based off of Carlos Sanchez's depman example use case.
     *
     * In a simple project chain of A:1.0 -&gt; B:1.0 -&gt; C:1.0 -&gt; D:1.0
     * If B:1.0 has a dependency management section stating dep D should be version 2.0
     * Then the dep D when viewed from A should be version 2.0 
     */
    public void testApplyNodeVersionCarlosABCD()
    {
        DependencyManagementStack stack = new DependencyManagementStack();
        Dependency dep;

        // project node, A
        DependencyGraphNode nodeA = toNode( "org.apache.maven.archiva:carlos-A:1.0::jar" );
        stack.push( nodeA );

        // sub node, B
        DependencyGraphNode nodeB = toNode( "org.apache.maven.archiva:carlos-B:1.0::jar" );
        dep = toDependency( "org.apache.maven.archiva:carlos-D:2.0::jar" );
        nodeB.addDependencyManagement( dep );
        stack.push( nodeB );

        // sub node, C
        DependencyGraphNode nodeC = toNode( "org.apache.maven.archiva:carlos-C:1.0::jar" );
        stack.push( nodeC );

        // sub node, D
        // Not added to the stack, as this is the node that is having the rules applied to it.
        DependencyGraphNode nodeD = toNode( "org.apache.maven.archiva:carlos-D:1.0::jar" );

        // Test it
        assertRules( "node D (lvl:C)", stack, nodeD, "2.0", null, null );
        stack.pop();
        assertRules( "node D (lvl:B)", stack, nodeD, "2.0", null, null );
        stack.pop();
        assertNoRules( "node D (lvl:A)", stack, nodeD, "2.0", null, null );
    }

    /**
     * Test for expected rules, that should be enforced for the provided node.
     * NOTE: This test will update the node.artifact.version to whatever is stated in the rules.
     */
    private void assertRules( String msg, DependencyManagementStack stack, DependencyGraphNode node,
                              String expectedVersion, String expectedScope, String expectedExclusions[] )
    {
        Rules rules = stack.getRules( node );
        assertNotNull( msg + " rules should not be null.", rules );

        node.getArtifact().setVersion( rules.artifact.getVersion() );

        assertEquals( msg + ": version", expectedVersion, rules.artifact.getVersion() );
        assertEquals( msg + ": scope", expectedScope, rules.scope );

        if ( expectedExclusions != null )
        {
            // TODO: test for exclusion settings.
        }
    }

    /**
     * Test for when there are no rules being enforced for the provided node.
     * Similar to assertRules() above.
     */
    private void assertNoRules( String msg, DependencyManagementStack stack, DependencyGraphNode node,
                                String expectedVersion, String expectedScope, String expectedExclusions[] )
    {
        Rules rules = stack.getRules( node );
        assertNull( msg + " rules should be null.", rules );

        assertEquals( msg + ": version", expectedVersion, node.getArtifact().getVersion() );

        if ( expectedExclusions != null )
        {
            // TODO: test for exclusion settings.
        }
    }
}
