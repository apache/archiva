package org.apache.maven.archiva.web.action;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Browse the repository.
 *
 * @todo cache should be a proper cache class that is a singleton requirement rather than static variables
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="browseAction"
 */
public class BrowseAction
    extends PlexusActionSupport
{
    /**
     * @plexus.requirement
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private List groups;

    private String groupId;

    private static final String GROUP_SEPARATOR = ".";

    private List artifactIds;

    private String artifactId;

    private List versions;

    private static GroupTreeNode rootNode;

    private static long groupCacheTime;

    public String browse()
        throws RepositoryIndexException, IOException
    {
        RepositoryArtifactIndex index = getIndex();

        if ( !index.exists() )
        {
            addActionError( "The repository is not yet indexed. Please wait, and then try again." );
            return ERROR;
        }

        GroupTreeNode rootNode = buildGroupTree( index );

        this.groups = collateGroups( rootNode );

        return SUCCESS;
    }

    public String browseGroup()
        throws RepositoryIndexException, IOException, RepositoryIndexSearchException
    {
        RepositoryArtifactIndex index = getIndex();

        if ( !index.exists() )
        {
            addActionError( "The repository is not yet indexed. Please wait, and then try again." );
            return ERROR;
        }

        GroupTreeNode rootNode = buildGroupTree( index );

        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        StringTokenizer tok = new StringTokenizer( groupId, GROUP_SEPARATOR );
        while ( tok.hasMoreTokens() )
        {
            String part = tok.nextToken();

            if ( !rootNode.getChildren().containsKey( part ) )
            {
                // TODO: i18n
                getLogger().debug(
                    "Can't find part: " + part + " for groupId " + groupId + " in children " + rootNode.getChildren() );
                addActionError( "The group specified was not found" );
                return ERROR;
            }
            else
            {
                rootNode = (GroupTreeNode) rootNode.getChildren().get( part );
            }
        }

        this.groups = collateGroups( rootNode );

        this.artifactIds = index.getArtifactIds( groupId );
        Collections.sort( this.artifactIds );

        return SUCCESS;
    }

    public String browseArtifact()
        throws RepositoryIndexException, IOException, RepositoryIndexSearchException
    {
        RepositoryArtifactIndex index = getIndex();

        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        if ( StringUtils.isEmpty( artifactId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a artifact ID to browse" );
            return ERROR;
        }

        this.versions = index.getVersions( groupId, artifactId );
        Collections.sort( this.versions );

        if ( versions.isEmpty() )
        {
            // TODO: i18n
            addActionError( "Could not find any artifacts with the given group and artifact ID" );
            return ERROR;
        }

        return SUCCESS;
    }

    private GroupTreeNode buildGroupTree( RepositoryArtifactIndex index )
        throws IOException, RepositoryIndexException
    {
        // TODO: give action message if indexing is in progress

        long lastUpdate = index.getLastUpdatedTime();

        if ( rootNode == null || lastUpdate > groupCacheTime )
        {
            List groups = index.getAllGroupIds();

            getLogger().info( "Loaded " + groups.size() + " groups from index" );

            rootNode = new GroupTreeNode();

            // build a tree structure
            for ( Iterator i = groups.iterator(); i.hasNext(); )
            {
                String groupId = (String) i.next();

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
                        node = (GroupTreeNode) node.getChildren().get( part );
                    }
                }
            }
            groupCacheTime = lastUpdate;
        }
        else
        {
            getLogger().debug( "Loaded groups from cache" );
        }

        return rootNode;
    }

    private List collateGroups( GroupTreeNode rootNode )
    {
        List groups = new ArrayList();
        for ( Iterator i = rootNode.getChildren().values().iterator(); i.hasNext(); )
        {
            GroupTreeNode node = (GroupTreeNode) i.next();

            while ( node.getChildren().size() == 1 )
            {
                node = (GroupTreeNode) node.getChildren().values().iterator().next();
            }

            groups.add( node.getFullName() );
        }
        return groups;
    }

    public List getGroups()
    {
        return groups;
    }

    public List getArtifactIds()
    {
        return artifactIds;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public List getVersions()
    {
        return versions;
    }

    private static class GroupTreeNode
    {
        private final String name;

        private final String fullName;

        private final Map children = new TreeMap();

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

        public Map getChildren()
        {
            return children;
        }

        public void addChild( GroupTreeNode newNode )
        {
            children.put( newNode.name, newNode );
        }
    }
}
