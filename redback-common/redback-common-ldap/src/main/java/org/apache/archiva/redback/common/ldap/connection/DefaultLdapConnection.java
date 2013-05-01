package org.apache.archiva.redback.common.ldap.connection;

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

import com.sun.jndi.ldap.LdapCtxFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * The configuration for a connection will not change.
 *
 * @author <a href="mailto:trygvis@inamo.no">trygvis</a>
 */
public class DefaultLdapConnection
{

    private static LdapCtxFactory ctxFactory;// = new LdapCtxFactory();


    static
    {
        initCtxFactory();
    }


    private Logger log = LoggerFactory.getLogger( getClass() );

    private LdapConnectionConfiguration config;

    private DirContext context;

    private List<Rdn> baseDnRdns;

    private static void initCtxFactory()
    {
        ctxFactory = new LdapCtxFactory();
    }

    public DefaultLdapConnection( LdapConnectionConfiguration config, Rdn subRdn )
        throws LdapException
    {
        this.config = config;

        LdapName baseDn = new LdapName( config.getBaseDn().getRdns() );

        if ( subRdn != null )
        {
            baseDn.add( subRdn );
        }

        log.debug( "baseDn: {}", baseDn );

        baseDnRdns = Collections.unmodifiableList( baseDn.getRdns() );

        if ( context != null )
        {
            throw new LdapException( "Already connected." );
        }

        log.debug( "baseDnRdns: {}", baseDnRdns );

        Hashtable<Object, Object> e = getEnvironment();

        try
        {
            context = (DirContext) ctxFactory.getInitialContext( e );
        }
        catch ( NamingException ex )
        {
            throw new LdapException( "Could not connect to the server.", ex );
        }
    }

    /**
     * This ldap connection will attempt to establish a connection using the configuration,
     * replacing the principal and the password
     *
     * @param config
     * @param bindDn
     * @param password
     * @throws LdapException
     */
    public DefaultLdapConnection( LdapConnectionConfiguration config, String bindDn, String password )
        throws LdapException
    {
        this.config = config;

        Hashtable<Object, Object> e = getEnvironment();

        e.put( Context.SECURITY_PRINCIPAL, bindDn );
        e.put( Context.SECURITY_CREDENTIALS, password );

        try
        {
            context = (DirContext) ctxFactory.getInitialContext( e );
        }
        catch ( NamingException ex )
        {
            throw new LdapException( "Could not connect to the server.", ex );
        }
    }

    // ----------------------------------------------------------------------
    // Connection Managment
    // ----------------------------------------------------------------------

    public Hashtable<Object, Object> getEnvironment()
        throws LdapException
    {
        Properties env = new Properties();

        env.putAll( config.getExtraProperties() );

        config.check();

        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getContextFactory() );

        // REDBACK-289/MRM-1488
        // enable connection pooling when using Sun's LDAP context factory
        if ( config.getContextFactory().equals( "com.sun.jndi.ldap.LdapCtxFactory" ) )
        {
            env.put( "com.sun.jndi.ldap.connect.pool", "true" );

            env.put( "com.sun.jndi.ldap.connect.pool.timeout", "3600" );
        }

        if ( config.getHostname() != null )
        {
            String protocol = "ldap";// config.isSsl() ? "ldaps" : "ldap";
            if ( config.getPort() != 0 )
            {
                env.put( Context.PROVIDER_URL, protocol + "://" + config.getHostname() + ":" + config.getPort() + "/" );
            }
            else
            {
                env.put( Context.PROVIDER_URL, protocol + "://" + config.getHostname() + "/" );
            }
        }

        if ( config.isSsl() )
        {
            env.put( Context.SECURITY_PROTOCOL, "ssl" );
        }

        if ( config.getAuthenticationMethod() != null )
        {
            env.put( Context.SECURITY_AUTHENTICATION, config.getAuthenticationMethod() );
        }

        if ( config.getBindDn() != null )
        {
            env.put( Context.SECURITY_PRINCIPAL, config.getBindDn().toString() );
        }

        if ( config.getPassword() != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, config.getPassword() );
        }

        // ----------------------------------------------------------------------
        // Object Factories
        // ----------------------------------------------------------------------

        String objectFactories = null;

        for ( Class<?> objectFactoryClass : config.getObjectFactories() )
        {
            if ( objectFactories == null )
            {
                objectFactories = objectFactoryClass.getName();
            }
            else
            {
                objectFactories += ":" + objectFactoryClass.getName();
            }
        }

        if ( objectFactories != null )
        {
            env.setProperty( Context.OBJECT_FACTORIES, objectFactories );
        }

        // ----------------------------------------------------------------------
        // State Factories
        // ----------------------------------------------------------------------

        String stateFactories = null;

        for ( Class<?> stateFactoryClass : config.getStateFactories() )
        {
            if ( stateFactories == null )
            {
                stateFactories = stateFactoryClass.getName();
            }
            else
            {
                stateFactories += ":" + stateFactoryClass.getName();
            }
        }

        if ( stateFactories != null )
        {
            env.setProperty( Context.STATE_FACTORIES, stateFactories );
        }

        log.debug( "env properties: {}", env );

        return env;
    }

    public void close()
    {
        try
        {
            if ( context != null )
            {
                context.close();
            }
        }
        catch ( NamingException ex )
        {
            log.info( "skip error closing ldap connection {}", ex.getMessage() );
        }
        finally
        {
            context = null;
        }
    }

    // ----------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------

    public LdapConnectionConfiguration getConfiguration()
    {
        return config;
    }

    public List<Rdn> getBaseDnRdns()
    {
        return baseDnRdns;
    }

    public DirContext getDirContext()
    {
        return context;
    }
}
