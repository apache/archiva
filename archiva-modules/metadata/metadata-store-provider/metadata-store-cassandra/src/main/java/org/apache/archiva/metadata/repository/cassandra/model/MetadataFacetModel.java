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

import org.apache.archiva.metadata.repository.cassandra.CassandraUtils;

import static org.apache.archiva.metadata.repository.cassandra.model.ColumnNames.*;

/**
 * Cassandra storage model for {@link org.apache.archiva.metadata.model.MetadataFacet}
 *
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class MetadataFacetModel
{
    public static final String[] COLUMNS = new String[] { FACET_ID.toString(), KEY.toString(), VALUE.toString(),
        REPOSITORY_NAME.toString(), NAMESPACE_ID.toString(), PROJECT_ID.toString(), PROJECT_VERSION.toString() };

    private String facetId;

    private String key;

    private String name;

    private String value;

    private String projectVersion;

    public MetadataFacetModel()
    {
        // no op
    }

    public MetadataFacetModel( String facetId, String key, String value, String name, String projectVersion )
    {
        this.key = key;
        this.value = value;
        this.name = name;
        this.facetId = facetId;
        this.projectVersion = projectVersion;
    }

    public String getFacetId()
    {
        return facetId;
    }

    public void setFacetId( String facetId )
    {
        this.facetId = facetId;
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

    public String getProjectVersion()
    {
        return projectVersion;
    }

    public void setProjectVersion( String projectVersion )
    {
        this.projectVersion = projectVersion;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "MetadataFacetModel{" );
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
            String str = CassandraUtils.generateKey( this.artifactMetadataModel == null
                                                         ? this.repositoryId
                                                         : new ArtifactMetadataModel.KeyBuilder().withNamespace(
                                                             this.artifactMetadataModel.getNamespace() ) //
                                                             .withProject( this.artifactMetadataModel.getProject() )  //
                                                             .withProjectVersion(
                                                                 this.artifactMetadataModel.getProjectVersion() ) //
                                                             .withRepositoryId(
                                                                 this.artifactMetadataModel.getRepositoryId() ) //
                                                             .withId( this.artifactMetadataModel.getId() ) //
                                                             .build(), //
                                                     this.facetId, //
                                                     this.name, //
                                                     this.key
            );

            return str;
        }
    }
}
