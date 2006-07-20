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
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchLayer;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * Searches for searchString in all indexed fields.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="searchAction"
 */
public class SearchAction
    extends ActionSupport
{
    /**
     * Query string.
     */
    private String q;

    /**
     * The MD5 to search by.
     */
    private String md5;

    /**
     * Search results.
     */
    private List searchResults;

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
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    public String quickSearch()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException,
        ConfigurationStoreException
    {
        // TODO: give action message if indexing is in progress

        assert q != null && q.length() != 0;

        ArtifactRepositoryIndex index = getIndex();

        if ( !index.indexExists() )
        {
            addActionError( "The repository is not yet indexed. Please wait, and then try again." );
            return ERROR;
        }

        searchResults = searchLayer.searchGeneral( q, index );

        return SUCCESS;
    }

    public String findArtifact()
        throws ConfigurationStoreException, RepositoryIndexException, RepositoryIndexSearchException
    {
        // TODO: give action message if indexing is in progress

        assert md5 != null && md5.length() != 0;

        ArtifactRepositoryIndex index = getIndex();

        if ( !index.indexExists() )
        {
            addActionError( "The repository is not yet indexed. Please wait, and then try again." );
            return ERROR;
        }

        searchResults = searchLayer.searchAdvanced( new SinglePhraseQuery( "md5", md5 ), index );

        return SUCCESS;
    }

    private ArtifactRepositoryIndex getIndex()
        throws ConfigurationStoreException, RepositoryIndexException
    {
        Configuration configuration = configurationStore.getConfigurationFromStore();
        File indexPath = new File( configuration.getIndexPath() );

        ArtifactRepository repository = repositoryFactory.createRepository( configuration );

        return factory.createArtifactRepositoryIndex( indexPath, repository );
    }

    public String doInput()
    {
        return INPUT;
    }

    public String getQ()
    {
        return q;
    }

    public void setQ( String q )
    {
        this.q = q;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public List getSearchResults()
    {
        return searchResults;
    }

}
