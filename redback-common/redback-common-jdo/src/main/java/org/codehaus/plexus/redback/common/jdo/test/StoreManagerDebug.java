package org.codehaus.plexus.redback.common.jdo.test;

/*
 * Copyright 2009 The Codehaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jdo.PersistenceManager;

import org.jpox.AbstractPersistenceManagerFactory;
import org.jpox.ClassLoaderResolver;
import org.jpox.plugin.ConfigurationElement;
import org.jpox.plugin.Extension;
import org.jpox.store.rdbms.RDBMSManager;

/**
 * A extension to JPOX store manager that allows counting the SQL queries
 * 
 * @author Carlos Sanchez <a href="mailto:carlos@apache.org">
 */
public class StoreManagerDebug
    extends RDBMSManager
{
    private static int counter;

    public StoreManagerDebug( ClassLoaderResolver clr, AbstractPersistenceManagerFactory pmf, String userName,
                              String password )
    {
        super( clr, pmf, userName, password );
    }

    /**
     * This method will change JPOX store manager extension so it uses our class instead of whatever is configured in
     * the plugin.xml
     * 
     * @param pmf
     */
    public static void setup( AbstractPersistenceManagerFactory pmf )
    {
        /* set our own Store Manager to allow counting SQL statements */
        Extension[] extensions =
            pmf.getPMFContext().getPluginManager().getExtensionPoint( "org.jpox.store_manager" ).getExtensions();
        Extension e = extensions[0];
        for ( ConfigurationElement element : e.getConfigurationElements() )
        {
            element.putAttribute( "class-name", StoreManagerDebug.class.getName() );
        }
    }

    /**
     * Get the currently configured store manager from JPOX. Will fail if
     * {@link #setup(AbstractPersistenceManagerFactory)} is not called first.
     * 
     * @param persistenceManager
     * @return
     */
    public static StoreManagerDebug getConfiguredStoreManager( PersistenceManager persistenceManager )
    {
        return (StoreManagerDebug) ( (org.jpox.PersistenceManager) persistenceManager ).getStoreManager();
    }

    @Override
    public int[] executeStatementBatch( String stmt, PreparedStatement ps )
        throws SQLException
    {
        counter++;
        return super.executeStatementBatch( stmt, ps );
    }

    @Override
    public ResultSet executeStatementQuery( String stmt, PreparedStatement ps )
        throws SQLException
    {
        counter++;
        return super.executeStatementQuery( stmt, ps );
    }

    @Override
    public int executeStatementUpdate( String stmt, PreparedStatement ps )
        throws SQLException
    {
        counter++;
        return super.executeStatementUpdate( stmt, ps );
    }

    public void resetCounter()
    {
        counter = 0;
    }

    public int counter()
    {
        return counter;
    }
}
