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
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.DatabaseCleanupConsumer;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * Consumer for removing or deleting from the database the project models fo artifacts that have been
 * deleted/removed from the repository.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 *         <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.DatabaseCleanupConsumer"
 *                   role-hint="not-present-remove-db-project"
 *                   instantiation-strategy="per-lookup"
 */
public class DatabaseCleanupRemoveProjectConsumer
    extends AbstractMonitoredConsumer
    implements DatabaseCleanupConsumer
{
    /**
     * @plexus.configuration default-value="not-present-remove-db-project"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Remove project from database if not present on filesystem."
     */
    private String description;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ProjectModelDAO projectModelDAO;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;
    
    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    public void beginScan()
    {
        // TODO Auto-generated method stub
    }

    public void completeScan()
    {
        // TODO Auto-generated method stub
    }

    public List<String> getIncludedTypes()
    {       	
    	return null;
    }

    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {    	
    	if ( !StringUtils.equals( "pom", artifact.getType() ) )
        {
            // Not a pom.  Skip it.
            return;
        }
    	
    	try
    	{
	    	ManagedRepositoryContent repositoryContent = 
	    		repositoryFactory.getManagedRepositoryContent( artifact.getModel().getRepositoryId() );
	        
	    	File file = new File( repositoryContent.getRepoRoot(), toPath( artifact ) );
	    	
	    	if( !file.exists() )
	        {	        	
        		ArchivaProjectModel projectModel = projectModelDAO.getProjectModel( 
        				artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );   
        		
        		projectModelDAO.deleteProjectModel( projectModel );        		
	        }
    	}
    	catch ( RepositoryException re )
    	{    		
    		throw new ConsumerException( "Can't run database cleanup remove artifact consumer: " + 
    				re.getMessage() );
    	}
    	catch ( ArchivaDatabaseException e )
        {    	
            throw new ConsumerException( e.getMessage() );
        }    	
      
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
        return false;
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

    public void setProjectModelDAO( ProjectModelDAO projectModelDAO )
    {
        this.projectModelDAO = projectModelDAO;
    }

    public void setBidirectionalRepositoryLayoutFactory( BidirectionalRepositoryLayoutFactory layoutFactory )
    {
        this.layoutFactory = layoutFactory;
    }
    
    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }
    
}
