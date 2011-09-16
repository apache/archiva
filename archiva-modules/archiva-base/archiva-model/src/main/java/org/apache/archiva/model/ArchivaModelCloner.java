package org.apache.archiva.model;

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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Utility methods for cloning various Archiva Model objects. 
 *
 * @version $Id$
 */
public class ArchivaModelCloner
{

    public static ArtifactReference clone( ArtifactReference artifactReference )
    {
        if ( artifactReference == null )
        {
            return null;
        }

        ArtifactReference cloned = new ArtifactReference();

        cloned.setGroupId( artifactReference.getGroupId() );
        cloned.setArtifactId( artifactReference.getArtifactId() );
        cloned.setVersion( artifactReference.getVersion() );
        cloned.setClassifier( artifactReference.getClassifier() );
        cloned.setType( artifactReference.getType() );

        return cloned;
    }

    @SuppressWarnings("unchecked")
    public static Properties clone( Properties properties )
    {
        if ( properties == null )
        {
            return null;
        }

        Properties cloned = new Properties();

        Enumeration<String> keys = (Enumeration<String>) properties.propertyNames();
        while ( keys.hasMoreElements() )
        {
            String key = (String) keys.nextElement();
            String value = properties.getProperty( key );
            cloned.setProperty( key, value );
        }

        return cloned;
    }

    public static SnapshotVersion clone( SnapshotVersion snapshotVersion )
    {
        if ( snapshotVersion == null )
        {
            return null;
        }

        SnapshotVersion cloned = new SnapshotVersion();

        cloned.setTimestamp( snapshotVersion.getTimestamp() );
        cloned.setBuildNumber( snapshotVersion.getBuildNumber() );

        return cloned;
    }

    public static VersionedReference clone( VersionedReference versionedReference )
    {
        if ( versionedReference == null )
        {
            return null;
        }

        VersionedReference cloned = new VersionedReference();

        cloned.setGroupId( versionedReference.getGroupId() );
        cloned.setArtifactId( versionedReference.getArtifactId() );
        cloned.setVersion( versionedReference.getVersion() );

        return cloned;
    }

    public static List<ArtifactReference> cloneArtifactReferences( List<ArtifactReference> artifactReferenceList )
    {
        if ( artifactReferenceList == null )
        {
            return null;
        }

        List<ArtifactReference> ret = new ArrayList<ArtifactReference>();

        for ( ArtifactReference ref : artifactReferenceList )
        {
            ret.add( clone( ref ) );
        }

        return ret;
    }

    private static List<String> cloneSimpleStringList( List<String> simple )
    {
        if ( simple == null )
        {
            return null;
        }

        List<String> ret = new ArrayList<String>();

        for ( String txt : simple )
        {
            ret.add( txt );
        }

        return ret;
    }

    public static List<String> cloneAvailableVersions( List<String> availableVersions )
    {
        return cloneSimpleStringList( availableVersions );
    }
}
