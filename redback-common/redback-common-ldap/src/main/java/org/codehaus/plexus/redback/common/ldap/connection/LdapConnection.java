package org.codehaus.plexus.redback.common.ldap.connection;

/*
 * The MIT License
 * Copyright (c) 2005, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.sun.jndi.ldap.LdapCtxFactory;
import org.jvnet.animal_sniffer.IgnoreJRERequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
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
 * @version $Id$
 */
public class LdapConnection
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

    @IgnoreJRERequirement
    private static void initCtxFactory()
    {
        ctxFactory = new LdapCtxFactory();
    }

    @IgnoreJRERequirement
    public LdapConnection( LdapConnectionConfiguration config, Rdn subRdn )
        throws LdapException
    {
        this.config = config;

        LdapName baseDn = new LdapName( config.getBaseDn().getRdns() );

        if ( subRdn != null )
        {
            baseDn.add( subRdn );
        }

        baseDnRdns = Collections.unmodifiableList( baseDn.getRdns() );

        if ( context != null )
        {
            throw new LdapException( "Already connected." );
        }

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
    @IgnoreJRERequirement
    public LdapConnection( LdapConnectionConfiguration config, String bindDn, String password )
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
        if( config.getContextFactory().equals( "com.sun.jndi.ldap.LdapCtxFactory" ) )
        {
            env.put( "com.sun.jndi.ldap.connect.pool", "true");

            env.put( "com.sun.jndi.ldap.connect.pool.timeout", "3600" );
        }

        if ( config.getHostname() != null )
        {
            String protocol = config.isSsl() ? "ldaps" : "ldap";
            if ( config.getPort() != 0 )
            {
                env.put( Context.PROVIDER_URL, protocol + "://" + config.getHostname() + ":" + config.getPort() + "/" );
            }
            else
            {
                env.put( Context.PROVIDER_URL, protocol + "://" + config.getHostname() + "/" );
            }
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
