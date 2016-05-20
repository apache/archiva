package org.apache.archiva.cli;

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

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.RepositoryContentConsumer;
import org.apache.archiva.converter.RepositoryConversionException;
import org.apache.archiva.converter.legacy.LegacyRepositoryConverter;
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ArchivaCli
 *
 * TODO add back reading of archiva.xml from a given location
 */
public class ArchivaCli
{
    // ----------------------------------------------------------------------------
    // Properties controlling Repository conversion
    // ----------------------------------------------------------------------------

    public static final String SOURCE_REPO_PATH = "sourceRepositoryPath";

    public static final String TARGET_REPO_PATH = "targetRepositoryPath";

    public static final String BLACKLISTED_PATTERNS = "blacklistPatterns";

    public static final String POM_PROPERTIES = "/META-INF/maven/org.apache.archiva/archiva-cli/pom.properties";

    private static String getVersion()
        throws IOException
    {

        try (InputStream pomStream = ArchivaCli.class.getResourceAsStream( POM_PROPERTIES ))
        {
            if ( pomStream == null )
            {
                throw new IOException( "Failed to load " + POM_PROPERTIES );
            }
            Properties properties = new Properties();
            properties.load( pomStream );
            return properties.getProperty( "version" );
        }
    }

    private ClassPathXmlApplicationContext applicationContext;

    public ArchivaCli()
    {
        applicationContext =
            new ClassPathXmlApplicationContext( new String[]{ "classpath*:/META-INF/spring-context.xml" } );
    }

    public static void main( String[] args )
        throws Exception
    {
        Commands command = new Commands();

        try
        {
            Args.parse( command, args );
        }
        catch ( IllegalArgumentException e )
        {
            System.err.println( e.getMessage() );
            Args.usage( command );
            return;
        }

        ArchivaCli cli = new ArchivaCli();
        try
        {
            cli.execute( command );
        }
        finally
        {
            cli.destroy();
        }
    }

    private void destroy()
    {
        applicationContext.destroy();
    }

    private void execute( Commands command )
        throws Exception
    {
        if ( command.help )
        {
            Args.usage( command );
        }
        else if ( command.version )
        {
            System.out.print( "Version: " + getVersion() );
        }
        else if ( command.convert )
        {
            doConversion( command.properties );
        }
        else if ( command.scan )
        {
            if ( command.repository == null )
            {
                System.err.println( "The repository must be specified." );
                Args.usage( command );
                return;
            }

            doScan( command.repository, command.consumers.split( "," ) );
        }
        else if ( command.listConsumers )
        {
            dumpAvailableConsumers();
        }
        else
        {
            Args.usage( command );
        }
    }

    private void doScan( String path, String[] consumers )
        throws ConsumerException, MalformedURLException
    {
        ManagedRepository repo = new ManagedRepository();
        repo.setId( new File( path ).getName() );
        repo.setName( "Archiva CLI Provided Repo" );
        repo.setLocation( path );

        List<KnownRepositoryContentConsumer> knownConsumerList = new ArrayList<>();

        knownConsumerList.addAll( getConsumerList( consumers ) );

        List<InvalidRepositoryContentConsumer> invalidConsumerList = Collections.emptyList();

        List<String> ignoredContent = new ArrayList<>();
        ignoredContent.addAll( Arrays.asList( RepositoryScanner.IGNORABLE_CONTENT ) );

        RepositoryScanner scanner = applicationContext.getBean( RepositoryScanner.class );

        try
        {
            RepositoryScanStatistics stats = scanner.scan( repo, knownConsumerList, invalidConsumerList, ignoredContent,
                                                           RepositoryScanner.FRESH_SCAN );

            System.out.println( "\n" + stats.toDump( repo ) );
        }
        catch ( RepositoryScannerException e )
        {
            e.printStackTrace( System.err );
        }
    }

