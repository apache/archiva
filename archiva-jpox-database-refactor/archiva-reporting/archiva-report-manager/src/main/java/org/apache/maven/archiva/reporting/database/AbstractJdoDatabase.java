package org.apache.maven.archiva.reporting.database;

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

import org.codehaus.plexus.jdo.JdoFactory;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.util.List;

import javax.jdo.Extent;
import javax.jdo.JDOException;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

/**
 * AbstractJdoResults - Base class for all JDO related results.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractJdoDatabase
    implements Initializable
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private JdoFactory jdoFactory;

    private PersistenceManagerFactory pmf;

    // -------------------------------------------------------------------
    // JPOX / JDO Specifics.
    // -------------------------------------------------------------------

    protected List getAllObjects( Class clazz, String ordering )
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            Extent extent = pm.getExtent( clazz, true );

            Query query = pm.newQuery( extent );

            if ( ordering != null )
            {
                query.setOrdering( ordering );
            }

//            for ( Iterator i = fetchGroups.iterator(); i.hasNext(); )
//            {
//                pm.getFetchPlan().addGroup( (String) i.next() );
//            }

            List result = (List) query.execute();

            result = (List) pm.detachCopyAll( result );

            tx.commit();

            return result;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    protected Object getObjectByKey( Class clazz, Object key )
        throws JDOObjectNotFoundException, JDOException
    {
        if ( key == null )
        {
            throw new JDOException( "Unable to get object from jdo using null key." );
        }

        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            //            if ( fetchGroup != null )
            //            {
            //                pm.getFetchPlan().addGroup( fetchGroup );
            //            }

            Object objectId = pm.newObjectIdInstance( clazz, key.toString() );

            Object object = pm.getObjectById( objectId );

            object = pm.detachCopy( object );

            tx.commit();

            return object;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public void initialize()
        throws InitializationException
    {
        pmf = jdoFactory.getPersistenceManagerFactory();
    }

    protected void removeObject( Object o )
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            o = pm.getObjectById( pm.getObjectId( o ) );

            pm.deletePersistent( o );

            tx.commit();
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    protected Object saveObject( Object object )
    {
        return saveObject( object, null );
    }

    protected Object saveObject( Object object, String fetchGroups[] )
        throws JDOException
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            if ( ( JDOHelper.getObjectId( object ) != null ) && !JDOHelper.isDetached( object ) )
            {
                throw new JDOException( "Existing object is not detached: " + object );
            }

            if ( fetchGroups != null )
            {
                for ( int i = 0; i >= fetchGroups.length; i++ )
                {
                    pm.getFetchPlan().addGroup( fetchGroups[i] );
                }
            }

            pm.makePersistent( object );

            object = pm.detachCopy( object );

            tx.commit();

            return object;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    protected PersistenceManager getPersistenceManager()
    {
        PersistenceManager pm = pmf.getPersistenceManager();

        pm.getFetchPlan().setMaxFetchDepth( -1 );

        return pm;
    }

    protected static void closePersistenceManager( PersistenceManager pm )
    {
        try
        {
            pm.close();
        }
        catch ( JDOUserException e )
        {
            // ignore
        }
    }

    protected static void rollbackIfActive( Transaction tx )
    {
        PersistenceManager pm = tx.getPersistenceManager();

        try
        {
            if ( tx.isActive() )
            {
                tx.rollback();
            }
        }
        finally
        {
            closePersistenceManager( pm );
        }
    }
}
