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
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.plexus.ibatis.PlexusIbatisHelper;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * 
 * IbatisMetadataStore 
 *
 * @author <a href="mailto:jmcconnell@apache.com">Jesse McConnell</a>
 * @version $Id$
 * 
 */
public class AbstractMetadataKeyDatabase 
    extends AbstractLogEnabled 
    implements Initializable
{
    /**
     * @plexus.requirement 
     */
    protected PlexusIbatisHelper ibatisHelper;
    
    /**
     * @plexus.configuration default-value="create"
     */
    private String createPrefix;
    
    /**
     * @plexus.configuration default-value="drop"
     */
    private String dropPrefix;
    
    public MetadataKey getMetadataKey( Metadata metadata ) 
        throws ArchivaDatabaseException
    {
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            getLogger().info( "Getting metadata key" );
            MetadataKey newMetadataKey = (MetadataKey) sqlMap.queryForObject( "getMetadataKey", metadata );
            
            if ( newMetadataKey == null )
            {
                getLogger().info( "added new metadata" );
                sqlMap.update( "addMetadataKey", metadata );
                
                newMetadataKey = (MetadataKey) sqlMap.queryForObject( "getMetadataKey", metadata );
                
                if ( newMetadataKey == null )
                {
                    throw new ArchivaDatabaseException( "unable to create new MetadataKeys" );
                }
            }
            
            return newMetadataKey;
            
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while adding metadata, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new ArchivaDatabaseException ( "Error while interacting with the database.", e );
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


    protected void initializeTable( String tableName )
        throws ArchivaDatabaseException    
    {
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            Connection con = sqlMap.getCurrentConnection();

            DatabaseMetaData databaseMetaData = con.getMetaData();

            ResultSet rs = databaseMetaData.getTables( con.getCatalog(), null, null, null );

            // check if the index database exists in the database
            while ( rs.next() )
            {
                String dbTableName = rs.getString( "TABLE_NAME" );

                // if it does then we are already initialized
                if ( dbTableName.toLowerCase().equals( tableName.toLowerCase() ) )
                {
                    return;
                }
            }
            
            // Create the tables
            
            getLogger().info( "Creating table: " + tableName );
            sqlMap.update( createPrefix + tableName, null );
            
            sqlMap.commitTransaction();
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while initializing database, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new ArchivaDatabaseException( "Error while setting up database.", e );
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
    
    protected void dropTable( String tableName )
    throws ArchivaDatabaseException    
    {
    SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

    try
    {
        sqlMap.startTransaction();

        getLogger().info( "Dropping table: " + tableName );
        sqlMap.update( dropPrefix + tableName, null );
        
        sqlMap.commitTransaction();
    }
    catch ( SQLException e )
    {
        getLogger().error( "Error while dropping database, showing all linked exceptions in SQLException." );

        while ( e != null )
        {
            getLogger().error( e.getMessage(), e );

            e = e.getNextException();
        }

        throw new ArchivaDatabaseException( "Error while dropping database.", e );
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
        try
        {
            initializeTable( "MetadataKeys" );
        }
        catch ( ArchivaDatabaseException ade )
        {
            throw new InitializationException( "unable to initialize metadata keys database" );
        }
    }
    
    
    protected boolean tableExists( String tableName )
    throws ArchivaDatabaseException    
{
    SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

    try
    {
        sqlMap.startTransaction();

        Connection con = sqlMap.getCurrentConnection();

        DatabaseMetaData databaseMetaData = con.getMetaData();

        ResultSet rs = databaseMetaData.getTables( con.getCatalog(), null, null, null );

        // check if the index database exists in the database
        while ( rs.next() )
        {
            String dbTableName = rs.getString( "TABLE_NAME" );

            // if it does then we are already initialized
            if ( dbTableName.toLowerCase().equals( tableName.toLowerCase() ) )
            {
                return true;
            }
        }
        return false;
    }
    catch ( SQLException e )
    {
        getLogger().error( "Error while check database, showing all linked exceptions in SQLException." );

        while ( e != null )
        {
            getLogger().error( e.getMessage(), e );

            e = e.getNextException();
        }

        throw new ArchivaDatabaseException( "Error while checking database.", e );
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
    
}
