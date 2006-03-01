package org.apache.maven.repository.manager.web.action;

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

            RepositoryIndexSearchLayer searchLayer = factory.createRepositoryIndexSearchLayer( index );

            searchResult = searchLayer.searchGeneral( searchString );

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
