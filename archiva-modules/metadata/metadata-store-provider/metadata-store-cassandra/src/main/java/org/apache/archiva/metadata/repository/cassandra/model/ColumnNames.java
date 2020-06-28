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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Cassandra column names
 * 
 * @author carlos@apache.org
 */
public enum ColumnNames
{
    FACET_ID( "facetId" ),
    REPOSITORY_NAME( "repositoryName" ),
    NAME( "name" ),
    NAMESPACE_ID( "namespaceId" ),
    PROJECT_ID( "projectId" ),
    PROJECT_VERSION( "projectVersion" ),
    KEY( "facetKey" ),
    VALUE( "value" ),
    ID( "id" ),
    SIZE( "size" ),
    MD5( "md5" ),
    SHA1( "sha1" ),
    PROJECT( "project" ),
    FILE_LAST_MODIFIED( "fileLastModified" ),
    VERSION( "version" ),
    GROUP_ID( "groupId" ),
    ARTIFACT_ID( "artifactId" ),
    DESCRIPTION( "description" ),
    URL( "url" ),
    WHEN_GATHERED( "whenGathered" ),
    CHECKSUM_ALG("checksumAlgorithm"),
    CHECKSUM_VALUE("checksumValue");

    private final String name;

    private ColumnNames( String name )
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }

}
