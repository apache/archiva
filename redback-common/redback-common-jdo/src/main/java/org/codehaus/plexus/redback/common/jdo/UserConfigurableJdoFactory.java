package org.codehaus.plexus.redback.common.jdo;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.jdo.DefaultConfigurableJdoFactory;
import org.codehaus.plexus.redback.configuration.UserConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * UserConfigurableJdoFactory
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service( "jdoFactory#users" )
public class UserConfigurableJdoFactory
    extends DefaultConfigurableJdoFactory
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "userConfiguration" )
    private UserConfiguration config;

    private String getConfigString( String key, String currentValue, String defaultValue )
    {
        String valueFromSysProps = System.getProperty( "redback." + key );
        if (StringUtils.isNotEmpty( valueFromSysProps ))
        {
            return valueFromSysProps;
        }
        String value = null;
        if ( StringUtils.isNotEmpty( currentValue ) )
        {
            value = config.getString( key, currentValue );
        }
        else
        {
            value = config.getString( key, defaultValue );
        }
        // do some interpolation as we can have some ${plexus.home} etc...
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource( new PropertiesBasedValueSource( System.getProperties() ) );

        try
        {
            return interpolator.interpolate( value );
        }
        catch ( InterpolationException e )
        {
            // ignore interpolation issue
            log.warn( "skip issue during interpolation " + e.getMessage() );
            return value;
        }
    }

    @PostConstruct
    public void initialize()
    {
        String jdbcDriverName =
            getConfigString( "jdbc.driver.name", super.getDriverName(), "org.apache.derby.jdbc.EmbeddedDriver" );
        String jdbcUrl =
            getConfigString( "jdbc.url", super.getUrl(), "jdbc:derby:${plexus.home}/database;create=true" );

        String jdbcUsername = getConfigString( "jdbc.username", super.getUserName(), "sa" );
        String jdbcPassword = getConfigString( "jdbc.password", super.getPassword(), "" );

        super.setDriverName( jdbcDriverName );
        super.setUrl( jdbcUrl );
        super.setUserName( jdbcUsername );
        super.setPassword( jdbcPassword );

        if ( StringUtils.isEmpty( super.persistenceManagerFactoryClass ) )
        {
            super.setPersistenceManagerFactoryClass( "org.jpox.PersistenceManagerFactoryImpl" );
        }

        if ( ( super.otherProperties == null ) || super.otherProperties.isEmpty() )
        {
            super.setProperty( "org.jpox.autoCreateSchema", "true" );
            super.setProperty( "org.jpox.validateSchema", "false" );
            super.setProperty( "org.jpox.validateTables", "false" );
            super.setProperty( "org.jpox.validateConstraints", "false" );
            super.setProperty( "org.jpox.transactionIsolation", "READ_COMMITTED" );
            super.setProperty( "org.jpox.rdbms.dateTimezone", "JDK_DEFAULT_TIMEZONE" );
        }

        super.initialize();
    }

    public UserConfiguration getConfig()
    {
        return config;
    }

    public void setConfig( UserConfiguration config )
    {
        this.config = config;
    }
}
