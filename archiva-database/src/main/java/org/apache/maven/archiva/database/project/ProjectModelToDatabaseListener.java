package org.apache.maven.archiva.database.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;
import org.apache.maven.archiva.repository.project.resolvers.FilesystemBasedResolver;
import org.apache.maven.archiva.repository.project.resolvers.ProjectModelResolutionListener;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.List;

/**
 * Just in Time save of project models to the database, implemented as a listener
 * on {@link ProjectModelResolver} objects that implement {@link FilesystemBasedResolver}.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *              role="org.apache.maven.archiva.repository.project.resolvers.ProjectModelResolutionListener"
 *              role-hint="model-to-db"
 */
public class ProjectModelToDatabaseListener
    extends AbstractLogEnabled
    implements ProjectModelResolutionListener
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    private void saveInDatabase( ArchivaProjectModel model )
        throws ProjectModelException
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

    private void removeFromDatabase( ArchivaProjectModel model )
        throws ProjectModelException
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

    private boolean existsInDatabase( ArchivaProjectModel model )
        throws ProjectModelException
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

    public void resolutionAttempting( VersionedReference projectRef, ProjectModelResolver resolver )
    {
        /* do nothing */
    }

    public void resolutionError( VersionedReference projectRef, ProjectModelResolver resolver, Exception cause )
    {
        /* do nothing */
    }

    public void resolutionMiss( VersionedReference projectRef, ProjectModelResolver resolver )
    {
        /* do nothing */
    }

    public void resolutionNotFound( VersionedReference projectRef, List resolverList )
    {
        /* do nothing */
    }

    public void resolutionStart( VersionedReference projectRef, List resolverList )
    {
        /* do nothing */
    }

    public void resolutionSuccess( VersionedReference projectRef, ProjectModelResolver resolver,
                                   ArchivaProjectModel model )
    {
        if ( !( resolver instanceof FilesystemBasedResolver ) )
        {
            // Nothing to do. skip it.
            return;
        }

        model.setOrigin( "filesystem" );

        try
        {
            // Test if it exists.
            if ( existsInDatabase( model ) )
            {
                removeFromDatabase( model );
            }

            saveInDatabase( model );
        }
        catch ( ProjectModelException e )
        {
            getLogger().warn( e.getMessage(), e );
        }
    }
}
