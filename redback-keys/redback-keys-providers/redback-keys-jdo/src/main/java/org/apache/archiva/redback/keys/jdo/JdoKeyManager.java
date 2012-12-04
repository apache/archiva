package org.apache.archiva.redback.keys.jdo;

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

import org.apache.archiva.redback.components.jdo.JdoFactory;
import org.apache.archiva.redback.components.jdo.RedbackJdoUtils;
import org.apache.archiva.redback.components.jdo.RedbackObjectNotFoundException;
import org.apache.archiva.redback.components.jdo.RedbackStoreException;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.AbstractKeyManager;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.keys.KeyNotFoundException;
import org.codehaus.plexus.util.StringUtils;
import org.jpox.PersistenceManagerFactoryImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.util.Calendar;
import java.util.List;

/**
 * JdoKeyManager
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@Service( "keyManager#jdo" )
public class JdoKeyManager
    extends AbstractKeyManager
{
    @Inject
    @Named( value = "jdoFactory#users" )
    private JdoFactory jdoFactory;

    private PersistenceManagerFactory pmf;

    public AuthenticationKey createKey( String principal, String purpose, int expirationMinutes )
        throws KeyManagerException
    {
        JdoAuthenticationKey authkey = new JdoAuthenticationKey();
        authkey.setKey( super.generateUUID() );
        authkey.setForPrincipal( principal );
        authkey.setPurpose( purpose );

        Calendar now = getNowGMT();
        authkey.setDateCreated( now.getTime() );

        if ( expirationMinutes >= 0 )
        {
            Calendar expiration = getNowGMT();
            expiration.add( Calendar.MINUTE, expirationMinutes );
            authkey.setDateExpires( expiration.getTime() );
        }

        return addKey( authkey );
    }

    public AuthenticationKey addKey( AuthenticationKey key )
    {
        return (AuthenticationKey) RedbackJdoUtils.addObject( getPersistenceManager(), key );
    }

    public void eraseDatabase()
    {
        RedbackJdoUtils.removeAll( getPersistenceManager(), JdoAuthenticationKey.class );
        RedbackJdoUtils.removeAll( getPersistenceManager(), RedbackKeyManagementJdoModelloMetadata.class );
    }

    public AuthenticationKey findKey( String key )
        throws KeyNotFoundException, KeyManagerException
    {
        if ( StringUtils.isEmpty( key ) )
        {
            throw new KeyNotFoundException( "Empty key not found." );
        }

        try
        {
            JdoAuthenticationKey authkey = (JdoAuthenticationKey) RedbackJdoUtils.getObjectById(
                getPersistenceManager(), JdoAuthenticationKey.class, key );

            if ( authkey == null )
            {
                throw new KeyNotFoundException( "Key [" + key + "] not found." );
            }
            assertNotExpired( authkey );

            return authkey;
        }
        catch ( RedbackObjectNotFoundException e )
        {
            throw new KeyNotFoundException( e.getMessage() );
        }
        catch ( RedbackStoreException e )
        {
            throw new KeyManagerException(
                "Unable to get " + JdoAuthenticationKey.class.getName() + "', key '" + key + "' from jdo store." );
        }
    }

    public void deleteKey( AuthenticationKey authkey )
        throws KeyManagerException
    {
        RedbackJdoUtils.removeObject( getPersistenceManager(), authkey );
    }

    public void deleteKey( String key )
        throws KeyManagerException
    {
        try
        {
            AuthenticationKey authkey = findKey( key );
            RedbackJdoUtils.removeObject( getPersistenceManager(), authkey );
        }
        catch ( KeyNotFoundException e )
        {
            // not found? nothing to do.
        }
    }

    @SuppressWarnings( "unchecked" )
    public List<AuthenticationKey> getAllKeys()
    {
        return RedbackJdoUtils.getAllObjectsDetached( getPersistenceManager(), JdoAuthenticationKey.class );
    }

    @PostConstruct
    public void initialize()
    {
        pmf = jdoFactory.getPersistenceManagerFactory();

        if ( pmf instanceof PersistenceManagerFactoryImpl )
        {
            PersistenceManagerFactoryImpl jpoxpmf = (PersistenceManagerFactoryImpl) pmf;
            if ( !StringUtils.equals( "JDK_DEFAULT_TIMEZONE", jpoxpmf.getDateTimezone() ) )
            {
                throw new RuntimeException( "The JdoFactory property 'org.jpox.rdbms.dateTimezone' MUST BE "
                                                       + "Set to 'JDK_DEFAULT_TIMEZONE' in order for jpox and JdoKeyManager to operate correctly." );
            }
        }
    }

    private PersistenceManager getPersistenceManager()
    {
        PersistenceManager pm = pmf.getPersistenceManager();

        pm.getFetchPlan().setMaxFetchDepth( 5 );

        return pm;
    }

    public String getId()
    {
        return "JDO Key Manager - " + this.getClass().getName();
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
