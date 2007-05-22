package org.apache.maven.archiva.database.jdo;

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
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.DeclarativeConstraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.SimpleConstraint;
import org.apache.maven.archiva.database.constraints.AbstractSimpleConstraint;
import org.apache.maven.archiva.model.CompoundKey;
import org.codehaus.plexus.jdo.JdoFactory;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.jpox.PMFConfiguration;
import org.jpox.SchemaTool;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
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
import javax.jdo.datastore.DataStoreCache;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.InstanceLifecycleListener;
import javax.jdo.listener.StoreLifecycleListener;
import javax.jdo.spi.Detachable;
import javax.jdo.spi.PersistenceCapable;

/**
 * JdoAccess 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.database.jdo.JdoAccess" role-hint="archiva"
 */
public class JdoAccess
    extends AbstractLogEnabled
    implements Initializable, InstanceLifecycleListener, StoreLifecycleListener
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private JdoFactory jdoFactory;

    private PersistenceManagerFactory pmf;

    public void initialize()
        throws InitializationException
    {
        pmf = jdoFactory.getPersistenceManagerFactory();

        /* Primitive (and failed) attempt at creating the schema on startup.
           Just to prevent the multiple stack trace warnings on auto-gen of schema.
         
        // Create the schema (if needed)
        URL jdoFileUrls[] = new URL[] { getClass().getResource( "/org/apache/maven/archiva/model/package.jdo" ) };

        File propsFile = null; // intentional
        boolean verbose = true;

        try
        {
            String connectionFactoryName = pmf.getConnectionFactoryName();
            if ( StringUtils.isNotBlank( connectionFactoryName ) && connectionFactoryName.startsWith( "java:comp" ) )
            {
                // We have a JNDI datasource!
                String jndiDatasource = connectionFactoryName;
                System.setProperty( PMFConfiguration.JDO_DATASTORE_URL_PROPERTY, jndiDatasource );
            }
            
            // TODO: figure out how to get the jdbc driver details from JNDI to pass into SchemaTool.

            SchemaTool.createSchemaTables( jdoFileUrls, new URL[] {}, propsFile, verbose, null );
        }
        catch ( Exception e )
        {
            getLogger().error( "Unable to create schema: " + e.getMessage(), e );
        }

        pmf.getPersistenceManager();
        */

        // Add the lifecycle listener.
        pmf.addInstanceLifecycleListener( this, null );
    }

    public static void dumpObjectState( PrintStream out, Object o )
    {
        final String STATE = "[STATE] ";
        final String INDENT = "        ";

        if ( o == null )
        {
            out.println( STATE + "Object is null." );
            return;
        }

        out.println( STATE + "Object " + o.getClass().getName() );

        if ( !( o instanceof PersistenceCapable ) )
        {
            out.println( INDENT + "is NOT PersistenceCapable (not a jdo object?)" );
            return;
        }

        out.println( INDENT + "is PersistenceCapable." );
        if ( o instanceof Detachable )
        {
            out.println( INDENT + "is Detachable" );
        }

        out.println( INDENT + "is new : " + Boolean.toString( JDOHelper.isNew( o ) ) );
        out.println( INDENT + "is transactional : " + Boolean.toString( JDOHelper.isTransactional( o ) ) );
        out.println( INDENT + "is deleted : " + Boolean.toString( JDOHelper.isDeleted( o ) ) );
        out.println( INDENT + "is detached : " + Boolean.toString( JDOHelper.isDetached( o ) ) );
        out.println( INDENT + "is dirty : " + Boolean.toString( JDOHelper.isDirty( o ) ) );
        out.println( INDENT + "is persistent : " + Boolean.toString( JDOHelper.isPersistent( o ) ) );

        out.println( INDENT + "object id : " + JDOHelper.getObjectId( o ) );
    }

    public PersistenceManager getPersistenceManager()
    {
        PersistenceManager pm = pmf.getPersistenceManager();

        pm.getFetchPlan().setMaxFetchDepth( -1 );

        return pm;
    }

    public void enableCache( Class clazz )
    {
        DataStoreCache cache = pmf.getDataStoreCache();
        cache.pinAll( clazz, false ); // Pin all objects of type clazz from now on
    }

    public Object saveObject( Object object )
    {
        return saveObject( object, null );
    }

    public Object saveObject( Object object, String[] fetchGroups )
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            if ( ( JDOHelper.getObjectId( object ) != null ) && !JDOHelper.isDetached( object ) )
            {
                // This is a fatal error that means we need to fix our code.
                // Leave it as a JDOUserException, it's intentional.
                throw new JDOUserException( "Existing object is not detached: " + object, object );
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

    public List getAllObjects( Class clazz )
    {
        return queryObjects( clazz, null );
    }

    public List queryObjects( Class clazz, Constraint constraint )
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            List result = null;

            if ( constraint != null )
            {
                if ( constraint instanceof DeclarativeConstraint )
                {
                    result = processConstraint( pm, clazz, (DeclarativeConstraint) constraint );
                }
                else if ( constraint instanceof AbstractSimpleConstraint )
                {
                    result = processConstraint( pm, (SimpleConstraint) constraint );
                }
                else
                {
                    result = processUnconstrained( pm, clazz );
                }
            }
            else
            {
                result = processUnconstrained( pm, clazz );
            }

            result = (List) pm.detachCopyAll( result );

            tx.commit();

            return result;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public List queryObjects( SimpleConstraint constraint )
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            List result = processConstraint( pm, constraint );

            // Only detach if results are known to be persistable.
            if ( constraint.isResultsPersistable() )
            {
                result = (List) pm.detachCopyAll( result );
            }
            else
            {
                List copiedResults = new ArrayList();
                copiedResults.addAll( result );
                result = copiedResults;
            }

            tx.commit();

            return result;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    private List processUnconstrained( PersistenceManager pm, Class clazz )
    {
        Extent extent = pm.getExtent( clazz, true );
        Query query = pm.newQuery( extent );
        return (List) query.execute();
    }

    private List processConstraint( PersistenceManager pm, SimpleConstraint constraint )
    {
        Query query = pm.newQuery( constraint.getSelectSql() );

        if ( constraint.getResultClass() == null )
        {
            throw new IllegalStateException( "Unable to use a SimpleConstraint with a null result class." );
        }

        query.setResultClass( constraint.getResultClass() );

        if ( constraint.getFetchLimits() != null )
        {
            pm.getFetchPlan().addGroup( constraint.getFetchLimits() );
        }

        if ( constraint.getParameters() != null )
        {
            return processParameterizedQuery( query, constraint.getParameters() );
        }

        return (List) query.execute();
    }

    private List processConstraint( PersistenceManager pm, Class clazz, DeclarativeConstraint constraint )
    {
        Extent extent = pm.getExtent( clazz, true );
        Query query = pm.newQuery( extent );

        if ( constraint.getSortColumn() != null )
        {
            String ordering = constraint.getSortColumn();

            if ( constraint.getSortDirection() != null )
            {
                ordering += " " + constraint.getSortDirection();
            }

            query.setOrdering( ordering );
        }

        if ( constraint.getFetchLimits() != null )
        {
            pm.getFetchPlan().addGroup( constraint.getFetchLimits() );
        }

        if ( constraint.getWhereCondition() != null )
        {
            query.setFilter( constraint.getWhereCondition() );
        }

        if ( constraint.getDeclaredImports() != null )
        {
            query.declareImports( StringUtils.join( constraint.getDeclaredImports(), ", " ) );
        }

        if ( constraint.getDeclaredParameters() != null )
        {
            if ( constraint.getParameters() == null )
            {
                throw new JDOException( "Unable to use query, there are declared parameters, "
                    + "but no parameter objects to use." );
            }

            if ( constraint.getParameters().length != constraint.getDeclaredParameters().length )
            {
                throw new JDOException( "Unable to use query, there are <" + constraint.getDeclaredParameters().length
                    + "> declared parameters, yet there are <" + constraint.getParameters().length
                    + "> parameter objects to use.  This should be equal." );
            }

            query.declareParameters( StringUtils.join( constraint.getDeclaredParameters(), ", " ) );

            return processParameterizedQuery( query, constraint.getParameters() );
        }
        else
        {
            return (List) query.execute();
        }
    }

    private List processParameterizedQuery( Query query, Object parameters[] )
    {
        switch ( parameters.length )
        {
            case 1:
                return (List) query.execute( parameters[0] );
            case 2:
                return (List) query.execute( parameters[0], parameters[1] );
            case 3:
                return (List) query.execute( parameters[0], parameters[1], parameters[2] );
            default:
                throw new JDOException( "Unable to use more than 3 parameters." );
        }
    }

    public Object getObjectById( Class clazz, Object id, String fetchGroup )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( id == null )
        {
            throw new ObjectNotFoundException( "Unable to get object '" + clazz.getName() + "' from jdo using null id." );
        }

        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            if ( fetchGroup != null )
            {
                pm.getFetchPlan().addGroup( fetchGroup );
            }

            Object objectId = null;

            if ( id instanceof CompoundKey )
            {
                objectId = pm.newObjectIdInstance( clazz, id.toString() );
            }
            else
            {
                objectId = pm.newObjectIdInstance( clazz, id );
            }

            Object object = pm.getObjectById( objectId );

            object = pm.detachCopy( object );

            tx.commit();

            return object;
        }
        catch ( JDOObjectNotFoundException e )
        {
            throw new ObjectNotFoundException( "Unable to find Database Object [" + id + "] of type " + clazz.getName()
                + " using " + ( ( fetchGroup == null ) ? "no fetch-group" : "a fetch-group of [" + fetchGroup + "]" ),
                                               e, id );
        }
        catch ( JDOException e )
        {
            throw new ArchivaDatabaseException( "Error in JDO during get of Database object id [" + id + "] of type "
                + clazz.getName() + " using "
                + ( ( fetchGroup == null ) ? "no fetch-group" : "a fetch-group of [" + fetchGroup + "]" ), e );
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public Object getObjectById( Class clazz, String id, String fetchGroup )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( StringUtils.isEmpty( id ) )
        {
            throw new ObjectNotFoundException( "Unable to get object '" + clazz.getName()
                + "' from jdo using null/empty id." );
        }

        return getObjectById( clazz, (Object) id, fetchGroup );
    }

    public boolean objectExists( Object object )
    {
        return ( JDOHelper.getObjectId( object ) != null );
    }

    public boolean objectExistsById( Class clazz, String id )
        throws ArchivaDatabaseException
    {
        try
        {
            Object o = getObjectById( clazz, id, null );
            return ( o != null );
        }
        catch ( ObjectNotFoundException e )
        {
            return false;
        }
    }

    public void removeObject( Object o )
        throws ArchivaDatabaseException
    {
        if ( o == null )
        {
            throw new ArchivaDatabaseException( "Unable to remove null object '" + o.getClass().getName() + "'" );
        }

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

    public void rollbackIfActive( Transaction tx )
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

    public void closePersistenceManager( PersistenceManager pm )
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

    public void postDelete( InstanceLifecycleEvent evt )
    {
        PersistenceCapable obj = ( (PersistenceCapable) evt.getSource() );

        if ( obj == null )
        {
            // Do not track null objects.
            // These events are typically a product of an internal lifecycle event.
            return;
        }
    }

    public void preDelete( InstanceLifecycleEvent evt )
    {
        // ignore
    }

    public void postStore( InstanceLifecycleEvent evt )
    {
        // PersistenceCapable obj = ( (PersistenceCapable) evt.getSource() );
    }

    public void preStore( InstanceLifecycleEvent evt )
    {
        // ignore
    }

    public void removeAll( Class aClass )
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            Query query = pm.newQuery( aClass );
            query.deletePersistentAll();

            tx.commit();
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public JdoFactory getJdoFactory()
    {
        return jdoFactory;
    }

}
