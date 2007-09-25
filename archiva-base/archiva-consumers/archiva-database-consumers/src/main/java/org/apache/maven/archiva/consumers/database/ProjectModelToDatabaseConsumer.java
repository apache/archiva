package org.apache.maven.archiva.consumers.database;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.reporting.artifact.CorruptArtifactReport;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.FilenameParts;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.layout.RepositoryLayoutUtils;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.filters.EffectiveProjectModelFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ProjectModelToDatabaseConsumer
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer"
 * role-hint="update-db-project"
 * instantiation-strategy="per-lookup"
 */
public class ProjectModelToDatabaseConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseUnprocessedArtifactConsumer
{
    /**
     * @plexus.configuration default-value="update-db-project"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Update database with project model information."
     */
    private String description;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    /**
     * @plexus.requirement role-hint="model400"
     */
    private ProjectModelReader project400Reader;

    /**
     * @plexus.requirement role-hint="model300"
     */
    private ProjectModelReader project300Reader;

    /**
     * @plexus.requirement role-hint="expression"
     */
    private ProjectModelFilter expressionModelFilter;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.project.ProjectModelFilter"
     * role-hint="effective"
     */
    private EffectiveProjectModelFilter effectiveModelFilter;

    private List includes;

    public ProjectModelToDatabaseConsumer()
    {
        includes = new ArrayList();
        includes.add( "pom" );
    }

    public void beginScan()
    {
        /* nothing to do here */
    }

    public void completeScan()
    {
        /* nothing to do here */
    }

    public List getIncludedTypes()
    {
        return includes;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        if ( !StringUtils.equals( "pom", artifact.getType() ) )
        {
            // Not a pom.  Skip it.
            return;
        }

        if ( hasProjectModelInDatabase( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() ) )
        {
            // Already in the database.  Skip it.
            return;
        }

        File artifactFile = toFile( artifact );
        AbstractRepositoryConfiguration repo = getRepository( artifact );
        ProjectModelReader reader = project400Reader;

        if ( StringUtils.equals( "legacy", repo.getLayout() ) )
        {
            reader = project300Reader;
        }

        try
        {
            ArchivaProjectModel model = reader.read( artifactFile );

            model.setOrigin( "filesystem" );

            // Filter the model
            model = expressionModelFilter.filter( model );

            // Resolve the project model
            model = effectiveModelFilter.filter( model );

            // The version should be updated to the filename version if it is a unique snapshot
            FilenameParts parts = RepositoryLayoutUtils.splitFilename( artifactFile.getName(), null );
            if ( model.getVersion().equals( VersionUtil.getBaseVersion( parts.version ) ) &&
                VersionUtil.isUniqueSnapshot( parts.version ) )
            {
                model.setVersion( parts.version );
            }

            if ( isValidModel( model, artifact ) )
            {
                getLogger().info( "Add project model " + model + " to database." );

                dao.getProjectModelDAO().saveProjectModel( model );
            }
            else
            {
                getLogger().warn(
                    "Invalid or corrupt pom. Project model " + model + " was not added in the database." );
            }

        }
        catch ( ProjectModelException e )
        {
            getLogger().warn( "Unable to read project model " + artifactFile + " : " + e.getMessage(), e );

            addProblem( artifact, "Unable to read project model " + artifactFile + " : " + e.getMessage() );
        }
        catch ( ArchivaDatabaseException e )
        {
            getLogger().warn( "Unable to save project model " + artifactFile + " to the database : " + e.getMessage(),
                              e );
        }
        catch ( Throwable t )
        {
            // Catch the other errors in the process to allow the rest of the process to complete.
            getLogger().error( "Unable to process model " + artifactFile + " due to : " + t.getClass().getName() +
                " : " + t.getMessage(), t );
        }
    }

    private boolean hasProjectModelInDatabase( String groupId, String artifactId, String version )
    {
        try
        {
            ArchivaProjectModel model = dao.getProjectModelDAO().getProjectModel( groupId, artifactId, version );
            return ( model != null );
        }
        catch ( ObjectNotFoundException e )
        {
            return false;
        }
        catch ( ArchivaDatabaseException e )
        {
            return false;
        }
    }

