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
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * Base implementation of artifact. A builder is used to create instances.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ArchivaArtifact extends ArchivaContentItem implements Artifact
{
    private String namespace;
    private String id;
    private String artifactVersion;
    private Version version;
    private String type;
    private String classifier;
    private String remainder;
    private String contentType;
    private StorageAsset asset;

    private ArchivaArtifact() {

    }

    @Override
    public String getNamespace( )
    {
        return namespace;
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
    public StorageAsset getAsset( )
    {
        return asset;
    }

    public static ArtifactVersionBuilder withId(String id) {
        return new Builder( ).withId( id );
    }


    public interface ArtifactVersionBuilder {
        VersionBuilder withArtifactVersion( String version );
    }
    public interface VersionBuilder {
        AssetBuilder withVersion( Version version );
    }
    public interface AssetBuilder {
        Builder withAsset( StorageAsset asset );
    }


    public static class Builder implements ArtifactVersionBuilder, VersionBuilder,
        AssetBuilder
    {
        ArchivaArtifact artifact = new ArchivaArtifact( );

        public ArtifactVersionBuilder withId(String id) {
            artifact.id = id;
            return this;
        }


        @Override
        public VersionBuilder withArtifactVersion( String version )
        {
            if ( StringUtils.isEmpty( version ) ) {
                throw new IllegalArgumentException( "version may not be null or empty" );
            }
            artifact.artifactVersion = version;
            return this;
        }

        @Override
        public AssetBuilder withVersion( Version version )
        {
            if (version==null) {
                throw new IllegalArgumentException( "version may not be null" );
            }
            artifact.version = version;
            return this;
        }

        public Builder withAsset(StorageAsset asset) {
            if (asset==null) {
                throw new IllegalArgumentException( "Asset may not be null" );
            }
            artifact.asset = asset;
            return this;
        }

        public Builder withNamespace(String namespace) {
            artifact.namespace = namespace;
            return this;
        }

        public Builder withType(String type) {
            artifact.type = type;
            return this;
        }

        public Builder withClassifier(String classifier) {
            artifact.classifier = classifier;
            return this;
        }

        public Builder withRemainder(String remainder) {
            artifact.remainder = remainder;
            return this;
        }

        public Builder withContentType(String contentType) {
            artifact.contentType = contentType;
            return this;
        }

        public ArchivaArtifact build() {
            if (artifact.namespace==null) {
                artifact.namespace = "";
            }
            if (artifact.classifier==null) {
                artifact.classifier = "";
            }
            if (artifact.type == null)  {
                artifact.type = "";
            }
            if (artifact.contentType==null) {
                artifact.contentType = "";
            }

            return artifact;
        }
    }
}
