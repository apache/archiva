package org.apache.maven.archiva.database.jdo;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.ArchivaRepositoryModel;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.List;

/**
 * JdoArchivaDAO 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="jdo"
 */
public class JdoArchivaDAO
    extends AbstractLogEnabled
    implements ArchivaDAO
{
    /**
     * @plexus.requirement role-hint="default"
     */
    private JdoAccess jdo;

    /* .\ Archiva Repository \.____________________________________________________________ */

    public ArchivaRepositoryModel createRepository( String id, String url )
    {
        ArchivaRepositoryModel repo;

        try
        {
            repo = getRepository( id );
        }
        catch ( ArchivaDatabaseException e )
        {
            repo = new ArchivaRepositoryModel();
            repo.setId( id );
            repo.setUrl( url );
        }

        return repo;
    }

    public List getRepositories()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.getAllObjects( ArchivaRepositoryModel.class );
    }

    public ArchivaRepositoryModel getRepository( String id )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return (ArchivaRepositoryModel) jdo.getObjectById( ArchivaRepositoryModel.class, id, null );
    }

    public List queryRepository( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.getAllObjects( ArchivaRepositoryModel.class, constraint );
    }

    public ArchivaRepositoryModel saveRepository( ArchivaRepositoryModel repository )
    {
        return (ArchivaRepositoryModel) jdo.saveObject( repository );
    }

    public void deleteRepository( ArchivaRepositoryModel repository )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( repository );
    }

    /* .\ Archiva Artifact \. _____________________________________________________________ */

    public ArchivaArtifactModel createArtifact( String groupId, String artifactId, String version, String classifier, String type )
    {
        ArchivaArtifactModel artifact;

        try
        {
            artifact = getArtifact( groupId, artifactId, version, classifier, type );
        }
        catch ( ArchivaDatabaseException e )
        {
            artifact = new ArchivaArtifactModel();
            artifact.setGroupId( groupId );
            artifact.setArtifactId( artifactId );
            artifact.setVersion( version );
            artifact.setClassifier( classifier );
            artifact.setType( type );
        }

        return artifact;
    }

    public ArchivaArtifactModel getArtifact( String groupId, String artifactId, String version, String classifier, String type )
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

    public ArchivaArtifactModel saveArtifact( ArchivaArtifactModel artifact )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteArtifact( ArchivaArtifactModel artifact )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub

    }

}
