package org.apache.archiva.repository.content.base;

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

import org.apache.archiva.repository.content.ItemSelector;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Item selector for querying artifacts and other content items.
 */
public class ArchivaItemSelector implements ItemSelector
{

    private String projectId = null;
    private String version = null;
    private String artifactVersion = null;
    private String artifactId = null;
    private String namespace = "";
    private String type = null;
    private String classifier = null;
    private String extension = null;
    private Map<String, String> attributes;


    private ArchivaItemSelector( )
    {

    }

    public static Builder builder( )
    {
        return new Builder( );
    }

    public static class Builder
    {
        private final ArchivaItemSelector selector = new ArchivaItemSelector( );

        public Builder withNamespace( String namespace )
        {
            selector.namespace = namespace;
            return this;
        }


        public Builder withProjectId( String projectId )
        {
            selector.projectId = projectId;
            return this;
        }


        public Builder withVersion( String version )
        {
            selector.version = version;
            return this;
        }


        public Builder withArtifactVersion( String artifactVersion )
        {
            selector.artifactVersion = artifactVersion;
            return this;
        }


        public Builder withArtifactId( String artifactId )
        {
            selector.artifactId = artifactId;
            return this;
        }


        public Builder withType( String type )
        {
            selector.type = type;
            return this;
        }


        public Builder withClassifier( String classifier )
        {
            selector.classifier = classifier;
            return this;
        }


        public Builder withAttribute( String key, String value )
        {
            selector.setAttribute( key, value );
            return this;
        }

        public Builder withExtension( String extension )
        {
            selector.extension = extension;
            return this;
        }

        public ArchivaItemSelector build( )
        {
            return selector;
        }
    }

    private void setAttribute( String key, String value )
    {
        if ( this.attributes == null )
        {
            this.attributes = new HashMap<>( );
        }
        this.attributes.put( key, value );
    }

    @Override
    public String getProjectId( )
    {
        return projectId;
    }

    @Override
    public String getNamespace( )
    {
        return namespace;
    }

    @Override
    public String getVersion( )
    {
        return version;
    }

    @Override
    public String getArtifactVersion( )
    {
        return artifactVersion;
    }

    @Override
    public String getArtifactId( )
    {
        return artifactId;
    }

    @Override
    public String getType( )
    {
        return type;
    }

    @Override
    public String getClassifier( )
    {
        return classifier;
    }

    @Override
    public String getAttribute( String key )
    {
        if ( this.attributes == null || !this.attributes.containsKey( key ) )
        {

            return "";
        }
        else
        {
            return this.attributes.get( key );
        }
    }

    @Override
    public String getExtension( String extension )
    {
        return null;
    }

    @Override
    public Map<String, String> getAttributes( )
    {
        if ( this.attributes == null )
        {
            return Collections.emptyMap( );
        }
        else
        {
            return Collections.unmodifiableMap( this.attributes );
        }
    }

    @Override
    public boolean hasAttributes( )
    {
        return attributes != null && attributes.size( ) > 0;
    }

    @Override
    public boolean hasExtension( )
    {
        return StringUtils.isNotEmpty( extension );
    }
}
