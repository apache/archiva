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

import org.apache.archiva.model.ArchivaModelCloner;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.Plugin;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RepositoryMetadataMerge 
 *
 *
 */
public class RepositoryMetadataMerge
{
    public static ArchivaRepositoryMetadata merge( final ArchivaRepositoryMetadata mainMetadata,
                                                   final ArchivaRepositoryMetadata sourceMetadata )
        throws RepositoryMetadataException
    {
        if ( mainMetadata == null )
        {
            throw new RepositoryMetadataException( "Cannot merge a null main project." );
        }

        if ( sourceMetadata == null )
        {
            throw new RepositoryMetadataException( "Cannot copy to a null parent project." );
        }

        ArchivaRepositoryMetadata merged = new ArchivaRepositoryMetadata();

        merged.setGroupId( merge( mainMetadata.getGroupId(), sourceMetadata.getGroupId() ) );
        merged.setArtifactId(  merge(mainMetadata.getArtifactId(), sourceMetadata.getArtifactId()));
        merged.setVersion( merge(mainMetadata.getVersion(), sourceMetadata.getVersion()) );
        merged.setReleasedVersion( merge( mainMetadata.getReleasedVersion(), sourceMetadata.getReleasedVersion() ) );
        merged.setSnapshotVersion( merge( mainMetadata.getSnapshotVersion(), sourceMetadata.getSnapshotVersion() ) );
        merged.setAvailableVersions( mergeAvailableVersions( mainMetadata.getAvailableVersions(), sourceMetadata.getAvailableVersions() ) );
        merged.setPlugins( mergePlugins( mainMetadata.getPlugins(), sourceMetadata.getPlugins() ) );
        
        //Don't set if merge was not possible
        long lastUpdated = mergeTimestamp( mainMetadata.getLastUpdated(), sourceMetadata.getLastUpdated());
        if (lastUpdated > -1)
        {
            merged.setLastUpdated(  Long.toString(lastUpdated) );
        }
        
        return merged;
    }

    private static boolean empty( String val )
    {
        if ( val == null )
        {
            return true;
        }

        return ( val.trim().length() <= 0 );
    }
    
    private static long mergeTimestamp(String mainTimestamp, String sourceTimestamp)
    {
        if (sourceTimestamp == null && mainTimestamp != null)
        {
            return convertTimestampToLong(mainTimestamp);
        }
        
        if (mainTimestamp == null && sourceTimestamp != null)
        {
            return convertTimestampToLong(sourceTimestamp);
        }
        
        if (sourceTimestamp == null && mainTimestamp == null)
        {
            return -1;
        }
        
        return mergeTimestamp(convertTimestampToLong(mainTimestamp), convertTimestampToLong(sourceTimestamp));
    }
    
    private static long mergeTimestamp(long mainTimestamp, long sourceTimestamp)
    { 
        return Math.max( mainTimestamp, sourceTimestamp );
    }

    private static SnapshotVersion merge( SnapshotVersion mainSnapshotVersion, SnapshotVersion sourceSnapshotVersion )
    {
        if ( sourceSnapshotVersion == null )
        {
            return mainSnapshotVersion;
        }

        if ( mainSnapshotVersion == null )
        {
            return ArchivaModelCloner.clone( sourceSnapshotVersion );
        }

        SnapshotVersion merged = new SnapshotVersion();
       
        long mainSnapshotLastUpdated = convertTimestampToLong(mainSnapshotVersion.getTimestamp());
        long sourceSnapshotLastUpdated = convertTimestampToLong(sourceSnapshotVersion.getTimestamp());
                        
        long lastUpdated = mergeTimestamp(mainSnapshotLastUpdated, sourceSnapshotLastUpdated);
        
        if (lastUpdated == mainSnapshotLastUpdated)
        {
            merged.setTimestamp(mainSnapshotVersion.getTimestamp());
            merged.setBuildNumber(mainSnapshotVersion.getBuildNumber());
        }
        else
        {
            merged.setTimestamp(sourceSnapshotVersion.getTimestamp());
            merged.setBuildNumber(sourceSnapshotVersion.getBuildNumber());
        }

        return merged;
    }
    
    private static long convertTimestampToLong(String timestamp)
    {
        if (timestamp == null)
        {
            return -1;
        }
        
        return getLongFromTimestampSafely(StringUtils.replace(timestamp, ".", ""));
    }
    
    private static long getLongFromTimestampSafely( String timestampString )
    {
        try
        {
            return Long.parseLong(timestampString);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    private static String merge( String main, String source )
    {
        if ( empty( main ) && !empty( source ) )
        {
            return source;
        }

        return main;
    }
    
    private static List<Plugin> mergePlugins(List<Plugin> mainPlugins, List<Plugin> sourcePlugins)
    {
        if ( sourcePlugins == null )
        {
            return mainPlugins;
        }
        
        if ( mainPlugins == null )
        {
            return clonePlugins( sourcePlugins );
        }
        
        List<Plugin> merged = clonePlugins( mainPlugins );
        
        for ( Plugin plugin : sourcePlugins )
        {
            if ( !merged.contains( plugin ) )
            {
                merged.add( plugin );
            }
        }

        return merged;
    }
    
    /**
     * Clones a list of plugins.
     * 
     * This method exists because ArchivaModelCloner.clonePlugins() 
     * only works with artifact references.
     * 
     * @param plugins
     * @return list of cloned plugins
     */
    private static List<Plugin> clonePlugins(List<Plugin> plugins)
    {
        if (plugins == null)
        {
            return null;
        }
        
        List<Plugin> result = new ArrayList<>();
        
        for (Plugin plugin : plugins)
        {
            Plugin clonedPlugin = new Plugin();
            clonedPlugin.setArtifactId(plugin.getArtifactId());
            clonedPlugin.setName(plugin.getName());
            clonedPlugin.setPrefix(plugin.getPrefix());
            result.add(plugin);
        }
        
        return result;
    }

    private static List<String> mergeAvailableVersions( List<String> mainAvailableVersions, List<String> sourceAvailableVersions )
    {
        if ( sourceAvailableVersions == null )
        {
            return mainAvailableVersions;
        }

        if ( mainAvailableVersions == null )
        {
            return ArchivaModelCloner.cloneAvailableVersions( sourceAvailableVersions );
        }

        List<String> merged = ArchivaModelCloner.cloneAvailableVersions( mainAvailableVersions );

        for ( String sourceVersion : sourceAvailableVersions )
        {
            if ( !merged.contains( sourceVersion ) )
            {
                merged.add( sourceVersion );
            }
        }

        return merged;
    }
}
