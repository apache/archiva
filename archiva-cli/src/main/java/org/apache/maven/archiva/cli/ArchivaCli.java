package org.apache.maven.archiva.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.maven.archiva.Archiva;
import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.tools.cli.AbstractCli;
import org.codehaus.plexus.tools.cli.Cli;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Jason van Zyl
 */
public class ArchivaCli
    extends AbstractCli
{
    public static final char CONVERT = 'c';

    public static final String SOURCE_REPO_PATH = "sourceRepositoryPath";

    public static final String TARGET_REPO_PATH = "targetRepositoryPath";

    public static final String BLACKLISTED_PATTERNS = "blacklistPatterns";

    // ----------------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------------

    public static void main( String[] args )
        throws Exception
    {
        new ArchivaCli().execute( args );
    }

    public String getPomPropertiesPath()
    {
        return "META-INF/maven/org.apache.maven.archiva/archiva-cli/pom.properties";
    }

    public Options buildCliOptions( Options options )
    {
        options.addOption( OptionBuilder.withLongOpt( "convert" ).hasArg().withDescription(
            "Convert a legacy Maven 1.x repository to a Maven 2.x repository using a properties file to describe the conversion." )
            .create( CONVERT ) );

        return options;
    }

    public void invokePlexusComponent( CommandLine cli,
                                       PlexusContainer plexus )
        throws Exception
    {
        Archiva archiva = (Archiva) plexus.lookup( Archiva.ROLE );

        if ( cli.hasOption( CONVERT ) )
        {
            Properties p = new Properties();

            try
            {
                p.load( new FileInputStream( cli.getOptionValue( CONVERT ) ) );
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
}
