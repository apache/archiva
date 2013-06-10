package org.apache.archiva.metadata.repository.cassandra.model;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Cassandra storage model for {@link org.apache.archiva.metadata.model.MetadataFacet}
 *
 * @author Olivier Lamy
 */
@Entity
public class MetadataFacetModel
{
    // id is repositoryId + namespaceId + projectId + facetId + name + mapKey
    @Id
    @Column( name = "id" )
    private String id;

    @Column( name = "artifactMetadataModel" )
    private ArtifactMetadataModel artifactMetadataModel;

    @Column( name = "facetId" )
    private String facetId;

    @Column( name = "name" )
    private String name;

    @Column( name = "key" )
    private String key;

    @Column( name = "value" )
    private String value;

    public MetadataFacetModel()
    {
        // no op
    }

    public MetadataFacetModel( String id, ArtifactMetadataModel artifactMetadataModel, String facetId, String key,
                               String value, String name )
    {
        this.id = id;
        this.artifactMetadataModel = artifactMetadataModel;
        this.key = key;
        this.value = value;
        this.name = name;
        this.facetId = facetId;
    }

    public String getFacetId()
    {
        return facetId;
    }

    public void setFacetId( String facetId )
    {
        this.facetId = facetId;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public ArtifactMetadataModel getArtifactMetadataModel()
    {
        return artifactMetadataModel;
    }

    public void setArtifactMetadataModel( ArtifactMetadataModel artifactMetadataModel )
    {
        this.artifactMetadataModel = artifactMetadataModel;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        MetadataFacetModel that = (MetadataFacetModel) o;

        if ( !id.equals( that.id ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "MetadataFacetModel{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", artifactMetadataModel=" ).append( artifactMetadataModel );
        sb.append( ", key='" ).append( key ).append( '\'' );
        sb.append( ", value='" ).append( value ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }

    public static class KeyBuilder
    {

        private ArtifactMetadataModel artifactMetadataModel;

        private String key;

        private String name;

        private String facetId;

        private String repositoryId;

        public KeyBuilder()
        {

        }

        public KeyBuilder withArtifactMetadataModel( ArtifactMetadataModel artifactMetadataModel )
        {
            this.artifactMetadataModel = artifactMetadataModel;
            return this;
        }

        public KeyBuilder withKey( String key )
        {
            this.key = key;
            return this;
        }

        public KeyBuilder withName( String name )
        {
            this.name = name;
            return this;
        }

        public KeyBuilder withFacetId( String facetId )
        {
            this.facetId = facetId;
            return this;
        }

        public KeyBuilder withRepositoryId( String repositoryId )
        {
            this.repositoryId = repositoryId;
            return this;
        }

        public String build()
        {
            // FIXME add some controls
            // getArtifactMetadataModelId can have no namespace, no project and no projectid for statistics
            // only repositoryId with artifactMetadataModel
            return ( this.artifactMetadataModel == null
                ? this.repositoryId
                : this.artifactMetadataModel.getArtifactMetadataModelId() ) + "-" + this.facetId + "-" + this.name + "-"
                + this.key;
        }
    }
}
