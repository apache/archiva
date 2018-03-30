package org.apache.archiva.metadata.model.facets;

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

import org.apache.archiva.metadata.model.MetadataFacet;

import java.util.HashMap;
import java.util.Map;

public class RepositoryProblemFacet
    implements MetadataFacet
{
    public static final String FACET_ID = "org.apache.archiva.reports";

    private String repositoryId;

    private String namespace;

    private String project;

    private String version;

    private String id;

    private String message;

    private String problem;

    @Override
    public String getFacetId()
    {
        return FACET_ID;
    }

    @Override
    public String getName()
    {
        return createName( namespace, project, version, id );
    }

    @Override
    public Map<String, String> toProperties()
    {
        Map<String, String> map = new HashMap<>();
        map.put( "repositoryId", repositoryId );
        map.put( "namespace", namespace );
        map.put( "project", project );
        map.put( "version", version );
        if ( id != null )
        {
            map.put( "id", id );
        }
        map.put( "message", message );
        map.put( "problem", problem );
        return map;
    }

    @Override
    public void fromProperties( Map<String, String> properties )
    {
        repositoryId = properties.get( "repositoryId" );
        namespace = properties.get( "namespace" );
        project = properties.get( "project" );
        version = properties.get( "version" );
        id = properties.get( "id" );
        message = properties.get( "message" );
        problem = properties.get( "problem" );
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setProject( String project )
    {
        this.project = project;
    }

    public String getProject()
    {
        return project;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }

    public void setProblem( String problem )
    {
        this.problem = problem;
    }

    public String getProblem()
    {
        return problem;
    }

    public static String createName( String namespace, String project, String projectVersion, String id )
    {
        String name = namespace + "/" + project + "/" + projectVersion;
        if ( id != null )
        {
            name = name + "/" + id;
        }
        return name;
    }
}