    private List<KnownRepositoryContentConsumer> getConsumerList( String[] consumers )
        throws ConsumerException
    {
        List<KnownRepositoryContentConsumer> consumerList = new ArrayList<>();

        Map<String, KnownRepositoryContentConsumer> availableConsumers = getConsumers();

        for ( String specifiedConsumer : consumers )
        {
            if ( !availableConsumers.containsKey( specifiedConsumer ) )
            {
                System.err.println( "Specified consumer [" + specifiedConsumer + "] not found." );
                dumpAvailableConsumers();
                System.exit( 1 );
            }

            consumerList.add( availableConsumers.get( specifiedConsumer ) );
        }

        return consumerList;
    }

    private void dumpAvailableConsumers()
    {
        Map<String, KnownRepositoryContentConsumer> availableConsumers = getConsumers();

        System.out.println( ".\\ Available Consumer List \\.______________________________" );

        for ( Map.Entry<String, KnownRepositoryContentConsumer> entry : availableConsumers.entrySet() )
        {
            String consumerHint = entry.getKey();
            RepositoryContentConsumer consumer = entry.getValue();
            System.out.println(
                "  " + consumerHint + ": " + consumer.getDescription() + " (" + consumer.getClass().getName() + ")" );
        }
    }

    @SuppressWarnings( "unchecked" )
    private Map<String, KnownRepositoryContentConsumer> getConsumers()
    {
        Map<String, KnownRepositoryContentConsumer> beans =
            applicationContext.getBeansOfType( KnownRepositoryContentConsumer.class );
        // we use a naming conventions knownRepositoryContentConsumer#hint
        // with plexus we used only hint so remove before#

        Map<String, KnownRepositoryContentConsumer> smallNames = new HashMap<>( beans.size() );

        for ( Map.Entry<String, KnownRepositoryContentConsumer> entry : beans.entrySet() )
        {
            smallNames.put( StringUtils.substringAfterLast( entry.getKey(), "#" ), entry.getValue() );
        }

        return smallNames;
    }

    private void doConversion( String properties )
        throws IOException, RepositoryConversionException
    {
        LegacyRepositoryConverter legacyRepositoryConverter = applicationContext.getBean( LegacyRepositoryConverter.class );

        Properties p = new Properties();

        try (InputStream fis = Files.newInputStream( Paths.get(properties)))
        {
            p.load( fis );
        }

        File oldRepositoryPath = new File( p.getProperty( SOURCE_REPO_PATH ) );

        File newRepositoryPath = new File( p.getProperty( TARGET_REPO_PATH ) );

        System.out.println( "Converting " + oldRepositoryPath + " to " + newRepositoryPath );

        List<String> fileExclusionPatterns = null;

        String s = p.getProperty( BLACKLISTED_PATTERNS );

        if ( s != null )
        {
            fileExclusionPatterns = Arrays.asList( StringUtils.split( s, "," ) );
        }

        legacyRepositoryConverter.convertLegacyRepository( oldRepositoryPath, newRepositoryPath,
                                                           fileExclusionPatterns );
    }

    private static class Commands
    {
        @Argument( description = "Display help information", value = "help", alias = "h" )
        private boolean help;

        @Argument( description = "Display version information", value = "version", alias = "v" )
        private boolean version;

        @Argument( description = "List available consumers", value = "listconsumers", alias = "l" )
        private boolean listConsumers;

        @Argument( description = "The consumers to use (comma delimited)", value = "consumers", alias = "u" )
        private String consumers = "count-artifacts";

        @Argument( description = "Scan the specified repository", value = "scan", alias = "s" )
        private boolean scan;

        @Argument(
            description = "Convert a legacy Maven 1.x repository to a Maven 2.x repository using a properties file to describe the conversion",
            value = "convert", alias = "c" )
        private boolean convert;

        @Argument( description = "The properties file for the conversion", value = "properties" )
        private String properties = "conversion.properties";

        @Argument( description = "The repository to scan", value = "repository" )
        private String repository;
    }
}
