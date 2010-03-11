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

import org.apache.archiva.metadata.model.MetadataFacet;

import java.util.HashMap;
import java.util.Map;

public class MavenProjectFacet
    implements MetadataFacet
{
    private String groupId;

    private String artifactId;

    private MavenProjectParent parent;

    private String packaging;

    public static final String FACET_ID = "org.apache.archiva.metadata.repository.storage.maven2.project";

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

    public String getName()
    {
        // TODO: not needed, perhaps version metadata facet should be separate interface?
        return null;
    }

    public Map<String, String> toProperties()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put( "groupId", groupId );
        properties.put( "artifactId", artifactId );
        properties.put( "packaging", packaging );
        if ( parent != null )
        {
            properties.put( "parent.groupId", parent.getGroupId() );
            properties.put( "parent.artifactId", parent.getArtifactId() );
            properties.put( "parent.version", parent.getVersion() );
        }
        return properties;
    }

    public void fromProperties( Map<String, String> properties )
    {
        groupId = properties.get( "groupId" );
        artifactId = properties.get( "artifactId" );
        packaging = properties.get( "packaging" );
        String parentArtifactId = properties.get( "parent.artifactId" );
        if ( parentArtifactId != null )
        {
            MavenProjectParent parent = new MavenProjectParent();
            parent.setGroupId( properties.get( "parent.groupId" ) );
            parent.setArtifactId( parentArtifactId );
            parent.setVersion( properties.get( "parent.version" ) );
            this.parent = parent;
        }
    }
}
