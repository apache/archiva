package org.apache.maven.archiva.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author jason van zyl
 * @version $Id$
 * @noinspection UseOfSystemOutOrSystemErr,ACCESS_STATIC_VIA_INSTANCE
 */
public abstract class AbstractCli
    implements Cli
{
    // ----------------------------------------------------------------------------
    // These are standard options that we would want to use for all our projects.
    // ----------------------------------------------------------------------------

    public static final char QUIET = 'q';

    public static final char DEBUG = 'X';

    public static final char ERRORS = 'e';

    public static final char HELP = 'h';

    public static final char VERSION = 'v';

    public static final char SET_SYSTEM_PROPERTY = 'D';

    // ----------------------------------------------------------------------------
    // Abstract methods
    // ----------------------------------------------------------------------------

    protected static Cli getCli()
    {
        throw new UnsupportedOperationException( "You must implement this getCli() in your subclass." );
    }

    protected abstract Options buildOptions( Options options );

    protected abstract void processOptions( CommandLine cli,
                                            PlexusContainer container )
        throws Exception;

    protected abstract String getPomPropertiesPath();

    // ----------------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------------

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
        return getCli().execute( args, classWorld );
    }

    public int execute( String[] args,
                        ClassWorld classWorld )
    {
        CommandLine cli;

        try
        {
            cli = parse( args );
        }
        catch ( ParseException e )
        {
            System.err.println( "Unable to parse command line options: " + e.getMessage() );

            displayHelp();

            return 1;
        }

        if ( System.getProperty( "java.class.version", "44.0" ).compareTo( "48.0" ) < 0 )
        {
            System.err.println( "Sorry, but JDK 1.4 or above is required to execute Maven" );

            System.err.println(
                "You appear to be using Java version: " + System.getProperty( "java.version", "<unknown>" ) );

            return 1;
        }

        boolean debug = cli.hasOption( DEBUG );

        boolean quiet = !debug && cli.hasOption( QUIET );

        boolean showErrors = debug || cli.hasOption( ERRORS );

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
            loggingLevel = 0;
        }
        else if ( quiet )
        {
            loggingLevel = 0;
        }
        else
        {
            loggingLevel = 0;
        }

        // ----------------------------------------------------------------------
        // Process particular command line options
        // ----------------------------------------------------------------------

        if ( cli.hasOption( HELP ) )
        {
            displayHelp();

            return 0;
        }

        if ( cli.hasOption( VERSION ) )
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

            processOptions( cli, plexus );
        }
        catch ( PlexusContainerException e )
        {
            showFatalError( "Cannot create Plexus container.", e, true );
        }
        catch ( ComponentLookupException e )
        {
            showError( "Cannot lookup application component.", e, true );
        }
        catch ( Exception e )
        {
            showError( "Problem executing command line.", e, true );
        }

        return 0;
    }

    protected int showFatalError( String message,
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

    protected void showError( String message,
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
    private void showVersion()
    {
        InputStream resourceAsStream;
        try
        {
            Properties properties = new Properties();

            resourceAsStream = AbstractCli.class.getClassLoader().getResourceAsStream( getPomPropertiesPath() );

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

    private Properties getExecutionProperties( CommandLine commandLine )
    {
        Properties executionProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( SET_SYSTEM_PROPERTY ) )
        {
            String[] defStrs = commandLine.getOptionValues( SET_SYSTEM_PROPERTY );

            for ( int i = 0; i < defStrs.length; ++i )
            {
                setCliProperty( defStrs[i], executionProperties );
            }
        }

        executionProperties.putAll( System.getProperties() );

        return executionProperties;
    }

    private void setCliProperty( String property,
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

    private Options options;

    public Options buildOptions()
    {
        options = new Options();

        return buildOptions( options );
    }

    public CommandLine parse( String[] args )
        throws ParseException
    {
        // We need to eat any quotes surrounding arguments...
        String[] cleanArgs = cleanArgs( args );

        CommandLineParser parser = new GnuParser();

        return parser.parse( buildOptions(), cleanArgs );
    }

    private static String[] cleanArgs( String[] args )
    {
        List cleaned = new ArrayList();

        StringBuffer currentArg = null;

        for ( int i = 0; i < args.length; i++ )
        {
            String arg = args[i];

            boolean addedToBuffer = false;

            if ( arg.startsWith( "\"" ) )
            {
                // if we're in the process of building up another arg, push it and start over.
                // this is for the case: "-Dfoo=bar "-Dfoo2=bar two" (note the first unterminated quote)
                if ( currentArg != null )
                {
                    cleaned.add( currentArg.toString() );
                }

                // start building an argument here.
                currentArg = new StringBuffer( arg.substring( 1 ) );

                addedToBuffer = true;
            }

            // this has to be a separate "if" statement, to capture the case of: "-Dfoo=bar"
            if ( arg.endsWith( "\"" ) )
            {
                String cleanArgPart = arg.substring( 0, arg.length() - 1 );

                // if we're building an argument, keep doing so.
                if ( currentArg != null )
                {
                    // if this is the case of "-Dfoo=bar", then we need to adjust the buffer.
                    if ( addedToBuffer )
                    {
                        currentArg.setLength( currentArg.length() - 1 );
                    }
                    // otherwise, we trim the trailing " and append to the buffer.
                    else
                    {
                        // TODO: introducing a space here...not sure what else to do but collapse whitespace
                        currentArg.append( ' ' ).append( cleanArgPart );
                    }

                    // we're done with this argument, so add it.
                    cleaned.add( currentArg.toString() );
                }
                else
                {
                    // this is a simple argument...just add it.
                    cleaned.add( cleanArgPart );
                }

                // the currentArg MUST be finished when this completes.
                currentArg = null;

                continue;
            }

            // if we haven't added this arg to the buffer, and we ARE building an argument
            // buffer, then append it with a preceding space...again, not sure what else to
            // do other than collapse whitespace.
            // NOTE: The case of a trailing quote is handled by nullifying the arg buffer.
            if ( !addedToBuffer )
            {
                // append to the argument we're building, collapsing whitespace to a single space.
                if ( currentArg != null )
                {
                    currentArg.append( ' ' ).append( arg );
                }
                // this is a loner, just add it directly.
                else
                {
                    cleaned.add( arg );
                }
            }
        }

        // clean up.
        if ( currentArg != null )
        {
            cleaned.add( currentArg.toString() );
        }

        int cleanedSz = cleaned.size();
        String[] cleanArgs = null;

        if ( cleanedSz == 0 )
        {
            // if we didn't have any arguments to clean, simply pass the original array through
            cleanArgs = args;
        }
        else
        {
            cleanArgs = (String[]) cleaned.toArray( new String[cleanedSz] );
        }

        return cleanArgs;
    }

    public void displayHelp()
    {
        System.out.println();

        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp( "mvn [options] [<goal(s)>] [<phase(s)>]", "\nOptions:", options, "\n" );
    }
}
