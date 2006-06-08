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
import org.apache.maven.repository.indexing.RepositoryIndex;
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
 * Search by package name.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.PackageSearchAction"
 */
public class PackageSearchAction
    implements Action
{
    private String packageName;

    private String md5;

    private List searchResult;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexingFactory factory;

    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexSearchLayer searchLayer;

    public String execute()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException
    {
        String searchTerm;
        String key;
        if ( packageName != null && packageName.length() != 0 )
        {
            searchTerm = packageName;
            key = RepositoryIndex.FLD_PACKAGES;
        }
        else if ( md5 != null && md5.length() != 0 )
        {
            searchTerm = md5;
            key = "md5";
        }
        else
        {
            return ERROR;
        }

        // TODO: better config - share with general [!]
        Configuration configuration = new Configuration();
        File indexPath = new File( configuration.getIndexPath() );

        File repositoryDirectory = new File( configuration.getRepositoryDirectory() );
        String repoDir = repositoryDirectory.toURL().toString();

        ArtifactRepositoryLayout layout =
            (ArtifactRepositoryLayout) repositoryLayouts.get( configuration.getRepositoryLayout() );
        ArtifactRepository repository =
            repositoryFactory.createArtifactRepository( "repository", repoDir, layout, null, null );

        ArtifactRepositoryIndex index = factory.createArtifactRepositoryIndex( indexPath, repository );

        searchResult = searchLayer.searchAdvanced( new SinglePhraseQuery( key, searchTerm ), index );

        return SUCCESS;
    }

    public void setPackageName( String packageName )
    {
        this.packageName = packageName;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public List getSearchResult()
    {
        return searchResult;
    }
}
