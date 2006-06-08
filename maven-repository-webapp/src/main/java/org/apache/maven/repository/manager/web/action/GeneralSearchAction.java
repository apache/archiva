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

import com.opensymphony.xwork.Action;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchLayer;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.apache.maven.repository.manager.web.job.Configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Searches for searchString in all indexed fields.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.GeneralSearchAction"
 */
public class GeneralSearchAction
    implements Action
{
    private String searchString;

    private List searchResult;

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
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private Configuration configuration;

    public String execute()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException
    {
        if ( searchString != null && searchString.length() != 0 )
        {
            String indexPath = configuration.getIndexDirectory();

            // TODO: reduce the amount of lookup?

            File repositoryDirectory = new File( configuration.getRepositoryDirectory() );
            String repoDir = repositoryDirectory.toURL().toString();

            ArtifactRepository repository =
                repositoryFactory.createArtifactRepository( "test", repoDir, configuration.getLayout(), null, null );

            ArtifactRepositoryIndex index = factory.createArtifactRepositoryIndex( indexPath, repository );

            searchResult = searchLayer.searchGeneral( searchString, index );

            return SUCCESS;
        }
        else
        {
            return ERROR;
        }
    }

    public void setSearchString( String searchString )
    {
        this.searchString = searchString;
    }

    public List getSearchResult()
    {
        return searchResult;
    }

}
