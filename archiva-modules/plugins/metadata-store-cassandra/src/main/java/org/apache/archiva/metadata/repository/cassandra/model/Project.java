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
import java.io.Serializable;

/**
 * @author Olivier Lamy
 */
@Entity
public class Project
    implements Serializable
{
    @Id
    @Column( name = "projectKey" )
    private String projectKey;

    @Column( name = "projectId" )
    private String projectId;


    @Column( name = "repository" )
    private Namespace namespace;

    public Project()
    {
        // no op
    }

    public Project( String projectKey, String projectId, Namespace namespace )
    {
        this.projectId = projectId;
        this.projectKey = projectKey;
        this.namespace = namespace;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setProjectKey( String projectKey )
    {
        this.projectKey = projectKey;
    }

    public Namespace getNamespace()
    {
        return namespace;
    }

    public void setNamespace( Namespace namespace )
    {
        this.namespace = namespace;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public void setProjectId( String projectId )
    {
        this.projectId = projectId;
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

        Project project = (Project) o;

        if ( !projectKey.equals( project.projectKey ) )
        {
            return false;
        }
        if ( !namespace.equals( project.namespace ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = projectKey.hashCode();
        result = 31 * result + namespace.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "Project{" );
        sb.append( "projectKey='" ).append( projectKey ).append( '\'' );
        sb.append( ", projectId='" ).append( projectId ).append( '\'' );
        sb.append( ", namespace=" ).append( namespace );
        sb.append( '}' );
        return sb.toString();
    }

    public static class KeyBuilder
    {

        private Namespace namespace;

        private String projectId;

        public KeyBuilder()
        {
            // no op
        }

        public KeyBuilder withNamespace( Namespace namespace )
        {
            this.namespace = namespace;
            return this;
        }

        public KeyBuilder withProjectId( String projectId )
        {
            this.projectId = projectId;
            return this;
        }


        public String build()
        {
            // FIXME add some controls
            return new Namespace.KeyBuilder().withNamespace( this.namespace ).build() + "-" + this.projectId;
        }
    }
}
