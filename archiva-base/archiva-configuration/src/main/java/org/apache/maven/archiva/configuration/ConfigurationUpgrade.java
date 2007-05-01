package org.apache.maven.archiva.configuration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A component that is first in the plexus startup that ensure that the configuration
 * file format has been upgraded properly. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.configuration.ConfigurationUpgrade"
 *                   role-hint="default"
 */
public class ConfigurationUpgrade
    extends AbstractLogEnabled
    implements Initializable
{
    public static final int CURRENT_CONFIG_VERSION = 1;

    /* NOTE: This component should *NOT USE* the configuration api to do it's upgrade */

    public void initialize()
        throws InitializationException
    {
        File userConfigFile = new File( System.getProperty( "user.home" ), ".m2/archiva.xml" );

        if ( !userConfigFile.exists() )
        {
            writeDefaultConfigFile( userConfigFile );
            return;
        }

        boolean configOk = false;
        try
        {
            XMLReader xml = new XMLReader( "configuration", userConfigFile );
            String configVersion = xml.getElementText( "//configuration/version" );
            if ( StringUtils.isNotBlank( configVersion ) )
            {
                configOk = true;

                // Found an embedded configuration version.
                int version = NumberUtils.toInt( configVersion, 0 );
                if ( version < CURRENT_CONFIG_VERSION )
                {
                    upgradeVersion( userConfigFile, xml );
                }
            }
        }
        catch ( XMLException e )
        {
            getLogger().warn( "Unable to read user configuration XML: " + e.getMessage(), e );
        }

        if ( !configOk )
        {
            try
            {
                FileUtils.copyFile( userConfigFile, new File( userConfigFile.getAbsolutePath() + ".bak" ) );
                writeDefaultConfigFile( userConfigFile );
            }
            catch ( IOException e )
            {
                getLogger().warn( "Unable to create backup of your configuration file: "
                    + e.getMessage(), e );
            }
        }

    }

    private void upgradeVersion( File userConfigFile, XMLReader xml )
    {
        // TODO: write implementation when we have a current version greater than 1.
    }

    private void writeDefaultConfigFile( File userConfigFile )
    {
        URL defaultConfigURL = this.getClass()
            .getResource( "/org/apache/maven/archiva/configuration/default-archiva.xml" );

        if ( defaultConfigURL == null )
        {
            try
            {
                FileWriter writer = new FileWriter( userConfigFile );
                writer.write( "<?xml version=\"1.0\"?>\n" );
                writer.write( "<configuration />" );
                writer.flush();
                writer.close();
                return;
            }
            catch ( IOException e )
            {
                getLogger().warn( "Unable to write default (generic) configuration file: "
                    + e.getMessage(), e );
            }
        }

        // Write default to user config file location.
        try
        {
            FileOutputStream output = new FileOutputStream( userConfigFile );
            InputStream input = defaultConfigURL.openStream();
            IOUtils.copy( input, output );
            output.flush();
            input.close();
            output.close();
        }
        catch ( IOException e )
        {
            getLogger().warn( "Unable to write default configuration file: " + e.getMessage(), e );
        }
    }

}
