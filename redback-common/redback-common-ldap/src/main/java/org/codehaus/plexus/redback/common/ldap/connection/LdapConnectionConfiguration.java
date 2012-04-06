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

import org.apache.commons.lang.StringUtils;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

/**
 * This class contains the configuration for a ldap connection.
 * <p/>
 * Properties of a ldap connection:
 * <ul>
 * <li>Hostname - String, required.
 * <li>Port - int, not required. If 0 then the default value is used by the ldap driver.
 * <li>Ssl - boolean, not required. If true then the ldaps will be used.
 * <li>Base DN - String, required.
 * <li>Context factory - String, required.
 * <li>Bind DN - String, not required.
 * <li>Password - String, not required.
 * </ul>
 * Note that both the bind dn and password must be set if any are set.
 *
 * @author <a href="mailto:trygvis@inamo.no">trygvis</a>
 * @version $Id$
 */
public class LdapConnectionConfiguration
{
    private String hostname;

    private int port;

    private boolean ssl;

    private LdapName baseDn;

    private String contextFactory;

    private LdapName bindDn;

    private String password;

    private String authenticationMethod;

    private List<Class<?>> objectFactories;

    private List<Class<?>> stateFactories;

    private Properties extraProperties;

    public LdapConnectionConfiguration()
    {
    }

    public LdapConnectionConfiguration( String hostname, int port, LdapName baseDn, String contextFactory,
                                        LdapName bindDn, String password, String authenticationMethod,
                                        Properties extraProperties )
        throws LdapException
    {
        this.hostname = hostname;

        this.port = port;

        if ( baseDn != null )
        {
            this.baseDn = new LdapName( baseDn.getRdns() );
        }

        this.contextFactory = contextFactory;

        if ( bindDn != null )
        {
            this.bindDn = new LdapName( bindDn.getRdns() );
        }

        this.password = password;

        this.authenticationMethod = authenticationMethod;

        this.extraProperties = extraProperties;

        check();
    }

    public LdapConnectionConfiguration( String hostname, int port, String baseDn, String contextFactory, String bindDn,
                                        String password, String authenticationMethod, Properties extraProperties )
        throws InvalidNameException, LdapException
    {
        this.hostname = hostname;
        this.port = port;

        if ( baseDn != null )
        {
            this.baseDn = new LdapName( baseDn );
        }

        if ( bindDn != null )
        {
            this.bindDn = new LdapName( bindDn );
        }

        this.contextFactory = contextFactory;

        this.password = password;

        this.authenticationMethod = authenticationMethod;

        this.extraProperties = extraProperties;

        check();
    }

    public LdapConnectionConfiguration( String hostname, int port, LdapName baseDn, String contextFactory )
        throws LdapException
    {
        this.hostname = hostname;

        this.port = port;

        this.baseDn = baseDn;

        this.contextFactory = contextFactory;

        check();
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public String getHostname()
    {
        return hostname;
    }

    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    public boolean isSsl()
    {
        return ssl;
    }

    public void setSsl( boolean ssl )
    {
        this.ssl = ssl;
    }

    public LdapName getBaseDn()
    {
        return baseDn;
    }

    public void setBaseDn( LdapName baseDn )
    {
        this.baseDn = baseDn;
    }

    public void setBaseDn( String baseDn )
        throws InvalidNameException
    {
        if ( baseDn != null )
        {
            this.baseDn = new LdapName( baseDn );
        }
    }

    public String getContextFactory()
    {
        return contextFactory;
    }

    public void setContextFactory( String contextFactory )
    {
        this.contextFactory = contextFactory;
    }

    public LdapName getBindDn()
    {
        return bindDn;
    }

    public void setBindDn( LdapName bindDn )
    {
        this.bindDn = bindDn;
    }

    public void setBindDn( String bindDn )
        throws InvalidNameException
    {
        if ( bindDn != null )
        {
            this.bindDn = new LdapName( bindDn );
        }
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getAuthenticationMethod()
    {
        return authenticationMethod;
    }

    public void setAuthenticationMethod( String authenticationMethod )
    {
        this.authenticationMethod = authenticationMethod;
    }

    public List<Class<?>> getObjectFactories()
    {
        if ( objectFactories == null )
        {
            objectFactories = new ArrayList<Class<?>>( 0 );
        }

        return objectFactories;
    }

    public void setObjectFactories( List<Class<?>> objectFactories )
    {
        this.objectFactories = objectFactories;
    }

    public List<Class<?>> getStateFactories()
    {
        if ( stateFactories == null )
        {
            stateFactories = new ArrayList<Class<?>>( 0 );
        }

        return stateFactories;
    }

    public void setStateFactories( List<Class<?>> stateFactories )
    {
        this.stateFactories = stateFactories;
    }

    public Properties getExtraProperties()
    {
        if ( extraProperties == null )
        {
            extraProperties = new Properties();
        }

        return extraProperties;
    }

    public void setExtraProperties( Properties extraProperties )
    {
        this.extraProperties = extraProperties;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void check()
        throws LdapException
    {
        if ( port < 0 || port > 65535 )
        {
            throw new LdapException( "The port must be between 1 and 65535." );
        }
        if ( baseDn == null )
        {
            throw new LdapException( "The base DN must be set." );
        }
        if ( StringUtils.isEmpty( contextFactory ) )
        {
            throw new LdapException( "The context factory must be set." );
        }
        if ( password != null && bindDn == null )
        {
            throw new LdapException( "The password cant be set unless the bind dn is." );
        }

        if ( extraProperties == null )
        {
            extraProperties = new Properties();
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String toString()
    {
        return "{LdapConnectionConfiguration: " +
            "hostname: " + getHostname() + ", " +
            "port: " + getPort() + ", " +
            "ssl: " + isSsl() + ", " +
            "baseDn: " + getBaseDn() + ", " +
            "contextFactory: " + getContextFactory() + ", " +
            "bindDn: " + getBindDn() + ", " +
            "password: " + getPassword() + ", " +
            "authenticationMethod: " + getAuthenticationMethod() + ", " +
            "objectFactories: " + getObjectFactories() + ", " +
            "stateFactories: " + getStateFactories() + ", " +
            "extraProperties: " + new TreeMap<Object, Object>( extraProperties ).toString() + "}";
    }
}
