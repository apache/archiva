package org.apache.maven.archiva.cli;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.archiva.Archiva;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.List;
import java.util.Arrays;

/**
 * @author jason van zyl
 * @version $Id$
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 * @todo complete separate out the general cli processing
 * @todo create a simple component to do the invocation
 */
public class Cli
{
    public static final String SOURCE_REPO_PATH = "sourceRepositoryPath";

    public static final String TARGET_REPO_PATH = "targetRepositoryPath";

    public static final String BLACKLISTED_PATTERNS = "blacklistPatterns";

    public static void main( String[] args )
    {
        ClassWorld classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );

        int result = main( args, classWorld );

        System.exit( result );
    }

    /**
     * @noinspection ConfusingMainMethod
     */
    public static int main( String[] args,
                            ClassWorld classWorld )
    {
        // ----------------------------------------------------------------------
        // Setup the command line parser
        // ----------------------------------------------------------------------

        CliManager cliManager = new CliManager();

        CommandLine cli;

        try
        {
            cli = cliManager.parse( args );
        }
        catch ( ParseException e )
        {
            System.err.println( "Unable to parse command line options: " + e.getMessage() );

            cliManager.displayHelp();

            return 1;
        }

        if ( System.getProperty( "java.class.version", "44.0" ).compareTo( "48.0" ) < 0 )
        {
            System.err.println( "Sorry, but JDK 1.4 or above is required to execute Maven" );

            System.err.println(
                "You appear to be using Java version: " + System.getProperty( "java.version", "<unknown>" ) );

            return 1;
        }

        boolean debug = cli.hasOption( CliManager.DEBUG );

        boolean quiet = !debug && cli.hasOption( CliManager.QUIET );

        boolean showErrors = debug || cli.hasOption( CliManager.ERRORS );

        if ( showErrors )
        {
            System.out.println( "+ Error stacktraces are turned on." );
        }

        // ----------------------------------------------------------------------------
        // Logging
        // ----------------------------------------------------------------------------

        int loggingLevel;

        if ( debug )
        {
            loggingLevel = 0; //MavenExecutionRequest.LOGGING_LEVEL_DEBUG;
        }
        else if ( quiet )
        {
            loggingLevel = 0; //MavenExecutionRequest.LOGGING_LEVEL_ERROR;
        }
        else
        {
            loggingLevel = 0; //MavenExecutionRequest.LOGGING_LEVEL_INFO;
        }

        // ----------------------------------------------------------------------
        // Process particular command line options
        // ----------------------------------------------------------------------

        if ( cli.hasOption( CliManager.HELP ) )
        {
            cliManager.displayHelp();

            return 0;
        }

        if ( cli.hasOption( CliManager.VERSION ) )
        {
            showVersion();

            return 0;
        }
        else if ( debug )
        {
            showVersion();
        }

        // ----------------------------------------------------------------------------
        // This is what we will generalize for the invocation of the command line.
        // ----------------------------------------------------------------------------

        try
        {
            PlexusContainer plexus = new DefaultPlexusContainer( "plexus.core", classWorld );

            Archiva archiva = (Archiva) plexus.lookup( Archiva.ROLE );

            if ( cli.hasOption( CliManager.CONVERT ) )
            {
                Properties p = new Properties();

                try
                {
                    p.load( new FileInputStream( cli.getOptionValue( CliManager.CONVERT ) ) );
                }
                catch ( IOException e )
                {
                    showFatalError( "Cannot find properties file which describes the conversion.", e, true );
                }

                File oldRepositoryPath = new File( p.getProperty( SOURCE_REPO_PATH ) );

                File newRepositoryPath = new File( p.getProperty( TARGET_REPO_PATH ) );

                System.out.println( "Converting " + oldRepositoryPath + " to " + newRepositoryPath );

                List blacklistedPatterns = null;

                String s = p.getProperty( BLACKLISTED_PATTERNS );

                if ( s != null )
                {
                    blacklistedPatterns = Arrays.asList( StringUtils.split( s, "," ) );
                }

                try
                {
                    archiva.convertLegacyRepository( oldRepositoryPath, newRepositoryPath, blacklistedPatterns, true );
                }
                catch ( RepositoryConversionException e )
                {
                    showFatalError( "Error converting repository.", e, true );
                }
                catch ( DiscovererException e )
                {
                    showFatalError( "Error discovery artifacts to convert.", e, true );
                }
            }

        }
        catch ( PlexusContainerException e )
        {
            showFatalError( "Cannot create Plexus container.", e, true );
        }
        catch ( ComponentLookupException e )
        {
            showError( "Cannot lookup application component.", e, true );
        }

        return 0;
    }

    private static int showFatalError( String message,
                                       Exception e,
                                       boolean show )
    {
        System.err.println( "FATAL ERROR: " + message );

        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
        else
        {
            System.err.println( "For more information, run with the -e flag" );
        }

        return 1;
    }

    private static void showError( String message,
                                   Exception e,
                                   boolean show )
    {
        System.err.println( message );

        if ( show )
        {
            System.err.println( "Error stacktrace:" );

            e.printStackTrace();
        }
    }

    // Need to get the versions of the application in a general way, so that I need a way to get the
    // specifics of the application so that I can do this in a general way.
    private static void showVersion()
    {
        InputStream resourceAsStream;
        try
        {
            Properties properties = new Properties();
            resourceAsStream = Cli.class.getClassLoader().getResourceAsStream(
                "META-INF/maven/org.apache.maven/maven-core/pom.properties" );
            properties.load( resourceAsStream );

            if ( properties.getProperty( "builtOn" ) != null )
            {
                System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" ) + " built on " +
                    properties.getProperty( "builtOn" ) );
            }
            else
            {
                System.out.println( "Maven version: " + properties.getProperty( "version", "unknown" ) );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    private static Properties getExecutionProperties( CommandLine commandLine )
    {
        Properties executionProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CliManager.SET_SYSTEM_PROPERTY ) )
        {
            String[] defStrs = commandLine.getOptionValues( CliManager.SET_SYSTEM_PROPERTY );

            for ( int i = 0; i < defStrs.length; ++i )
            {
                setCliProperty( defStrs[i], executionProperties );
            }
        }

        executionProperties.putAll( System.getProperties() );

        return executionProperties;
    }

    private static void setCliProperty( String property,
                                        Properties executionProperties )
    {
        String name;

        String value;

        int i = property.indexOf( "=" );

        if ( i <= 0 )
        {
            name = property.trim();

            value = "true";
        }
        else
        {
            name = property.substring( 0, i ).trim();

            value = property.substring( i + 1 ).trim();
        }

        executionProperties.setProperty( name, value );

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------

        System.setProperty( name, value );
    }
}
