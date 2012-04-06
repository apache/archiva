package org.codehaus.plexus.redback.configuration;

/*
 * Copyright 2001-2006 The Codehaus.
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

import org.codehaus.plexus.evaluator.DefaultExpressionEvaluator;
import org.codehaus.plexus.evaluator.EvaluatorException;
import org.codehaus.plexus.evaluator.ExpressionEvaluator;
import org.codehaus.plexus.evaluator.sources.SystemPropertyExpressionSource;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;

/**
 * ConfigurationFactory
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service( "userConfiguration" )
public class UserConfiguration
{
    private static final String DEFAULT_CONFIG_RESOURCE = "org/codehaus/plexus/redback/config-defaults.properties";

    protected Logger log = LoggerFactory.getLogger( getClass() );

    /**
     *
     *
     * @deprecated Please configure the Plexus registry instead
     */
    private List<String> configs;

    private Registry lookupRegistry;

    private static final String PREFIX = "org.codehaus.plexus.redback";

    @Inject
    @Named( value = "commons-configuration" )
    private Registry registry;

    //TODO move this method call in the constructor

    @PostConstruct
    public void initialize()
        throws RegistryException
    {
        performLegacyInitialization();

        try
        {
            registry.addConfigurationFromResource( DEFAULT_CONFIG_RESOURCE, PREFIX );
        }
        catch ( RegistryException e )
        {
            // Ok, not found in context classloader; try the one in this jar.

            ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
            try
            {

                Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
                registry.addConfigurationFromResource( DEFAULT_CONFIG_RESOURCE, PREFIX );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( prevCl );
            }
        }

        lookupRegistry = registry.getSubset( PREFIX );

        if ( log.isDebugEnabled() )
        {
            log.debug( lookupRegistry.dump() );
        }
    }

    private void performLegacyInitialization()
        throws RegistryException
    {
        ExpressionEvaluator evaluator = new DefaultExpressionEvaluator();
        evaluator.addExpressionSource( new SystemPropertyExpressionSource() );

        if ( configs != null )
        {
            if ( !configs.isEmpty() )
            {
                // TODO: plexus should be able to do this on it's own.
                log.warn(
                    "DEPRECATED: the <configs> elements is deprecated. Please configure the Plexus registry instead" );
            }

            for ( String configName : configs )
            {
                try
                {
                    configName = evaluator.expand( configName );
                }
                catch ( EvaluatorException e )
                {
                    log.warn( "Unable to resolve configuration name: " + e.getMessage(), e );
                }
                log.info( "Attempting to find configuration [{}] (resolved to [{}])", configName, configName );

                registry.addConfigurationFromFile( new File( configName ), PREFIX );
            }
        }
    }

    public String getString( String key )
    {
        return lookupRegistry.getString( key );
    }

    public String getString( String key, String defaultValue )
    {
        String value = lookupRegistry.getString( key, defaultValue );
        return value;
    }

    public int getInt( String key )
    {
        return lookupRegistry.getInt( key );
    }

    public int getInt( String key, int defaultValue )
    {
        return lookupRegistry.getInt( key, defaultValue );
    }

    public boolean getBoolean( String key )
    {
        return lookupRegistry.getBoolean( key );
    }

    public boolean getBoolean( String key, boolean defaultValue )
    {
        return lookupRegistry.getBoolean( key, defaultValue );
    }

    @SuppressWarnings( "unchecked" )
    public List<String> getList( String key )
    {
        return lookupRegistry.getList( key );
    }

    public String getConcatenatedList( String key, String defaultValue )
    {
        String concatenatedList;
        List<String> list = getList( key );
        if ( !list.isEmpty() )
        {
            StringBuilder s = new StringBuilder();
            for ( String value : list )
            {
                if ( s.length() > 0 )
                {
                    s.append( "," );
                }
                s.append( value );
            }
            concatenatedList = s.toString();
        }
        else
        {
            concatenatedList = defaultValue;
        }

        return concatenatedList;
    }

    /**
     * @return
     * @deprecated
     */
    public List<String> getConfigs()
    {
        return configs;
    }

    /**
     * @param configs
     * @deprecated
     */
    public void setConfigs( List<String> configs )
    {
        this.configs = configs;
    }

    public Registry getRegistry()
    {
        return registry;
    }

    public void setRegistry( Registry registry )
    {
        this.registry = registry;
    }
}
