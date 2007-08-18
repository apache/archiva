package org.apache.maven.archiva.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.converter.legacy.LegacyRepositoryConverter;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.scanner.RepositoryScanner;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.tools.cli.AbstractCli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ArchivaCli 
 *
 * @author Jason van Zyl
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaCli
    extends AbstractCli
{
    // ----------------------------------------------------------------------------
    // Options
    // ----------------------------------------------------------------------------

    public static final char CONVERT = 'c';

    public static final char SCAN = 's';

    public static final char CONSUMERS = 'u';

    public static final char LIST_CONSUMERS = 'l';

    public static final char DUMP_CONFIGURATION = 'd';

    // ----------------------------------------------------------------------------
    // Properties controlling Repository conversion
    // ----------------------------------------------------------------------------

    public static final String SOURCE_REPO_PATH = "sourceRepositoryPath";

    public static final String TARGET_REPO_PATH = "targetRepositoryPath";

    public static final String BLACKLISTED_PATTERNS = "blacklistPatterns";

    /**
     * Configuration store.
     *
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    public static void main( String[] args )
        throws Exception
    {
        new ArchivaCli().execute( args );
    }

    public String getPomPropertiesPath()
    {
        return "META-INF/maven/org.apache.maven.archiva/archiva-cli/pom.properties";
    }

    private Option createOption( char shortOpt, String longOpt, int argCount, String description )
    {
        boolean hasArg = ( argCount > 0 );
        Option opt = new Option( String.valueOf( shortOpt ), hasArg, description );
        opt.setLongOpt( longOpt );
        if ( hasArg )
        {
            opt.setArgs( argCount );
        }
        return opt;
    }

    public Options buildCliOptions( Options options )
    {
        Option convertOption = createOption( CONVERT, "convert", 1, "Convert a legacy Maven 1.x repository to a "
            + "Maven 2.x repository using a properties file to describe the conversion." );
        convertOption.setArgName( "conversion.properties" );
        options.addOption( convertOption );

        Option scanOption = createOption( SCAN, "scan", 1, "Scan the specified repository." );
        scanOption.setArgName( "repository directory" );
        options.addOption( scanOption );

        Option consumerOption = createOption( CONSUMERS, "consumers", 1, "The consumers to use. "
            + "(comma delimited. default: 'count-artifacts')" );
        consumerOption.setArgName( "consumer list" );
        options.addOption( consumerOption );

        Option listConsumersOption = createOption( LIST_CONSUMERS, "listconsumers", 0, "List available consumers." );
        options.addOption( listConsumersOption );

        Option dumpConfig = createOption( DUMP_CONFIGURATION, "dumpconfig", 0, "Dump Current Configuration." );
        options.addOption( dumpConfig );

        return options;
    }

    public void invokePlexusComponent( CommandLine cli, PlexusContainer plexus )
        throws Exception
    {
        if ( cli.hasOption( CONVERT ) )
        {
            doConversion( cli, plexus );
        }
        else if ( cli.hasOption( SCAN ) )
        {
            doScan( cli, plexus );
        }
        else if ( cli.hasOption( LIST_CONSUMERS ) )
        {
            dumpAvailableConsumers( plexus );
        }
        else if ( cli.hasOption( DUMP_CONFIGURATION ) )
        {
            dumpConfiguration( plexus );
        }
        else
        {
            displayHelp();
        }
    }

    private void doScan( CommandLine cli, PlexusContainer plexus )
        throws ConsumerException, ComponentLookupException
    {
        String path = cli.getOptionValue( SCAN );

        ArchivaRepository repo = new ArchivaRepository( "cliRepo", "Archiva CLI Provided Repo", "file://" + path );

        List knownConsumerList = new ArrayList();

        knownConsumerList.addAll( getConsumerList( cli, plexus ) );

        List invalidConsumerList = Collections.EMPTY_LIST;

        List ignoredContent = new ArrayList();
        ignoredContent.addAll( Arrays.asList( RepositoryScanner.IGNORABLE_CONTENT ) );

        RepositoryScanner scanner = (RepositoryScanner) plexus.lookup( RepositoryScanner.class );

        try
        {
            RepositoryContentStatistics stats = scanner.scan( repo, knownConsumerList, invalidConsumerList,
                                                              ignoredContent, RepositoryScanner.FRESH_SCAN );

            System.out.println( "\n" + stats.toDump( repo ) );
        }
        catch ( RepositoryException e )
        {
            e.printStackTrace( System.err );
        }
    }

    private Collection getConsumerList( CommandLine cli, PlexusContainer plexus )
        throws ComponentLookupException, ConsumerException
    {
        String specifiedConsumers = "count-artifacts";

        if ( cli.hasOption( CONSUMERS ) )
        {
            specifiedConsumers = cli.getOptionValue( CONSUMERS );
        }

        List consumerList = new ArrayList();

        Map availableConsumers = plexus.lookupMap( RepositoryContentConsumer.class );

        String consumerArray[] = StringUtils.split( specifiedConsumers, ',' );

        for ( int i = 0; i < consumerArray.length; i++ )
        {
            String specifiedConsumer = consumerArray[i];
            if ( !availableConsumers.containsKey( specifiedConsumer ) )
            {
                System.err.println( "Specified consumer [" + specifiedConsumer + "] not found." );
                dumpAvailableConsumers( plexus );
                System.exit( 1 );
            }

            consumerList.add( availableConsumers.get( specifiedConsumer ) );
        }

        return consumerList;
    }

    private void dumpAvailableConsumers( PlexusContainer plexus )
        throws ComponentLookupException
    {
        Map availableConsumers = plexus.lookupMap( RepositoryContentConsumer.class );

        System.out.println( ".\\ Available Consumer List \\.______________________________" );

        for ( Iterator iter = availableConsumers.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            String consumerHint = (String) entry.getKey();
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) entry.getValue();
            System.out.println( "  " + consumerHint + ": " + consumer.getDescription() + " ("
                + consumer.getClass().getName() + ")" );
        }
    }

    private void doConversion( CommandLine cli, PlexusContainer plexus )
        throws ComponentLookupException
    {
        LegacyRepositoryConverter legacyRepositoryConverter = (LegacyRepositoryConverter) plexus
            .lookup( LegacyRepositoryConverter.ROLE );

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

        List fileExclusionPatterns = null;

        String s = p.getProperty( BLACKLISTED_PATTERNS );

        if ( s != null )
        {
            fileExclusionPatterns = Arrays.asList( StringUtils.split( s, "," ) );
        }

        try
        {
            legacyRepositoryConverter.convertLegacyRepository( oldRepositoryPath, newRepositoryPath,
                                                               fileExclusionPatterns );
        }
        catch ( RepositoryConversionException e )
        {
            showFatalError( "Error converting repository.", e, true );
        }
    }

    private void dumpConfiguration( PlexusContainer plexus )
        throws ComponentLookupException
    {
        archivaConfiguration = (ArchivaConfiguration) plexus.lookup( ArchivaConfiguration.ROLE, "cli" );

        System.out.println( "File Type Count: "
            + archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().size() );
    }
}
