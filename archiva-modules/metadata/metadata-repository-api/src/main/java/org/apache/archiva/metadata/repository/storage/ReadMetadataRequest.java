package org.apache.archiva.metadata.repository.storage;
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

import org.apache.archiva.metadata.repository.filter.Filter;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
public class ReadMetadataRequest
{
    private String repoId;

    private String namespace;

    private String projectId;

    private String projectVersion;

    private Filter<String> filter;

    public ReadMetadataRequest()
    {
        // no op
    }

    public ReadMetadataRequest( String repoId, String namespace, String projectId, String projectVersion )
    {
        this.repoId = repoId;
        this.namespace = namespace;
        this.projectId = projectId;
        this.projectVersion = projectVersion;
    }

    public ReadMetadataRequest( String repoId, String namespace, String projectId, String projectVersion,
                                Filter<String> filter )
    {
        this( repoId, namespace, projectId, projectVersion );
        this.filter = filter;
    }

    public String getRepoId()
    {
        return repoId;
    }

    public void setRepoId( String repoId )
    {
        this.repoId = repoId;
    }

    public ReadMetadataRequest repoId( String repoId )
    {
        this.repoId = repoId;
        return this;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    public ReadMetadataRequest namespace( String namespace )
    {
        this.namespace = namespace;
        return this;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public void setProjectId( String projectId )
    {
        this.projectId = projectId;
    }

    public ReadMetadataRequest projectId( String projectId )
    {
        this.projectId = projectId;
        return this;
    }

    public String getProjectVersion()
    {
        return projectVersion;
    }

    public void setProjectVersion( String projectVersion )
    {
        this.projectVersion = projectVersion;
    }

    public ReadMetadataRequest projectVersion( String projectVersion )
    {
        this.projectVersion = projectVersion;
        return this;
    }

    public Filter<String> getFilter()
    {
        return filter;
    }

    public void setFilter( Filter<String> filter )
    {
        this.filter = filter;
    }

    public ReadMetadataRequest filter( Filter<String> filter )
    {
        this.filter = filter;
        return this;
    }
}
