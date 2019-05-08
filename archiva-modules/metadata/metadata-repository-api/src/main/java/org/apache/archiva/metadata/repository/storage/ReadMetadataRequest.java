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

import org.apache.archiva.filter.Filter;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
public class ReadMetadataRequest
{
    private String repositoryId;

    private String namespace;

    private String projectId;

    private String projectVersion;

    private Filter<String> filter;

    /**
     * define this request as a ui request to remove some constraints added for optimisations
     * @since 2.0.0
     */
    private boolean browsingRequest;

    public ReadMetadataRequest()
    {
        // no op
    }

    public ReadMetadataRequest( String repositoryId, String namespace, String projectId, String projectVersion )
    {
        this.repositoryId = repositoryId;
        this.namespace = namespace;
        this.projectId = projectId;
        this.projectVersion = projectVersion;
    }

    public ReadMetadataRequest( String repositoryId, String namespace, String projectId, String projectVersion,
                                Filter<String> filter )
    {
        this( repositoryId, namespace, projectId, projectVersion );
        this.filter = filter;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public ReadMetadataRequest repositoryId( String repoId )
    {
        this.repositoryId = repoId;
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

    public boolean isBrowsingRequest()
    {
        return browsingRequest;
    }

    public void setBrowsingRequest( boolean browsingRequest )
    {
        this.browsingRequest = browsingRequest;
    }

    public ReadMetadataRequest browsingRequest( boolean browsingRequest )
    {
        this.browsingRequest = browsingRequest;
        return this;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "ReadMetadataRequest{" );
        sb.append( "repositoryId='" ).append( repositoryId ).append( '\'' );
        sb.append( ", namespace='" ).append( namespace ).append( '\'' );
        sb.append( ", projectId='" ).append( projectId ).append( '\'' );
        sb.append( ", projectVersion='" ).append( projectVersion ).append( '\'' );
        sb.append( ", filter=" ).append( filter );
        sb.append( ", browsingRequest=" ).append( browsingRequest );
        sb.append( '}' );
        return sb.toString();
    }
}
