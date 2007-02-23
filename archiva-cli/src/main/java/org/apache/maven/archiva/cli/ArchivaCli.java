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
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.converter.legacy.LegacyRepositoryConverter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.tools.cli.AbstractCli;

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
    // ----------------------------------------------------------------------------
    // Options
    // ----------------------------------------------------------------------------

    public static final char CONVERT = 'c';

    // ----------------------------------------------------------------------------
    // Properties controlling Repository conversion
    // ----------------------------------------------------------------------------

    public static final String SOURCE_REPO_PATH = "sourceRepositoryPath";

    public static final String TARGET_REPO_PATH = "targetRepositoryPath";

    public static final String BLACKLISTED_PATTERNS = "blacklistPatterns";

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

    public void invokePlexusComponent( CommandLine cli, PlexusContainer plexus )
        throws Exception
    {
        LegacyRepositoryConverter legacyRepositoryConverter =
            (LegacyRepositoryConverter) plexus.lookup( LegacyRepositoryConverter.ROLE );

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

            List fileExclusionPatterns = null;

            String s = p.getProperty( BLACKLISTED_PATTERNS );

            if ( s != null )
            {
                fileExclusionPatterns = Arrays.asList( StringUtils.split( s, "," ) );
            }

            try
            {
                legacyRepositoryConverter.convertLegacyRepository( oldRepositoryPath, newRepositoryPath,
                                                                   fileExclusionPatterns,
                                                                   true );
            }
            catch ( RepositoryConversionException e )
            {
                showFatalError( "Error converting repository.", e, true );
            }
        }
    }
}
