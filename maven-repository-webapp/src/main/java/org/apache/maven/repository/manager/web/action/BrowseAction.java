package org.apache.maven.repository.manager.web.action;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.ActionSupport;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchLayer;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Browse the repository.
 *
 * @todo the tree part probably belongs in a browsing component, along with the methods currently in the indexer
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="browseAction"
 */
public class BrowseAction
    extends ActionSupport
{
    /**
     * @plexus.requirement
     */
    private RepositoryIndexingFactory factory;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexSearchLayer searchLayer;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    private List groups;

    private String groupId;

    private static final String GROUP_SEPARATOR = "/";

    private List artifactIds;

    private String artifactId;

    private List versions;

    public String browse()
        throws ConfigurationStoreException, RepositoryIndexException, IOException
    {
        ArtifactRepositoryIndex index = getIndex();

        if ( !index.indexExists() )
        {
            addActionError( "The repository is not yet indexed. Please wait, and then try again." );
            return ERROR;
        }

        GroupTreeNode rootNode = buildGroupTree( index );

        this.groups = collateGroups( rootNode );

        return SUCCESS;
    }

    public String browseGroup()
        throws ConfigurationStoreException, RepositoryIndexException, IOException
    {
        ArtifactRepositoryIndex index = getIndex();

        if ( !index.indexExists() )
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
                addActionError( "The group specified was not found" );
                return ERROR;
            }
            else
            {
                rootNode = (GroupTreeNode) rootNode.getChildren().get( part );
            }
        }

        this.groups = collateGroups( rootNode );

        this.artifactIds = index.getArtifacts( groupId.replaceAll( GROUP_SEPARATOR, "." ) );

        return SUCCESS;
    }

    public String browseArtifact()
        throws ConfigurationStoreException, RepositoryIndexException, IOException
    {
        ArtifactRepositoryIndex index = getIndex();

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

        versions = index.getVersions( groupId.replaceAll( GROUP_SEPARATOR, "." ), artifactId );

        if ( versions.isEmpty() )
        {
            // TODO: i18n
            addActionError( "Could not find any artifacts with the given group and artifact ID" );
            return ERROR;
        }

        return SUCCESS;
    }

    private GroupTreeNode buildGroupTree( ArtifactRepositoryIndex index )
        throws IOException
    {
        // TODO: give action message if indexing is in progress

        // TODO: this will be inefficient over a very large number of artifacts, should be cached

        List groups = index.enumerateGroupIds();

        GroupTreeNode rootNode = new GroupTreeNode();

        // build a tree structure
        for ( Iterator i = groups.iterator(); i.hasNext(); )
        {
            String groupId = (String) i.next();

            StringTokenizer tok = new StringTokenizer( groupId, "." );

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

    private ArtifactRepositoryIndex getIndex()
        throws ConfigurationStoreException, RepositoryIndexException
    {
        Configuration configuration = configurationStore.getConfigurationFromStore();
        File indexPath = new File( configuration.getIndexPath() );

        ArtifactRepository repository = repositoryFactory.createRepository( configuration );

        return factory.createArtifactRepositoryIndex( indexPath, repository );
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
