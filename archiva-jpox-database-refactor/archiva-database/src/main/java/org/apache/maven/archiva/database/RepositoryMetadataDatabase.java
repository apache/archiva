package org.apache.maven.archiva.database;

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

import com.ibatis.sqlmap.client.SqlMapClient;

import org.apache.maven.archiva.database.key.MetadataKey;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.sql.SQLException;

/**
 * RepositoryMetadataDatabase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.RepositoryMetadataDatabase" role-hint="default"
 */
public class RepositoryMetadataDatabase extends AbstractMetadataKeyDatabase
{
    public void create( RepositoryMetadata metadata )
        throws ArchivaDatabaseException
    {
    
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            getLogger().info( "Adding repository metadata" );
            sqlMap.update( "addRepositoryMetadata", metadata );
            
            sqlMap.commitTransaction();
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while executing statement, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new ArchivaDatabaseException( "Error while executing statement.", e );
        }
        finally
        {
            try
            {
                sqlMap.endTransaction();
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
    }
 

    public RepositoryMetadata read( String groupId, String artifactId, String version )
        throws ArchivaDatabaseException
    {
        
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            getLogger().info( "Reading repository metadata" );
            RepositoryMetadata repositoryMetadata = (RepositoryMetadata) sqlMap.queryForObject( "getRepositoryMetadata", new MetadataKey( groupId, artifactId, version ) );
            
            return repositoryMetadata;
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while executing statement, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new ArchivaDatabaseException( "Error while executing statement.", e );
        }
        finally
        {
            try
            {
                sqlMap.endTransaction();
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * not implemented yet
     * 
     * @param metadata
     * @throws ArchivaDatabaseException
     */
    public void update( RepositoryMetadata metadata )
        throws ArchivaDatabaseException
    {
        
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            getLogger().info( "Updating repository metadata" );
            sqlMap.update( "updateRepositoryMetadata", metadata );
            
            sqlMap.commitTransaction();
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while executing statement, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new ArchivaDatabaseException( "Error while executing statement.", e );
        }
        finally
        {
            try
            {
                sqlMap.endTransaction();
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
    }

    public void delete( RepositoryMetadata metadata )
        throws ArchivaDatabaseException
    {
        // FIXME is this right? baseVersion seems wrong but I don't know enough about the metadata to say
        delete( metadata.getGroupId(), metadata.getArtifactId(), metadata.getBaseVersion() );
    }

    public void delete( String groupId, String artifactId, String version )
        throws ArchivaDatabaseException
    {
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            getLogger().info( "Removing repository metadata" );
            sqlMap.update( "removeRepositoryMetadata", new MetadataKey( groupId, artifactId, version ) );
            
            sqlMap.commitTransaction();
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while executing statement, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new ArchivaDatabaseException( "Error while executing statement.", e );
        }
        finally
        {
            try
            {
                sqlMap.endTransaction();
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
    }

    public void initialize()
        throws InitializationException
    {        
        super.initialize();
        try
        {
            initializeTable( "RepositoryMetadata" );
        }
        catch ( ArchivaDatabaseException ade )
        {
            throw new InitializationException( "unable to initialize repository metadata table", ade );
        }
    }
    
    
}
