package org.apache.archiva.metadata.repository.storage.maven2;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.archiva.metadata.model.ProjectVersionFacet;

public class MavenProjectFacet
    implements ProjectVersionFacet
{
    private String groupId;

    private String artifactId;

    private MavenProjectParent parent;

    private String packaging;

    public static final String FACET_ID = "org.apache.archiva.metadata.repository.storage.maven2";

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public MavenProjectParent getParent()
    {
        return parent;
    }

    public void setParent( MavenProjectParent parent )
    {
        this.parent = parent;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getFacetId()
    {
        return FACET_ID;
    }

    public Map<String, String> toProperties()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put( FACET_ID + ":groupId", groupId );
        properties.put( FACET_ID + ":artifactId", artifactId );
        properties.put( FACET_ID + ":packaging", packaging );
        if ( parent != null )
        {
            properties.put( FACET_ID + ":parent.groupId", parent.getGroupId() );
            properties.put( FACET_ID + ":parent.artifactId", parent.getArtifactId() );
            properties.put( FACET_ID + ":parent.version", parent.getVersion() );
        }
        return properties;
    }

    public void fromProperties( Map<String, String> properties )
    {
        groupId = properties.get( FACET_ID + ":groupId" );
        artifactId = properties.get( FACET_ID + ":artifactId" );
        packaging = properties.get( FACET_ID + ":packaging" );
        String parentArtifactId = properties.get( FACET_ID + ":parent.artifactId" );
        if ( parentArtifactId != null )
        {
            MavenProjectParent parent = new MavenProjectParent();
            parent.setGroupId( properties.get( FACET_ID + ":parent.groupId" ) );
            parent.setArtifactId( parentArtifactId );
            parent.setVersion( properties.get( FACET_ID + ":parent.version" ) );
            this.parent = parent;
        }
    }
}
