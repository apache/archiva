package org.apache.maven.archiva.consumers.database.project;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;

/**
 * Wrapped {@link ProjectModelResolver} to allow for insertion of resolved project models on discovery. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class WrappedDatabaseProjectModelResolver
    implements ProjectModelResolver
{
    private ArchivaDAO dao;

    private ProjectModelResolver resolver;

    public WrappedDatabaseProjectModelResolver( ArchivaDAO dao, ProjectModelResolver resolver )
    {
        this.dao = dao;
        this.resolver = resolver;
    }

    public ArchivaProjectModel resolveProjectModel( VersionedReference reference )
        throws ProjectModelException
    {
        ArchivaProjectModel model = resolver.resolveProjectModel( reference );
        if ( model == null )
        {
            return model;
        }

        // Test if it exists.
        if ( existsInDatabase( model ) )
        {
            removeFromDatabase( model );
        }

        saveInDatabase( model );

        return model;
    }

    private void saveInDatabase( ArchivaProjectModel model ) throws ProjectModelException
    {
        try
        {
            dao.getProjectModelDAO().saveProjectModel( model );
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new ProjectModelException( "Unable to save model to database: " + e.getMessage(), e );
        }
    }

    private void removeFromDatabase( ArchivaProjectModel model ) throws ProjectModelException
    {
        try
        {
            dao.getProjectModelDAO().deleteProjectModel( model );
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new ProjectModelException( "Unable to remove existing model from database: " + e.getMessage(), e );
        }
    }

    private boolean existsInDatabase( ArchivaProjectModel model ) throws ProjectModelException
    {
        try
        {
            ArchivaProjectModel dbmodel = dao.getProjectModelDAO().getProjectModel( model.getGroupId(),
                                                                                    model.getArtifactId(),
                                                                                    model.getVersion() );

            return ( dbmodel != null );
        }
        catch ( ObjectNotFoundException e )
        {
            return false;
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new ProjectModelException( "Unable to check for existing model from database: " + e.getMessage(), e );
        }
    }
}
