package org.apache.maven.archiva.repository.metadata;

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

import org.apache.maven.archiva.model.ArchivaModelCloner;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.SnapshotVersion;

import java.util.Iterator;
import java.util.List;

/**
 * RepositoryMetadataMerge 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
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

        merged.setReleasedVersion( merge( mainMetadata.getReleasedVersion(), sourceMetadata.getReleasedVersion() ) );
        merged.setSnapshotVersion( merge( mainMetadata.getSnapshotVersion(), sourceMetadata.getSnapshotVersion() ) );
        merged.setLastUpdated( merge( mainMetadata.getLastUpdated(), sourceMetadata.getLastUpdated() ) );
        merged.setAvailableVersions( mergeAvailableVersions( mainMetadata.getAvailableVersions(), sourceMetadata
            .getAvailableVersions() ) );

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

        merged.setTimestamp( merge( mainSnapshotVersion.getTimestamp(), sourceSnapshotVersion.getTimestamp() ) );
        merged
            .setBuildNumber( Math.max( mainSnapshotVersion.getBuildNumber(), sourceSnapshotVersion.getBuildNumber() ) );

        return merged;
    }

    private static String merge( String main, String source )
    {
        if ( empty( main ) && !empty( source ) )
        {
            return source;
        }

        return main;
    }

    private static List mergeAvailableVersions( List mainAvailableVersions, List sourceAvailableVersions )
    {
        if ( sourceAvailableVersions == null )
        {
            return mainAvailableVersions;
        }

        if ( mainAvailableVersions == null )
        {
            return ArchivaModelCloner.cloneAvailableVersions( sourceAvailableVersions );
        }

        List merged = ArchivaModelCloner.cloneAvailableVersions( mainAvailableVersions );

        Iterator it = sourceAvailableVersions.iterator();
        while ( it.hasNext() )
        {
            String sourceVersion = (String) it.next();
            if ( !merged.contains( sourceVersion ) )
            {
                merged.add( sourceVersion );
            }
        }

        return merged;
    }
}
