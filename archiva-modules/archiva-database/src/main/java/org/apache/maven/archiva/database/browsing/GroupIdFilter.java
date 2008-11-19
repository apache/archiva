package org.apache.maven.archiva.database.browsing;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * GroupIdFilter - utility methods for filtering groupIds. 
 *
 * @version $Id$
 */
public class GroupIdFilter
{
    private static final String GROUP_SEPARATOR = ".";

    /**
     * <p>
     * Filter out excessive groupId naming. (to provide a tree-ish view of the list of groupIds).
     * </p>
     * 
     * <pre>
     *  // Input List
     *  commons-lang
     *  com.jsch
     *  org.apache.apache
     *  org.apache.maven
     *  org.codehaus.modello
     *  // Filtered List
     *  commons-lang
     *  com.jsch
     *  org
     * </pre>
     * 
     * <pre>
     *  // Input List
     *  commons-lang
     *  commons-io
     *  commons-pool
     *  com.jsch
     *  com.jsch.lib
     *  com.jsch.providers
     *  org.apache.apache
     *  org.apache.maven
     *  org.apache.maven.archiva
     *  org.apache.maven.shared
     *  // Filtered List
     *  commons-lang
     *  commons-io
     *  commons-pool
     *  com.jsch
     *  org.apache
     * </pre>
     * 
     * @param groups the list of groupIds.
     * @return
     */
    public static List<String> filterGroups( List<String> groups )
    {
        GroupTreeNode tree = buildGroupTree( groups );
        return collateGroups( tree );
    }

    public static GroupTreeNode buildGroupTree( List<String> groups )
    {
        GroupTreeNode rootNode = new GroupTreeNode();

        // build a tree structure
        for ( String groupId : groups )
        {
            StringTokenizer tok = new StringTokenizer( groupId, GROUP_SEPARATOR );

            GroupTreeNode node = rootNode;

            while ( tok.hasMoreTokens() )
            {
                String part = tok.nextToken();

                if ( !node.getChildren().containsKey( part ) )
                {
                    GroupTreeNode newNode = new GroupTreeNode( part, node );
                    node.addChild( newNode );
                    node = newNode;
                }
                else
                {
                    node = node.getChildren().get( part );
                }
            }
        }

        return rootNode;
    }

    private static List<String> collateGroups( GroupTreeNode rootNode )
    {
        List<String> groups = new ArrayList<String>();
        for ( GroupTreeNode node : rootNode.getChildren().values() )
        {
            while ( node.getChildren().size() == 1 )
            {
                node = node.getChildren().values().iterator().next();
            }

            groups.add( node.getFullName() );
        }
        return groups;
    }

    private static class GroupTreeNode
    {
        private final String name;

        private final String fullName;

        private final Map<String, GroupTreeNode> children = new TreeMap<String, GroupTreeNode>();

        GroupTreeNode()
        {
            name = null;
            fullName = null;
        }

        GroupTreeNode( String name, GroupTreeNode parent )
        {
            this.name = name;
            this.fullName = parent.fullName != null ? parent.fullName + GROUP_SEPARATOR + name : name;
        }

        public String getName()
        {
            return name;
        }

        public String getFullName()
        {
            return fullName;
        }

        public Map<String, GroupTreeNode> getChildren()
        {
            return children;
        }

        public void addChild( GroupTreeNode newNode )
        {
            children.put( newNode.name, newNode );
        }
    }
}
