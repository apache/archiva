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

import org.codehaus.plexus.redback.configuration.UserConfiguration;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.StateFactory;
import java.util.Properties;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
@Service( "ldapConnectionFactory#configurable" )
public class ConfigurableLdapConnectionFactory
    implements LdapConnectionFactory
{
    /**
     *
     */
    private String hostname;

    /**
     *
     */
    private int port;

    /**
     *
     */
    private boolean ssl;

    /**
     *
     */
    private String baseDn;

    /**
     *
     */
    private String contextFactory;

    /**
     *
     */
    private String bindDn;

    /**
     *
     */
    private String password;

    /**
     *
     */
    private String authenticationMethod;

    /**
     *
     */
    private Properties extraProperties;

    private LdapConnectionConfiguration configuration;


    @Inject
    @Named( value = "userConfiguration" )
    private UserConfiguration userConf;

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------
    @PostConstruct
    public void initialize()
    {
        try
        {
            configuration = new LdapConnectionConfiguration();
            configuration.setHostname( userConf.getString( "ldap.config.hostname", hostname ) );
            configuration.setPort( userConf.getInt( "ldap.config.port", port ) );
            configuration.setSsl( userConf.getBoolean( "ldap.config.ssl", ssl ) );
            configuration.setBaseDn( userConf.getConcatenatedList( "ldap.config.base.dn", baseDn ) );
            configuration.setContextFactory( userConf.getString( "ldap.config.context.factory", contextFactory ) );
            configuration.setBindDn( userConf.getConcatenatedList( "ldap.config.bind.dn", bindDn ) );
            configuration.setPassword( userConf.getString( "ldap.config.password", password ) );
            configuration.setAuthenticationMethod(
                userConf.getString( "ldap.config.authentication.method", authenticationMethod ) );
            configuration.setExtraProperties( extraProperties );
        }
        catch ( InvalidNameException e )
        {
            throw new RuntimeException( "Error while initializing connection factory.", e );
        }
    }

    // ----------------------------------------------------------------------
    // LdapConnectionFactory Implementation
    // ----------------------------------------------------------------------

    public LdapConnection getConnection()
        throws LdapException
    {
        return new LdapConnection( configuration, null );
    }

    public LdapConnection getConnection( Rdn subRdn )
        throws LdapException
    {
        return new LdapConnection( configuration, subRdn );
    }

    public LdapConnection getConnection( String bindDn, String password )
        throws LdapException
    {
        return new LdapConnection( configuration, bindDn, password );
    }

    public LdapName getBaseDnLdapName()
        throws LdapException
    {
        try
        {
            return new LdapName( baseDn );
        }
        catch ( InvalidNameException e )
        {
            throw new LdapException( "The base DN is not a valid name.", e );
        }
    }

    public void addObjectFactory( Class<? extends ObjectFactory> objectFactoryClass )
    {
        configuration.getObjectFactories().add( objectFactoryClass );
    }

    public void addStateFactory( Class<? extends StateFactory> stateFactoryClass )
    {
        configuration.getStateFactories().add( stateFactoryClass );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String toString()
    {
        return "{ConfigurableLdapConnectionFactory: configuration: " + configuration + "}";
    }

    public LdapConnectionConfiguration getConfiguration()
    {
        return configuration;
    }

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

    public String getBaseDn()
    {
        return baseDn;
    }

    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }

    public String getContextFactory()
    {
        return contextFactory;
    }

    public void setContextFactory( String contextFactory )
    {
        this.contextFactory = contextFactory;
    }

    public String getBindDn()
    {
        return bindDn;
    }

    public void setBindDn( String bindDn )
    {
        this.bindDn = bindDn;
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

    public Properties getExtraProperties()
    {
        return extraProperties;
    }

    public void setExtraProperties( Properties extraProperties )
    {
        this.extraProperties = extraProperties;
    }

    public UserConfiguration getUserConf()
    {
        return userConf;
    }

    public void setUserConf( UserConfiguration userConf )
    {
        this.userConf = userConf;
    }
}