    private ManagedRepositoryConfiguration getRepository( ArchivaArtifact artifact )
    {
        String repoId = artifact.getModel().getRepositoryId();
        return archivaConfiguration.getConfiguration().findManagedRepositoryById( repoId );
    }

    private File toFile( ArchivaArtifact artifact )
    {
        ManagedRepositoryConfiguration repoConfig = getRepository( artifact );

        BidirectionalRepositoryLayout layout = null;

        try
        {
            layout = layoutFactory.getLayout( artifact );
        }
        catch ( LayoutException e )
        {
            getLogger().warn( "Unable to determine layout of " + artifact + ": " + e.getMessage(), e );
            return null;
        }

        return new File( repoConfig.getLocation(), layout.toPath( artifact ) );
    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isPermanent()
    {
        // Tells the configuration that this consumer cannot be disabled.
        return true;
    }

    private String toPath( ArchivaArtifact artifact )
    {
        try
        {
            BidirectionalRepositoryLayout layout = layoutFactory.getLayout( artifact );
            return layout.toPath( artifact );
        }
        catch ( LayoutException e )
        {
            getLogger().warn( "Unable to calculate path for artifact: " + artifact );
            return null;
        }
    }

    private boolean isValidModel( ArchivaProjectModel model, ArchivaArtifact artifact )
        throws ConsumerException
    {
        File artifactFile = toFile( artifact );

        try
        {
            FilenameParts parts = RepositoryLayoutUtils.splitFilename( artifactFile.getName(), null );

            if ( !parts.artifactId.equalsIgnoreCase( model.getArtifactId() ) )
            {
                StringBuffer emsg = new StringBuffer();
                emsg.append( "File " ).append( artifactFile.getName() );
                emsg.append( " has an invalid project model <" ).append( model.toString() ).append( ">: " );
                emsg.append( "The model artifactId <" ).append( model.getArtifactId() );
                emsg.append( "> does not match the artifactId portion of the filename: " ).append( parts.artifactId );
                
                getLogger().warn(emsg.toString() );
                addProblem( artifact, emsg.toString() );

                return false;
            }

            if ( !parts.version.equalsIgnoreCase( model.getVersion() ) &&
                !VersionUtil.getBaseVersion( parts.version ).equalsIgnoreCase( model.getVersion() ) )
            {
                StringBuffer emsg = new StringBuffer();
                emsg.append( "File " ).append( artifactFile.getName() );
                emsg.append( " has an invalid project model <" ).append( model.toString() ).append( ">: " );
                emsg.append( "The model version <" ).append( model.getVersion() );
                emsg.append( "> does not match the version portion of the filename: " ).append( parts.version );
                
                getLogger().warn(emsg.toString() );
                addProblem( artifact, emsg.toString() );

                return false;
            }

        }
        catch ( LayoutException le )
        {
            throw new ConsumerException( le.getMessage() );
        }

        return true;
    }

    private void addProblem( ArchivaArtifact artifact, String msg )
        throws ConsumerException
    {
        RepositoryProblem problem = new RepositoryProblem();
        problem.setRepositoryId( artifact.getModel().getRepositoryId() );
        problem.setPath( toPath( artifact ) );
        problem.setGroupId( artifact.getGroupId() );
        problem.setArtifactId( artifact.getArtifactId() );
        problem.setVersion( artifact.getVersion() );
        problem.setType( CorruptArtifactReport.PROBLEM_TYPE_CORRUPT_ARTIFACT );
        problem.setOrigin( getId() );
        problem.setMessage( msg );

        try
        {
            dao.getRepositoryProblemDAO().saveRepositoryProblem( problem );
        }
        catch ( ArchivaDatabaseException e )
        {
            String emsg = "Unable to save problem with artifact location to DB: " + e.getMessage();
            getLogger().warn( emsg, e );
            throw new ConsumerException( emsg, e );
        }
    }

}
