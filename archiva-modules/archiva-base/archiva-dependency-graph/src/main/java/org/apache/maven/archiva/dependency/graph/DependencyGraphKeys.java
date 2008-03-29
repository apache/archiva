package org.apache.maven.archiva.dependency.graph;

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
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Exclusion;

/**
 * Key generation for the various objects used within the DependencyGraph. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyGraphKeys
{
    public static String toManagementKey( DependencyGraphNode node )
    {
        return toManagementKey( node.getArtifact() );
    }

    public static String toManagementKey( ArtifactReference ref )
    {
        StringBuffer key = new StringBuffer();
        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() );
        return key.toString();
    }

    public static String toManagementKey( Dependency ref )
    {
        StringBuffer key = new StringBuffer();
        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() );
        return key.toString();
    }

    public static String toManagementKey( Exclusion ref )
    {
        StringBuffer key = new StringBuffer();
        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() );
        return key.toString();
    }

    public static String toKey( DependencyGraphNode node )
    {
        return toKey( node.getArtifact() );
    }

    public static String toKey( ArtifactReference ref )
    {
        StringBuffer key = new StringBuffer();
        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() ).append( ":" );
        key.append( ref.getVersion() ).append( ":" );
        key.append( StringUtils.defaultString( ref.getClassifier() ) ).append( ":" );
        key.append( ref.getType() );
        return key.toString();
    }
}
