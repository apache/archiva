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
import org.apache.archiva.repository.content.ArtifactType;
import org.apache.archiva.repository.content.BaseArtifactTypes;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.builder.ArtifactOptBuilder;
import org.apache.archiva.repository.content.base.builder.ArtifactVersionBuilder;
import org.apache.archiva.repository.content.base.builder.ArtifactWithIdBuilder;
import org.apache.archiva.repository.content.base.builder.WithVersionObjectBuilder;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

/**
 * Base implementation of artifact.
 * <p>
 * You have to use the builder method {@link #withAsset(StorageAsset)} to create a instance.
 * The build() method can be called after the required attributes are set.
 * <p>
 * Artifacts are equal if the following coordinates match:
 * <ul>
 *     <li>repository</li>
 *     <li>asset</li>
 *     <li>version</li>
 *     <li>artifactId</li>
 *     <li>artifactVersion</li>
 *     <li>type</li>
 *     <li>classifier</li>
 *     <li>artifactType</li>
 * </ul>
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ArchivaArtifact extends ArchivaContentItem implements Artifact
{
    private String id;
    private String artifactVersion;
    private Version version;
    private String type;
    private String classifier;
    private String remainder;
    private String contentType;
    private ArtifactType artifactType;

    private ArchivaArtifact( )
    {

    }


    @Override
    public String getId( )
    {
        return id;
    }

    @Override
    public String getArtifactVersion( )
    {
        return artifactVersion;
    }

    @Override
    public Version getVersion( )
    {
        return version;
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
    public String getRemainder( )
    {
        return remainder;
    }

    @Override
    public String getContentType( )
    {
        return contentType;
    }

    @Override
    public ArtifactType getArtifactType( )
    {
        return artifactType;
    }


    /**
     * Returns the builder for creating a new artifact instance. You have to fill the
     * required attributes before the build() method is available.
     *
     * @param asset the storage asset representing the artifact
     * @return a builder for creating new artifact instance
     */
    public static WithVersionObjectBuilder withAsset( StorageAsset asset )
    {
        return new Builder( ).withAsset( asset );
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        ArchivaArtifact that = (ArchivaArtifact) o;

        if ( !id.equals( that.id ) ) return false;
        if ( !artifactVersion.equals( that.artifactVersion ) ) return false;
        if ( !version.equals( that.version ) ) return false;
        if ( !type.equals( that.type ) ) return false;
        if ( !artifactType.equals(that.artifactType)) return false;
        return classifier.equals( that.classifier );
    }

    @Override
    public int hashCode( )
    {
        int result = super.hashCode( );
        result = 31 * result + id.hashCode( );
        result = 31 * result + artifactVersion.hashCode( );
        result = 31 * result + version.hashCode( );
        result = 31 * result + type.hashCode( );
        result = 31 * result + classifier.hashCode( );
        result = 31 * result + artifactType.hashCode( );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "ArchivaArtifact{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", artifactVersion='" ).append( artifactVersion ).append( '\'' );
        sb.append( ", version=" ).append( version );
        sb.append( ", type='" ).append( type ).append( '\'' );
        sb.append( ", classifier='" ).append( classifier ).append( '\'' );
        sb.append( ", remainder='" ).append( remainder ).append( '\'' );
        sb.append( ", contentType='" ).append( contentType ).append( '\'' );
        sb.append( ", artifactType=" ).append( artifactType );
        sb.append( '}' );
        return sb.toString( );
    }

    public static String defaultString( String value )
    {
        if ( value == null )
        {
            return "";
        }

        return value.trim();
    }

    public String toKey(  )
    {
        StringBuilder key = new StringBuilder();

        key.append( defaultString( getVersion().getProject().getNamespace().getNamespace() )).append( ":" );
        key.append( defaultString( getId() ) ).append( ":" );
        key.append( defaultString( getVersion().getVersion() ) ).append( ":" );
        key.append( defaultString( getArtifactVersion( ) ) ).append( ":" );
        key.append( defaultString( getClassifier() ) ).append( ":" );
        key.append( defaultString( getRemainder() ) );

        return key.toString();
    }

    private static class Builder
        extends ContentItemBuilder<ArchivaArtifact, ArtifactOptBuilder, WithVersionObjectBuilder>
        implements ArtifactVersionBuilder, WithVersionObjectBuilder, ArtifactWithIdBuilder, ArtifactOptBuilder
    {

        Builder( )
        {
            super( new ArchivaArtifact( ) );
        }

        @Override
        protected ArtifactOptBuilder getOptBuilder( )
        {
            return this;
        }

        @Override
        protected WithVersionObjectBuilder getNextBuilder( )
        {
            return this;
        }

        @Override
        public ArtifactWithIdBuilder withVersion( Version version )
        {
            if ( version == null )
            {
                throw new IllegalArgumentException( "version may not be null" );
            }
            item.version = version;
            super.setRepository( version.getRepository( ) );
            return this;
        }

        @Override
        public ArtifactOptBuilder withId( String id )
        {
            if ( StringUtils.isEmpty( id ) )
            {
                throw new IllegalArgumentException( "Artifact id may not be null or empty" );
            }
            item.id = id;
            return this;
        }


        @Override
        public ArtifactOptBuilder withArtifactVersion( String version )
        {
            if ( version == null )
            {
                throw new IllegalArgumentException( "version may not be null" );
            }
            item.artifactVersion = version;
            return this;
        }

        @Override
        public ArtifactOptBuilder withType( String type )
        {
            item.type = type;
            return this;
        }

        @Override
        public ArtifactOptBuilder withClassifier( String classifier )
        {
            item.classifier = classifier;
            return this;
        }

        @Override
        public ArtifactOptBuilder withRemainder( String remainder )
        {
            item.remainder = remainder;
            return this;
        }

        @Override
        public ArtifactOptBuilder withContentType( String contentType )
        {
            item.contentType = contentType;
            return this;
        }

        @Override
        public ArtifactOptBuilder withArtifactType( ArtifactType type )
        {
            item.artifactType = type;
            return this;
        }

        @Override
        public ArchivaArtifact build( )
        {
            super.build( );
            if ( item.artifactVersion == null )
            {
                item.artifactVersion = "";
            }
            if ( item.classifier == null )
            {
                item.classifier = "";
            }
            if ( item.type == null )
            {
                item.type = "";
            }
            if ( item.contentType == null )
            {
                item.contentType = "";
            }
            if ( item.remainder == null )
            {
                item.remainder = "";
            }
            if (item.artifactType==null) {
                item.artifactType = BaseArtifactTypes.MAIN;
            }

            return item;
        }
    }
}
