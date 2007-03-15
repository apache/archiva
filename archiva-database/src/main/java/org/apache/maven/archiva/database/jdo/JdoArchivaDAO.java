package org.apache.maven.archiva.database.jdo;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArchivaRepositoryByUrlConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryContent;
import org.apache.maven.archiva.model.RepositoryContentKey;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.List;

/**
 * JdoArchivaDAO 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.ArchivaDAO"
 *                   role-hint="jdo"
 */
public class JdoArchivaDAO
    extends AbstractLogEnabled
    implements ArchivaDAO
{
    /**
     * @plexus.requirement
     */
    private JdoAccess jdo;

    /* .\ Archiva Repository \.____________________________________________________________ */

    public ArchivaRepository createRepository( String id, String url )
    {
        ArchivaRepository repo;

        try
        {
            repo = getRepository( id );
        }
        catch ( ArchivaDatabaseException e )
        {
            repo = new ArchivaRepository();
            repo.setId( id );
            repo.setUrl( url );
        }

        return repo;
    }

    public List getRepositories()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.getAllObjects( ArchivaRepository.class );
    }

    public ArchivaRepository getRepository( String id )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return (ArchivaRepository) jdo.getObjectById( ArchivaRepository.class, id, null );
    }

    public List queryRepository( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.getAllObjects( ArchivaRepository.class, constraint );
    }

    public ArchivaRepository saveRepository( ArchivaRepository repository )
    {
        return (ArchivaRepository) jdo.saveObject( repository );
    }

    public void deleteRepository( ArchivaRepository repository )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( repository );
    }

    /* .\ Repository Content \.____________________________________________________________ */

    public RepositoryContent createRepositoryContent( String groupId, String artifactId, String version,
                                                      String repositoryId )
    {
        RepositoryContent repoContent;

        try
        {
            repoContent = getRepositoryContent( groupId, artifactId, version, repositoryId );
        }
        catch ( ArchivaDatabaseException e )
        {
            repoContent = new RepositoryContent( repositoryId, groupId, artifactId, version );
        }

        return repoContent;
    }

    public RepositoryContent getRepositoryContent( String groupId, String artifactId, String version,
                                                   String repositoryId )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        RepositoryContentKey key = new RepositoryContentKey();
        key.groupId = groupId;
        key.artifactId = artifactId;
        key.version = version;
        key.repositoryId = repositoryId;

        return (RepositoryContent) jdo.getObjectById( RepositoryContent.class, key, null );
    }

    public List queryRepositoryContents( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.getAllObjects( RepositoryContent.class, constraint );
    }

    public RepositoryContent saveRepositoryContent( RepositoryContent repoContent )
        throws ArchivaDatabaseException
    {
        return (RepositoryContent) jdo.saveObject( repoContent );
    }

    public void deleteRepositoryContent( RepositoryContent repoContent )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( repoContent );
    }

    /* .\ Archiva Artifact \. _____________________________________________________________ */

    public ArchivaArtifact createArtifact( RepositoryContent repoContent, String classifier, String type )
    {
        ArchivaArtifact artifact;

        try
        {
            artifact = getArtifact( repoContent, classifier, type );
        }
        catch ( ArchivaDatabaseException e )
        {
            artifact = new ArchivaArtifact();
            artifact.setContentKey( repoContent );
            artifact.setClassifier( classifier );
            artifact.setType( type );
        }

        return artifact;
    }

    public ArchivaArtifact getArtifact( RepositoryContent repoContent, String classifier, String type )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        
        return null;
    }

    public List queryArtifacts( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArchivaArtifact saveArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteArtifact( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub

    }

}
