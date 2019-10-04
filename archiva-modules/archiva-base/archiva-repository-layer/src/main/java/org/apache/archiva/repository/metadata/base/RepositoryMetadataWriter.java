package org.apache.archiva.repository.metadata.base;

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

import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.Plugin;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.xml.XMLException;
import org.apache.archiva.xml.XMLWriter;
import org.apache.archiva.xml.XmlUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * RepositoryMetadataWriter
 */
public class RepositoryMetadataWriter
{
    private static final Logger log = LoggerFactory.getLogger(RepositoryMetadataWriter.class);

    public static void write( ArchivaRepositoryMetadata metadata, StorageAsset outputFile )
        throws RepositoryMetadataException
    {
        boolean thrown = false;
        try (OutputStreamWriter writer = new OutputStreamWriter( outputFile.getWriteStream(true)))
        {
            write( metadata, writer );
            writer.flush();
        }
        catch ( IOException e )
        {
            thrown = true;
            throw new RepositoryMetadataException(
                "Unable to write metadata file: " + outputFile.getPath() + " - " + e.getMessage(), e );
        }
        finally
        {
            if ( thrown )
            {
                try {
                    outputFile.getStorage().removeAsset(outputFile);
                } catch (IOException e) {
                    log.error("Could not remove asset {}", outputFile);
                }
            }
        }
    }

    public static void write( ArchivaRepositoryMetadata metadata, Writer writer )
        throws RepositoryMetadataException
    {
        Document doc = null;
        try {
            doc = XmlUtil.createDocument();
        } catch (ParserConfigurationException e) {
            throw new RepositoryMetadataException("Could not create xml doc " + e.getMessage(), e);
        }

        Element root = doc.createElement( "metadata" );
        doc.appendChild(root);

        addOptionalElementText( root, "groupId", metadata.getGroupId() );
        addOptionalElementText( root, "artifactId", metadata.getArtifactId() );
        addOptionalElementText( root, "version", metadata.getVersion() );

        if ( CollectionUtils.isNotEmpty( metadata.getPlugins() ) )
        {
            Element plugins = XmlUtil.addChild(root, "plugins" );

            List<Plugin> pluginList = metadata.getPlugins();
            Collections.sort( pluginList, PluginComparator.INSTANCE );

            for ( Plugin plugin : metadata.getPlugins() )
            {
                Element p = XmlUtil.addChild(plugins, "plugin" );
                XmlUtil.addChild(doc, p, "prefix" ).setTextContent( plugin.getPrefix() );
                XmlUtil.addChild(doc, p, "artifactId" ).setTextContent( plugin.getArtifactId() );
                addOptionalElementText( p, "name", plugin.getName() );
            }
        }

        if ( CollectionUtils.isNotEmpty( metadata.getAvailableVersions() ) //
            || StringUtils.isNotBlank( metadata.getReleasedVersion() ) //
            || StringUtils.isNotBlank( metadata.getLatestVersion() ) //
            || StringUtils.isNotBlank( metadata.getLastUpdated() ) //
            || ( metadata.getSnapshotVersion() != null ) )
        {
            Element versioning = XmlUtil.addChild(root, "versioning" );

            addOptionalElementText( versioning, "latest", metadata.getLatestVersion() );
            addOptionalElementText( versioning, "release", metadata.getReleasedVersion() );

            if ( metadata.getSnapshotVersion() != null )
            {
                Element snapshot = XmlUtil.addChild(versioning, "snapshot" );
                String bnum = String.valueOf( metadata.getSnapshotVersion().getBuildNumber() );
                addOptionalElementText( snapshot, "buildNumber", bnum );
                addOptionalElementText( snapshot, "timestamp", metadata.getSnapshotVersion().getTimestamp() );
            }

            if ( CollectionUtils.isNotEmpty( metadata.getAvailableVersions() ) )
            {
                Element versions = XmlUtil.addChild(versioning, "versions" );
                Iterator<String> it = metadata.getAvailableVersions().iterator();
                while ( it.hasNext() )
                {
                    String version = it.next();
                    XmlUtil.addChild(versions, "version" ).setTextContent( version );
                }
            }

            addOptionalElementText( versioning, "lastUpdated", metadata.getLastUpdated() );
        }

        try
        {
            XMLWriter.write( doc, writer );
        }
        catch ( XMLException e )
        {
            throw new RepositoryMetadataException( "Unable to write xml contents to writer: " + e.getMessage(), e );
        }
    }

    private static void addOptionalElementText( Element elem, String elemName, String text )
    {
        if ( StringUtils.isBlank( text ) )
        {
            return;
        }

        XmlUtil.addChild(elem, elemName ).setTextContent( text );
    }

    private static class PluginComparator
        implements Comparator<Plugin>
    {
        private static final PluginComparator INSTANCE = new PluginComparator();

        @Override
        public int compare( Plugin plugin, Plugin plugin2 )
        {
            if ( plugin.getPrefix() != null && plugin2.getPrefix() != null )
            {
                return plugin.getPrefix().compareTo( plugin2.getPrefix() );
            }
            if ( plugin.getName() != null && plugin2.getName() != null )
            {
                return plugin.getName().compareTo( plugin2.getName() );
            }
            // we assume artifactId is not null which sounds good :-)
            return plugin.getArtifactId().compareTo( plugin2.getArtifactId() );
        }
    }
}
