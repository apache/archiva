package org.apache.maven.archiva.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.maven.archiva.database.key.MetadataKey;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.plexus.ibatis.PlexusIbatisHelper;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * 
 * IbatisMetadataStore 
 *
 * @author <a href="mailto:jmcconnell@apache.com">Jesse McConnell</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.MetadataStore" role-hint="ibatis"
 */
public class IbatisMetadataStore
    extends AbstractLogEnabled
    implements MetadataStore, Initializable
{

    /**
     * @plexus.requirement 
     */
    private PlexusIbatisHelper ibatisHelper;

    public void initialize()
        throws InitializationException
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
                String tableName = rs.getString( "TABLE_NAME" );

                // if it does then we are already initialized
                if ( tableName.toLowerCase().equals( "MetadataKeys" ) )
                {
                    return;
                }
            }
            
            // Create the tables
            
            getLogger().info( "Creating metadata keys instance table" );
            sqlMap.update( "initializeMetadataKeyTable", null );
            
            getLogger().info( "Creating repository metadata instance table" );
            sqlMap.update( "initializeRepositoryMetadataTable", null );

            getLogger().info( "Creating repository health metadata instance table" );
            sqlMap.update( "initializeHealthMetadataTable", null );
            
            getLogger().info( "Creating repository versions metadata instance table" );
            sqlMap.update( "initializeVersionsMetadataTable", null );
            
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

            throw new InitializationException( "Error while setting up database.", e );
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
    
    public void addMetadataKey( MetadataKey metadataKey ) 
        throws MetadataStoreException
    {
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            getLogger().info( "Adding metadata key" );
            sqlMap.update( "addMetadataKey", metadataKey );

            sqlMap.commitTransaction();
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while adding metadata, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new MetadataStoreException ( "Error while interacting with the database.", e );
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
