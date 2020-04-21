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

import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Item selector for querying artifacts and other content items.
 */
public class ArchivaItemSelector implements ItemSelector
{

    private String projectId = "";
    private String version = "";
    private String artifactVersion = "";
    private String artifactId = "";
    private String namespace = "";
    private String type = "";
    private String classifier = "";
    private String extension = "";
    private Map<String, String> attributes;
    private boolean includeRelatedArtifacts = false;
    private boolean recurse = false;


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

        public Builder withItem( ContentItem item ) {
            if (item instanceof Namespace ) {
                Namespace ns = (Namespace) item;
                selector.namespace = ns.getNamespace();
            } else if (item instanceof Project ) {
                Project proj = (Project)item;
                selector.namespace = proj.getNamespace( ).getNamespace( );
                selector.projectId = proj.getId( );
            } else if (item instanceof Version) {
                Version version = (Version)item;
                selector.namespace = version.getProject( ).getNamespace( ).getNamespace( );
                selector.projectId = version.getProject( ).getId( );
                selector.version = version.getVersion( );
            } else if (item instanceof Artifact ) {
                Artifact artifact = (Artifact)item;
                selector.namespace = artifact.getVersion( ).getProject( ).getNamespace( ).getNamespace( );
                selector.projectId = artifact.getVersion( ).getProject( ).getId( );
                selector.version = artifact.getVersion( ).getVersion( );
                selector.artifactId = artifact.getId( );
                selector.artifactVersion = artifact.getArtifactVersion( );
                selector.extension = artifact.getExtension( );
            }
            for (Map.Entry<String, String> att : item.getAttributes().entrySet()) {
                selector.setAttribute( att.getKey( ), att.getValue( ) );
            }
            return this;
        }

        public Builder withNamespace( String namespace )
        {
            if (namespace!=null)
            {
                selector.namespace = namespace;
            }
            return this;
        }


        public Builder withProjectId( String projectId )
        {
            if (projectId!=null)
            {
                selector.projectId = projectId;
            }
            return this;
        }


        public Builder withVersion( String version )
        {
            if (version!=null)
            {
                selector.version = version;
            }
            return this;
        }


        public Builder withArtifactVersion( String artifactVersion )
        {
            if (artifactVersion!=null)
            {
                selector.artifactVersion = artifactVersion;
            }
            return this;
        }


        public Builder withArtifactId( String artifactId )
        {
            if (artifactId!=null)
            {
                selector.artifactId = artifactId;
            }
            return this;
        }


        public Builder withType( String type )
        {
            if (type!=null)
            {
                selector.type = type;
            }
            return this;
        }


        public Builder withClassifier( String classifier )
        {
            if (classifier != null )
            {
                selector.classifier = classifier;
            }
            return this;
        }


        public Builder withAttribute( String key, String value )
        {
            selector.setAttribute( key, value );
            return this;
        }

        public Builder withExtension( String extension )
        {
            if (extension!=null)
            {
                selector.extension = extension;
            }
            return this;
        }

        public Builder includeRelatedArtifacts() {
            selector.includeRelatedArtifacts = true;
            return this;
        }

        public Builder recurse() {
            selector.recurse = true;
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
    public String getExtension(  )
    {
        return extension;
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
    public boolean recurse( )
    {
        return recurse;
    }

    @Override
    public boolean includeRelatedArtifacts( )
    {
        return includeRelatedArtifacts;
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

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        ArchivaItemSelector that = (ArchivaItemSelector) o;

        if ( includeRelatedArtifacts != that.includeRelatedArtifacts ) return false;
        if ( recurse != that.recurse ) return false;
        if ( !projectId.equals( that.projectId ) ) return false;
        if ( !version.equals( that.version ) ) return false;
        if ( !artifactVersion.equals( that.artifactVersion ) ) return false;
        if ( !artifactId.equals( that.artifactId ) ) return false;
        if ( !namespace.equals( that.namespace ) ) return false;
        if ( !type.equals( that.type ) ) return false;
        if ( !classifier.equals( that.classifier ) ) return false;
        if ( !extension.equals( that.extension ) ) return false;
        return attributes != null ? attributes.equals( that.attributes ) : that.attributes == null;
    }

    @Override
    public int hashCode( )
    {
        int result = projectId.hashCode( );
        result = 31 * result + version.hashCode( );
        result = 31 * result + artifactVersion.hashCode( );
        result = 31 * result + artifactId.hashCode( );
        result = 31 * result + namespace.hashCode( );
        result = 31 * result + type.hashCode( );
        result = 31 * result + classifier.hashCode( );
        result = 31 * result + extension.hashCode( );
        result = 31 * result + ( attributes != null ? attributes.hashCode( ) : 0 );
        result = 31 * result + ( includeRelatedArtifacts ? 1 : 0 );
        result = 31 * result + ( recurse ? 1 : 0 );
        return result;
    }
}
