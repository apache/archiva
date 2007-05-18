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
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.RepositoryURL;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ProjectModelToDatabaseConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer"
 *                   role-hint="update-db-project"
 *                   instantiation-strategy="per-lookup"
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

    private List includes;

    public ProjectModelToDatabaseConsumer()
    {
        includes = new ArrayList();
        includes.add( "pom" );
    }

    public void beginScan()
    {
        // TODO Auto-generated method stub

    }

    public void completeScan()
    {
        // TODO Auto-generated method stub

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
            return;
        }

        File artifactFile = toFile( artifact );
        RepositoryConfiguration repo = getRepository( artifact );

        if ( StringUtils.equals( "default", repo.getLayout() ) )
        {
            try
            {
                ArchivaProjectModel model = project400Reader.read( artifactFile );
                
                model.setOrigin( "filesystem" );
                
                dao.getProjectModelDAO().saveProjectModel( model );
            }
            catch ( ProjectModelException e )
            {
                getLogger().warn( "Unable to read project model " + artifactFile + " : " + e.getMessage(), e );
            }
            catch ( ArchivaDatabaseException e )
            {
                getLogger().warn(
                                  "Unable to save project model " + artifactFile + " to the database : "
                                      + e.getMessage(), e );
            }
        }
    }

    private RepositoryConfiguration getRepository( ArchivaArtifact artifact )
    {
        String repoId = artifact.getModel().getRepositoryId();
        return archivaConfiguration.getConfiguration().findRepositoryById( repoId );
    }

    private File toFile( ArchivaArtifact artifact )
    {
        RepositoryConfiguration repoConfig = getRepository( artifact );

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

        String path = layout.toPath( artifact );
        RepositoryURL url = new RepositoryURL( repoConfig.getUrl() );
        return new File( url.getPath(), path );
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
        return true;
    }

}
