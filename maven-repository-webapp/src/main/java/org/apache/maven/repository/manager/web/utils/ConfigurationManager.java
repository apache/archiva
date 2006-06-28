package org.apache.maven.repository.manager.web.utils;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.io.xpp3.ConfigurationXpp3Reader;
import org.apache.maven.repository.configuration.io.xpp3.ConfigurationXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * This class updates/sets the configuration values in the mrm-admin-config.xml file used
 * for discovery and indexing.
 *
 * @plexus.component role="org.apache.maven.repository.manager.web.utils.ConfigurationManager"
 */
public class ConfigurationManager
{
    public static final String WEB_XML_FILE = "web.xml";

    public static final String INDEX_CONFIG_FILE = "mrm-admin-config.xml";

    public static final String CONFIGURATION = "configuration";

    public static final String DISCOVER_SNAPSHOTS = "discoverSnapshots";

    public static final String DISCOVERY_CRON_EXPRESSION = "discoveryCronExpression";

    public static final String INDEXPATH = "indexPath";

    public static final String MIN_INDEXPATH = "minimalIndexPath";

    public static final String REPOSITORY_LAYOUT = "repositoryLayout";

    public static final String REPOSITORY_DIRECTORY = "repositoryDirectory";

    public static final String DISCOVERY_BLACKLIST_PATTERNS = "discoveryBlacklistPatterns";

    private Configuration config;

    private File plexusDescriptor;

    /**
     * Method for updating the configuration in mrm-admin-config.xml
     *
     * @param map contains the fields and the values to be updated in the configuration
     */
    public void updateConfiguration( Map map )
        throws IOException
    {
        File file = getConfigFile();

        try
        {
            config = readXmlDocument( file );
        }
        catch ( XmlPullParserException de )
        {
            de.printStackTrace();
        }

        for ( Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            if ( name.equals( DISCOVERY_CRON_EXPRESSION ) )
            {
                config.setDiscoveryCronExpression( value );
            }
            if ( name.equals( REPOSITORY_LAYOUT ) )
            {
                config.setRepositoryLayout( value );
            }
            if ( name.equals( DISCOVER_SNAPSHOTS ) )
            {
                config.setDiscoverSnapshots( Boolean.valueOf( value ).booleanValue() );
            }
            if ( name.equals( REPOSITORY_DIRECTORY ) )
            {
                config.setRepositoryDirectory( value );
            }
            if ( name.equals( INDEXPATH ) )
            {
                config.setIndexPath( value );
            }
            if ( name.equals( MIN_INDEXPATH ) )
            {
                config.setMinimalIndexPath( value );
            }
            if ( name.equals( DISCOVERY_BLACKLIST_PATTERNS ) )
            {
                config.setDiscoveryBlackListPatterns( value );
            }
        }

        writeXmlDocument( getConfigFile() );
    }

    /**
     * Method that gets the properties set in the mrm-admin-config.xml for the configuration fields
     * used in the schedule, indexing and discovery
     *
     * @return a Map that contains the elements in the properties of the configuration object
     */
    public Configuration getConfiguration()
        throws IOException
    {
        File file = getConfigFile();
        config = new Configuration();

        if ( !file.exists() )
        {
            writeXmlDocument( getConfigFile() );
        }
        else
        {
            try
            {
                config = readXmlDocument( file );
            }
            catch ( XmlPullParserException xe )
            {
                // TODO: fix error handling!
                xe.printStackTrace();
            }
        }

        return config;
    }

    /**
     * Method that reads the xml file and returns a Configuration object
     *
     * @param file the xml file to be read
     * @return a Document object that represents the contents of the xml file
     * @throws FileNotFoundException
     * @throws IOException
     * @throws XmlPullParserException
     */
    protected Configuration readXmlDocument( File file )
        throws IOException, XmlPullParserException
    {
        ConfigurationXpp3Reader configReader = new ConfigurationXpp3Reader();
        Reader reader = new FileReader( file );
        Configuration config = configReader.read( reader );

        return config;
    }

    /**
     * Method for writing the configuration into the xml file
     *
     * @param file the file where the document will be written to
     */
    protected void writeXmlDocument( File file )
        throws IOException
    {
        Writer writer = new FileWriter( file );
        ConfigurationXpp3Writer configWriter = new ConfigurationXpp3Writer();
        configWriter.write( writer, config );
    }

    /**
     * Method that returns the mrm-admin-config.xml file
     *
     * @return a File that references the plexus.xml
     */
    protected File getConfigFile()
    {
        URL indexConfigXml = getClass().getClassLoader().getResource( "../" + INDEX_CONFIG_FILE );

        if ( indexConfigXml != null )
        {
            plexusDescriptor = new File( indexConfigXml.getFile() );
        }
        else
        {
            URL xmlPath = getClass().getClassLoader().getResource( "../" + WEB_XML_FILE );
            String path = xmlPath.getFile();
            int lastIndex = path.lastIndexOf( '/' );
            path = path.substring( 0, lastIndex + 1 );
            path = path + INDEX_CONFIG_FILE;
            plexusDescriptor = new File( path );
        }

        return plexusDescriptor;
    }

}
