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
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchLayer;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

/**
 * Searches for searchString in all indexed fields.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="quickSearchAction"
 */
public class QuickSearchAction
    implements Action
{
    /**
     * Query string.
     */
    private String q;

    /**
     * Search results.
     */
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
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    public String execute()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException
    {
        if ( q != null && q.length() != 0 )
        {
            Configuration configuration = new Configuration(); // TODO!
            File indexPath = new File( configuration.getIndexPath() );

            // TODO: [!] repository should only have been instantiated once
            File repositoryDirectory = new File( configuration.getRepositoryDirectory() );
            String repoDir = repositoryDirectory.toURL().toString();

            ArtifactRepositoryLayout layout =
                (ArtifactRepositoryLayout) repositoryLayouts.get( configuration.getRepositoryLayout() );
            ArtifactRepository repository =
                repositoryFactory.createArtifactRepository( "test", repoDir, layout, null, null );

            ArtifactRepositoryIndex index = factory.createArtifactRepositoryIndex( indexPath, repository );

            searchResult = searchLayer.searchGeneral( q, index );

            return SUCCESS;
        }
        else
        {
            return ERROR;
        }
    }

    public String getQ()
    {
        return q;
    }

    public void setQ( String q )
    {
        this.q = q;
    }

    public List getSearchResult()
    {
        return searchResult;
    }

}
