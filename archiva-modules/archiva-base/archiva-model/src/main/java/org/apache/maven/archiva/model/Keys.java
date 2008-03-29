package org.apache.maven.archiva.model;

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

import org.apache.commons.lang.StringUtils;

/**
 * Keys - utility methods for converting common objects into string keys. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class Keys
{
    public static String toKey( ArchivaProjectModel model )
    {
        return toKey( model.getGroupId(), model.getArtifactId(), model.getVersion() );
    }
    
    public static String toKey( String groupId, String artifactId, String version, String classifier, String type )
    {
        StringBuffer key = new StringBuffer();

        key.append( groupId ).append( ":" );
        key.append( artifactId ).append( ":" );
        key.append( version ).append( ":" );
        key.append( StringUtils.defaultString( classifier ) ).append( ":" );
        key.append( type );

        return key.toString();
    }

    public static String toKey( ArtifactReference ref )
    {
        return toKey( ref.getGroupId(), ref.getArtifactId(), ref.getVersion(), ref.getClassifier(), ref.getType() );
    }

    public static String toKey( ProjectReference ref )
    {
        StringBuffer key = new StringBuffer();

        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() );

        return key.toString();
    }

    public static String toKey( String groupId, String artifactId, String version )
    {
        StringBuffer key = new StringBuffer();

        key.append( groupId ).append( ":" );
        key.append( artifactId ).append( ":" );
        key.append( version );

        return key.toString();
    }
    
    public static String toKey( VersionedReference ref )
    {
        return toKey( ref.getGroupId(), ref.getArtifactId(), ref.getVersion() );
    }
}
