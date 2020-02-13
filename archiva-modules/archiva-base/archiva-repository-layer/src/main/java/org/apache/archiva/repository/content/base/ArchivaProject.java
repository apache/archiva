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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContent;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;

/**
 * Immutable class, that represents a project.
 */
public class ArchivaProject extends ArchivaContentItem implements Project
{
    private String namespace;
    private String id;
    private RepositoryContent repositoryContent;
    private StorageAsset asset;

    // Setting all setters to private. Builder is the way to go.
    private ArchivaProject() {

    }


    /**
     * Creates the builder that allows to create a new instance.
     * @param id the project id, must not be <code>null</code>
     * @return a builder instance
     */
    public static Builder withId( String id) {
        return new Builder( ).withId( id );
    }


    @Override
    public String getNamespace( )
    {
        return this.namespace;
    }

    @Override
    public String getId( )
    {
        return this.id;
    }

    @Override
    public RepositoryContent getRepository( )
    {
        return this.repositoryContent;
    }

    @Override
    public StorageAsset getAsset( )
    {
        return asset;
    }



    /*
     * Builder interface chaining is used to restrict mandatory attributes
     * This interface is for the optional arguments.
     */
    public interface OptBuilder {

        OptBuilder withAsset( StorageAsset asset );

        OptBuilder withNamespace( String namespace);

        OptBuilder withAttribute( String key, String value );

    }
    /*
     * Builder classes for instantiation
     */
    public static final class Builder implements OptBuilder
    {
        final private ArchivaProject project = new ArchivaProject();

        private Builder( )
        {

        }

        private Builder withId(String id) {
            if ( StringUtils.isEmpty( id ) ) {
                throw new IllegalArgumentException( "Null or empty value not allowed for id" );
            }
            project.id = id;
            return this;
        }


        public OptBuilder withRepository( RepositoryContent repository ) {
            project.repositoryContent = repository;
            return this;
        }

        @Override
        public OptBuilder withAsset( StorageAsset asset )
        {
            project.asset = asset;
            return this;
        }

        public OptBuilder withNamespace( String namespace) {
            if (namespace==null) {
                throw new IllegalArgumentException( "Null value not allowed for namespace" );
            }
            project.namespace = namespace;
            return this;
        }

        public OptBuilder withAttribute( String key, String value) {
            project.putAttribute( key, value );
            return this;
        }

        ArchivaProject build() {
            if (project.namespace==null) {
                project.namespace = "";
            }
            if (project.asset == null) {
                if (project.getRepository() instanceof ManagedRepositoryContent) {
                    project.asset = (( ManagedRepositoryContent)project.getRepository( )).getRepository( ).getAsset( "" );
                }

            }
            return project;
        }
    }



}
