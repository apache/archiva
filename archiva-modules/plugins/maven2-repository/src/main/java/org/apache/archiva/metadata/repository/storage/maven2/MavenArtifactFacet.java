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

public class MavenArtifactFacet
    implements MetadataFacet
{
    private String classifier;

    private String type;

    private String timestamp;

    private int buildNumber;

    public static final String FACET_ID = "org.apache.archiva.metadata.repository.storage.maven2.artifact";

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( String timestamp )
    {
        this.timestamp = timestamp;
    }

    public int getBuildNumber()
    {
        return buildNumber;
    }

    public void setBuildNumber( int buildNumber )
    {
        this.buildNumber = buildNumber;
    }

    public String getFacetId()
    {
        return FACET_ID;
    }

    public String getName()
    {
        // TODO: not needed, perhaps artifact/version metadata facet should be separate interface?
        return null;
    }

    public Map<String, String> toProperties()
    {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put( "type", type );
        if ( classifier != null )
        {
            properties.put( "classifier", classifier );
        }
        if ( timestamp != null )
        {
            properties.put( "timestamp", timestamp );
        }
        if ( buildNumber > 0 )
        {
            properties.put( "buildNumber", Integer.toString( buildNumber ) );
        }
        return properties;
    }

    public void fromProperties( Map<String, String> properties )
    {
        type = properties.get( "type" );
        classifier = properties.get( "classifier" );
        timestamp = properties.get( "timestamp" );
        String buildNumber = properties.get( "buildNumber" );
        if ( buildNumber != null )
        {
            this.buildNumber = Integer.valueOf( buildNumber );
        }
    }
}
