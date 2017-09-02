package org.apache.archiva.repository.metadata;

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

import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.Plugin;
import org.apache.archiva.xml.XMLException;
import org.apache.archiva.xml.XMLWriter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * RepositoryMetadataWriter
 */
public class RepositoryMetadataWriter
{
    public static void write( ArchivaRepositoryMetadata metadata, Path outputFile )
        throws RepositoryMetadataException
    {
        boolean thrown = false;
        try (FileWriter writer = new FileWriter( outputFile.toFile() ))
        {
            write( metadata, writer );
            writer.flush();
        }
        catch ( IOException e )
        {
            thrown = true;
            throw new RepositoryMetadataException(
                "Unable to write metadata file: " + outputFile.toAbsolutePath() + " - " + e.getMessage(), e );
        }
        finally
        {
            if ( thrown )
            {
                FileUtils.deleteQuietly( outputFile );
            }
        }
    }

    public static void write( ArchivaRepositoryMetadata metadata, Writer writer )
        throws RepositoryMetadataException
    {
        Document doc = DocumentHelper.createDocument();

        Element root = DocumentHelper.createElement( "metadata" );
        doc.setRootElement( root );

        addOptionalElementText( root, "groupId", metadata.getGroupId() );
        addOptionalElementText( root, "artifactId", metadata.getArtifactId() );
        addOptionalElementText( root, "version", metadata.getVersion() );

        if ( CollectionUtils.isNotEmpty( metadata.getPlugins() ) )
        {
            Element plugins = root.addElement( "plugins" );

            List<Plugin> pluginList = metadata.getPlugins();
            Collections.sort( pluginList, PluginComparator.INSTANCE );

            for ( Plugin plugin : metadata.getPlugins() )
            {
                Element p = plugins.addElement( "plugin" );
                p.addElement( "prefix" ).setText( plugin.getPrefix() );
                p.addElement( "artifactId" ).setText( plugin.getArtifactId() );
                addOptionalElementText( p, "name", plugin.getName() );
            }
        }

        if ( CollectionUtils.isNotEmpty( metadata.getAvailableVersions() ) //
            || StringUtils.isNotBlank( metadata.getReleasedVersion() ) //
            || StringUtils.isNotBlank( metadata.getLatestVersion() ) //
            || StringUtils.isNotBlank( metadata.getLastUpdated() ) //
            || ( metadata.getSnapshotVersion() != null ) )
        {
            Element versioning = root.addElement( "versioning" );

            addOptionalElementText( versioning, "latest", metadata.getLatestVersion() );
            addOptionalElementText( versioning, "release", metadata.getReleasedVersion() );

            if ( metadata.getSnapshotVersion() != null )
            {
                Element snapshot = versioning.addElement( "snapshot" );
                String bnum = String.valueOf( metadata.getSnapshotVersion().getBuildNumber() );
                addOptionalElementText( snapshot, "buildNumber", bnum );
                addOptionalElementText( snapshot, "timestamp", metadata.getSnapshotVersion().getTimestamp() );
            }

            if ( CollectionUtils.isNotEmpty( metadata.getAvailableVersions() ) )
            {
                Element versions = versioning.addElement( "versions" );
                Iterator<String> it = metadata.getAvailableVersions().iterator();
                while ( it.hasNext() )
                {
                    String version = it.next();
                    versions.addElement( "version" ).setText( version );
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

        elem.addElement( elemName ).setText( text );
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
