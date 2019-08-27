package org.apache.archiva.metadata.model;

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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Information about a dependency that this project has on another project or artifact.
 *
 * TODO will be reviewing what is appropriate for the base here - rest should be in a maven dependency facet - avoid details on it externally
 */
@XmlRootElement(name = "dependency")
public class Dependency
    implements Serializable
{
    /**
     * The Maven classifier of the dependency.
     */
    private String classifier;

    /**
     * Whether the dependency is optional or required.
     */
    private boolean optional;

    /**
     * The Maven scope of the dependency - <tt>compile</tt> (default), <tt>runtime</tt>, etc.
     */
    private String scope;

    /**
     * The system path of the file of the dependency artifact to use.
     */
    private String systemPath;

    /**
     * The Maven type of the dependency.
     */
    private String type;

    /**
     * The Maven artifact ID of the dependency.
     */
    private String artifactId;

    /**
     * The Maven group ID of the dependency.
     */
    private String namespace;

    /**
     * The project id
     */
    private String projectId;

    /**
     * The version of the artifact to depend on. If this refers to a project version then the repository implementation
     * may choose the most appropriate artifact version to use.
     */
    private String version;

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String getScope()
    {
        return scope;
    }

    public void setSystemPath( String systemPath )
    {
        this.systemPath = systemPath;
    }

    public String getSystemPath()
    {
        return systemPath;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setNamespace(String groupId )
    {
        this.namespace = groupId;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "Dependency" );
        sb.append( "{classifier='" ).append( classifier ).append( '\'' );
        sb.append( ", optional=" ).append( optional );
        sb.append( ", scope='" ).append( scope ).append( '\'' );
        sb.append( ", systemPath='" ).append( systemPath ).append( '\'' );
        sb.append( ", type='" ).append( type ).append( '\'' );
        sb.append( ", artifactId='" ).append( artifactId ).append( '\'' );
        sb.append( ", namespace='" ).append(namespace).append( '\'' );
        sb.append( ", version='" ).append( version ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
