package org.apache.maven.archiva.cli;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Jason van Zyl
 * @version $Revision: 381114 $
 */
public class CliManager
{
    public static char CONVERT = 'c';

    public static final char OLD_REPOSITORY_PATH = 'o';

    public static final char NEW_REPOSITORY_PATH = 'n';

    // ----------------------------------------------------------------------------
    // These are standard options that we would want to use for all our projects.
    // ----------------------------------------------------------------------------

    public static final char QUIET = 'q';

    public static final char DEBUG = 'X';

    public static final char ERRORS = 'e';

    public static final char HELP = 'h';

    public static final char VERSION = 'v';

    public static final char SET_SYSTEM_PROPERTY = 'D';

    private Options options;

    public CliManager()
    {
        options = new Options();

        options.addOption( OptionBuilder.withLongOpt( "convert" ).withDescription(
            "Convert a legacy Maven 1.x repository to a Maven 2.x repository." ).create( CONVERT ) );

        options.addOption( OptionBuilder.withLongOpt( "old-repo" ).hasArg().withDescription(
            "Path to Maven 1.x legacy repository to convert." ).create( OLD_REPOSITORY_PATH ) );

        options.addOption( OptionBuilder.withLongOpt( "new-repo" ).hasArg().withDescription(
            "Path to newly created Maven 2.x repository." ).create( NEW_REPOSITORY_PATH ) );
    }

    public CommandLine parse( String[] args )
        throws ParseException
    {
        // We need to eat any quotes surrounding arguments...
        String[] cleanArgs = cleanArgs( args );

        CommandLineParser parser = new GnuParser();

        return parser.parse( options, cleanArgs );
    }

    private String[] cleanArgs( String[] args )
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
