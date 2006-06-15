package org.apache.maven.repository.manager.web.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.io.xpp3.ConfigurationXpp3Writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * This class updates the configuration in plexus.xml
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

    private Document document;

    /**
     * Method for updating the configuration in plexus.xml
     *
     * @param map   contains the fields and the values to be updated in the configuration
     */
    public void updateConfiguration( Map map )
        throws IOException
    {
        File file = getConfigFile();

        try
        {
            document = readXmlDocument( file );
        }
        catch ( DocumentException de )
        {
            de.printStackTrace();
        }

        Element rootElement = document.getRootElement();
        for( Iterator iter2 = rootElement.elementIterator(); iter2.hasNext(); )
        {
            Element field = (Element) iter2.next();
            if( !map.containsKey( field.getName() ) )
            {
                map.put( field.getName(), field.getData() );
            }
        }

        for( Iterator iter = map.entrySet().iterator();iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            String name = (String) entry.getKey();
            String value = ( String ) entry.getValue();

            if( name.equals( DISCOVERY_CRON_EXPRESSION ) )
            {
                config.setDiscoveryCronExpression( value );
            }
            if( name.equals( REPOSITORY_LAYOUT ) )
            {
                config.setRepositoryLayout( value );
            }
            if( name.equals( DISCOVER_SNAPSHOTS ) )
            {
                config.setDiscoverSnapshots( Boolean.getBoolean( value ) );
            }
            if( name.equals( REPOSITORY_DIRECTORY ) )
            {
                config.setRepositoryDirectory( value );
            }
            if( name.equals( INDEXPATH ) )
            {
                config.setIndexPath( value );
            }
            if( name.equals( MIN_INDEXPATH ) )
            {
                config.setMinimalIndexPath( value );
            }
            if( name.equals( DISCOVERY_BLACKLIST_PATTERNS ) )
            {
                config.setDiscoveryBlackListPatterns( value );
            }
        }

        writeXmlDocument( getConfigFile() );
    }

    /**
     * Method that gets the properties set in the index-config.xml for the configuration fields
     * used in the schedule, indexing and discovery
     *
     * @return a Map that contains the elements in the properties of the configuration object
     */
    public Configuration getConfiguration()
        throws IOException
    {
        Map map = null;
        File file = getConfigFile();
        config = new Configuration();

        if( !file.exists() )
        {
            writeXmlDocument( getConfigFile() );
        }
        else
        {
            try
            {
                document = readXmlDocument( file );
            }
            catch ( DocumentException de )
            {
                de.printStackTrace();
            }

            map = new HashMap();
            Element rootElement = document.getRootElement();
            for( Iterator iter2 = rootElement.elementIterator(); iter2.hasNext(); )
            {
                Element field = (Element) iter2.next();
                map.put( field.getName(), field.getData() );
            }

            if( map.get( DISCOVERY_CRON_EXPRESSION ) != null && !"".equals( map.get( DISCOVERY_CRON_EXPRESSION ) ) )
            {
                 config.setDiscoveryCronExpression( ( String ) map.get( DISCOVERY_CRON_EXPRESSION ) );
            }

            if( map.get( REPOSITORY_LAYOUT ) != null && !"".equals( map.get( REPOSITORY_LAYOUT ) ) )
            {
                config.setRepositoryLayout( (String ) map.get( REPOSITORY_LAYOUT ) );
            }

            if( map.get( DISCOVER_SNAPSHOTS ) != null && !"".equals( map.get( DISCOVER_SNAPSHOTS ) ) )
            {
                config.setDiscoverSnapshots( ( ( Boolean ) map.get( DISCOVER_SNAPSHOTS ) ).booleanValue() );
            }

            if( map.get( REPOSITORY_DIRECTORY ) != null && !"".equals( map.get( REPOSITORY_DIRECTORY ) ) )
            {
                config.setRepositoryDirectory( ( String ) map.get( REPOSITORY_DIRECTORY ) );
            }

            if( map.get( INDEXPATH ) != null && !"".equals( map.get( INDEXPATH ) ) )
            {
                config.setIndexPath( ( String ) map.get( INDEXPATH ) );
            }

            if( map.get( MIN_INDEXPATH ) != null && !"".equals( map.get( MIN_INDEXPATH ) ) )
            {
                config.setMinimalIndexPath( ( String ) map.get( MIN_INDEXPATH ) );
            }

            if( map.get( DISCOVERY_BLACKLIST_PATTERNS ) != null && !"".equals( map.get( DISCOVERY_BLACKLIST_PATTERNS ) ) )
            {
                config.setDiscoveryBlackListPatterns( ( String ) map.get( DISCOVERY_BLACKLIST_PATTERNS ) );
            }
        }

        return config;
    }

    /**
     * Method that reads the xml file and puts it in a Document object
     *
     * @param file  the xml file to be read
     * @return      a Document object that represents the contents of the xml file
     * @throws DocumentException
     */
    protected Document readXmlDocument( File file )
        throws DocumentException
    {
        SAXReader reader = new SAXReader();
        if ( file.exists() )
        {
            return reader.read( file );
        }

        return null;
    }

    /**
     * Method for removing the specified element from the document
     *
     * @param element
     * @param name
     */
    protected void removeElement( Element element, String name )
    {
        for( Iterator children = element.elementIterator(); children.hasNext(); )
        {
            Element child = (Element) children.next();
            if( child.getName().equals( name ) )
            {
                element.remove( child );
            }
        }
    }

    protected Element addElement( Element element, String name )
    {
        return element.addElement( name );
    }

    protected void setElementValue( Element element, String value )
    {
        element.setText( value );
    }

    protected Element addAndSetElement( Element element, String elementName, String elementValue )
    {
        Element retElement = addElement( element, elementName );

        setElementValue( retElement, elementValue );

        return retElement;
    }

    /**
     * Method for writing the document object into its corresponding
     * xml file.
     *
     * @param file      the file where the document will be written to
     */
    protected void writeXmlDocument( File file )
        throws IOException
    {
        Writer writer = new FileWriter( file );
        ConfigurationXpp3Writer configWriter = new ConfigurationXpp3Writer();
        configWriter.write( writer, config );
    }

    /**
     * Method that returns the index-config.xml file
     *
     * @return a File that references the plexus.xml
     */
    protected File getConfigFile()
    {
        URL indexConfigXml = getClass().getClassLoader().getResource( "../" + INDEX_CONFIG_FILE );

        if( indexConfigXml != null )
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
            plexusDescriptor = new File ( path );
        }

        return plexusDescriptor;
    }

    protected Document getDocument()
    {
        return document;
    }
}
