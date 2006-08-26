package org.apache.maven.archiva.web.action;

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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.indexer.record.StandardArtifactIndexRecord;
import org.apache.maven.archiva.indexer.record.StandardIndexRecordFields;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Browse the repository.
 *
 * @todo the tree part probably belongs in a browsing component, and the indexer could optimize how it retrieves the terms rather than querying everything
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="browseAction"
 */
public class BrowseAction
    extends ActionSupport
{
    /**
     * @plexus.requirement
     */
    private RepositoryArtifactIndexFactory factory;

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

    private static final String GROUP_SEPARATOR = ".";

    private List artifactIds;

    private String artifactId;

    private List versions;

    public String browse()
        throws ConfigurationStoreException, RepositoryIndexException, IOException, RepositoryIndexSearchException
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
        throws ConfigurationStoreException, RepositoryIndexException, IOException, RepositoryIndexSearchException
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
                addActionError( "The group specified was not found" );
                return ERROR;
            }
            else
            {
                rootNode = (GroupTreeNode) rootNode.getChildren().get( part );
            }
        }

        this.groups = collateGroups( rootNode );

        List records = index.search(
            new LuceneQuery( new TermQuery( new Term( StandardIndexRecordFields.GROUPID_EXACT, groupId ) ) ) );

        Set artifactIds = new HashSet();
        for ( Iterator i = records.iterator(); i.hasNext(); )
        {
            StandardArtifactIndexRecord record = (StandardArtifactIndexRecord) i.next();
            artifactIds.add( record.getArtifactId() );
        }
        this.artifactIds = new ArrayList( artifactIds );
        Collections.sort( this.artifactIds );

        return SUCCESS;
    }

    public String browseArtifact()
        throws ConfigurationStoreException, RepositoryIndexException, IOException, RepositoryIndexSearchException
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

        BooleanQuery query = new BooleanQuery();
        query.add( new TermQuery( new Term( StandardIndexRecordFields.GROUPID_EXACT, groupId ) ),
                   BooleanClause.Occur.MUST );
        query.add( new TermQuery( new Term( StandardIndexRecordFields.ARTIFACTID_EXACT, artifactId ) ),
                   BooleanClause.Occur.MUST );

        List records = index.search( new LuceneQuery( query ) );

        if ( records.isEmpty() )
        {
            // TODO: i18n
            addActionError( "Could not find any artifacts with the given group and artifact ID" );
            return ERROR;
        }

        Set versions = new HashSet();
        for ( Iterator i = records.iterator(); i.hasNext(); )
        {
            StandardArtifactIndexRecord record = (StandardArtifactIndexRecord) i.next();
            versions.add( record.getVersion() );
        }

        this.versions = new ArrayList( versions );
        Collections.sort( this.versions );

        return SUCCESS;
    }

    private GroupTreeNode buildGroupTree( RepositoryArtifactIndex index )
        throws IOException, RepositoryIndexSearchException
    {
        // TODO: give action message if indexing is in progress

        // TODO: this will be inefficient over a very large number of artifacts, should be cached

        List records = index.search( new LuceneQuery( new MatchAllDocsQuery() ) );

        Set groups = new TreeSet();
        for ( Iterator i = records.iterator(); i.hasNext(); )
        {
            StandardArtifactIndexRecord record = (StandardArtifactIndexRecord) i.next();
            groups.add( record.getGroupId() );
        }

        GroupTreeNode rootNode = new GroupTreeNode();

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

    private RepositoryArtifactIndex getIndex()
        throws ConfigurationStoreException, RepositoryIndexException
    {
        Configuration configuration = configurationStore.getConfigurationFromStore();
        File indexPath = new File( configuration.getIndexPath() );

        return factory.createStandardIndex( indexPath );
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
