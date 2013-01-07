package org.apache.archiva.redback.rbac.jdo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManagerListener;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.components.jdo.JdoFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
import javax.jdo.listener.DeleteLifecycleListener;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.StoreLifecycleListener;
import javax.jdo.spi.Detachable;
import javax.jdo.spi.PersistenceCapable;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * JdoTool - RBAC JDO Tools.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@Service("jdoTool")
public class JdoTool
    implements DeleteLifecycleListener, StoreLifecycleListener
{

    @Resource(name="jdoFactory#users")
    private JdoFactory jdoFactory;

    private PersistenceManagerFactory pmf;

    private RBACManagerListener listener;

    @PostConstruct
    public void initialize()
    {
        pmf = jdoFactory.getPersistenceManagerFactory();

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

        triggerInit();

        return pm;
    }

    private boolean hasTriggeredInit = false;

    @SuppressWarnings("unchecked")
    public void triggerInit()
    {
        if ( !hasTriggeredInit )
        {
            hasTriggeredInit = true;

            List<Role> roles = (List<Role>) getAllObjects( JdoRole.class );

            listener.rbacInit( roles.isEmpty() );
        }
    }

    public void enableCache( Class<?> clazz )
    {
        DataStoreCache cache = pmf.getDataStoreCache();
        if ( cache.getClass().getName().equals( "org.jpox.cache.EhcacheClassBasedLevel2Cache" )
            || cache.getClass().getName().equals( "org.jpox.cache.EhcacheLevel2Cache" ) )
        {
            /* Ehcache adapters don't support pinAll, the caching is handled in the configuration */
            return;
        }
        cache.pinAll( clazz, false ); // Pin all objects of type clazz from now on
    }

    public <T>T saveObject( T object )
    {
        return (T) saveObject( object, null );
    }

    public <T>T saveObject( T object, String[] fetchGroups )
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

            object = (T) pm.detachCopy( object );

            tx.commit();

            return object;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public List<?> getAllObjects( Class<?> clazz )
    {
        return getAllObjects( clazz, null, null );
    }

    public List<?> getAllObjects( Class<?> clazz, String ordering )
    {
        return getAllObjects( clazz, ordering, null );
    }

    public List<?> getAllObjects( Class<?> clazz, String ordering, String fetchGroup )
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

            if ( fetchGroup != null )
            {
                pm.getFetchPlan().addGroup( fetchGroup );
            }

            List<?> result = (List<?>) query.execute();

            result = (List<?>) pm.detachCopyAll( result );

            tx.commit();

            return result;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public List<?> getUserAssignmentsForRoles( Class<?> clazz, String ordering, Collection<String> roleNames )
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

            query.declareImports( "import java.lang.String" );

            StringBuilder filter = new StringBuilder();

            if ( roleNames.size() > 0 )
            {
                Iterator<String> i = roleNames.iterator();

                filter.append( "this.roleNames.contains(\"" ).append( i.next() ).append( "\")" );

                while ( i.hasNext() )
                {
                    filter.append( " || this.roleNames.contains(\"" ).append( i.next() ).append( "\")" );
                }

                query.setFilter( filter.toString() );
            }

            List<?> result = (List<?>) query.execute();

            result = (List<?>) pm.detachCopyAll( result );

            tx.commit();

            return result;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public <T>T getObjectById( Class<T> clazz, String id, String fetchGroup )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        if ( StringUtils.isEmpty( id ) )
        {
            throw new RbacObjectNotFoundException(
                "Unable to get object '" + clazz.getName() + "' from jdo using null/empty id." );
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

            Object objectId = pm.newObjectIdInstance( clazz, id );

            Object object = pm.getObjectById( objectId );

            object = pm.detachCopy( object );

            tx.commit();

            return (T) object;
        }
        catch ( JDOObjectNotFoundException e )
        {
            throw new RbacObjectNotFoundException( "Unable to find RBAC Object '" + id + "' of type " +
                clazz.getName() + " using fetch-group '" + fetchGroup + "'", e, id );
        }
        catch ( JDOException e )
        {
            throw new RbacManagerException( "Error in JDO during get of RBAC object id '" + id + "' of type " +
                clazz.getName() + " using fetch-group '" + fetchGroup + "'", e );
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public boolean objectExists( Object object )
    {
        return ( JDOHelper.getObjectId( object ) != null );
    }

    public boolean objectExistsById( Class<?> clazz, String id )
        throws RbacManagerException
    {
        try
        {
            Object o = getObjectById( clazz, id, null );
            return ( o != null );
        }
        catch ( RbacObjectNotFoundException e )
        {
            return false;
        }
    }

    public <T>T removeObject( T o )
        throws RbacManagerException
    {
        if ( o == null )
        {
            throw new RbacManagerException( "Unable to remove null object" );
        }

        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            o = (T) pm.getObjectById( pm.getObjectId( o ) );

            pm.deletePersistent( o );

            tx.commit();

            return o;
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

    public RBACManagerListener getListener()
    {
        return listener;
    }

    public void setListener( RBACManagerListener listener )
    {
        this.listener = listener;
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

        if ( obj instanceof Role )
        {
            listener.rbacRoleRemoved( (Role) obj );
        }
        else if ( obj instanceof Permission )
        {
            listener.rbacPermissionRemoved( (Permission) obj );
        }
    }

    public void preDelete( InstanceLifecycleEvent evt )
    {
        // ignore
    }

    public void postStore( InstanceLifecycleEvent evt )
    {
        PersistenceCapable obj = ( (PersistenceCapable) evt.getSource() );

        if ( obj instanceof Role )
        {
            listener.rbacRoleSaved( (Role) obj );
        }
        else if ( obj instanceof Permission )
        {
            listener.rbacPermissionSaved( (Permission) obj );
        }
    }

    public void preStore( InstanceLifecycleEvent evt )
    {
        // ignore
    }

    public void removeAll( Class<?> aClass )
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

    public void setJdoFactory( JdoFactory jdoFactory )
    {
        this.jdoFactory = jdoFactory;
    }
}
