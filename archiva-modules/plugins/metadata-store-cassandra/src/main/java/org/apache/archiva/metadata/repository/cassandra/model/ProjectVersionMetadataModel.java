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

import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.Scm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Entity
public class ProjectVersionMetadataModel
{
    // repositoryId + namespace + projectId + id (version)
    @Id
    private String rowId;

    @Column( name = "namespace" )
    private Namespace namespace;

    /**
     * id is the version
     */
    @Column( name = "id" )
    private String id;

    @Column( name = "projectId" )
    private String projectId;

    @Column(name = "url")
    private String url;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "organization")
    private Organization organization;

    @Column(name = "issueManagement")
    private IssueManagement issueManagement;

    @Column(name = "scm")
    private Scm scm;

    @Column(name = "ciManagement")
    private CiManagement ciManagement;

    // FIXME store those values in a separate table
    @Column(name = "licenses")
    private List<License> licenses = new ArrayList<License>();

    @Column(name = "mailingLists")
    private List<MailingList> mailingLists = new ArrayList<MailingList>();

    @Column(name = "dependencies")
    private List<Dependency> dependencies = new ArrayList<Dependency>();

    @Column(name = "incomplete")
    private boolean incomplete;

    public String getProjectId()
    {
        return projectId;
    }

    public void setProjectId( String projectId )
    {
        this.projectId = projectId;
    }

    public String getRowId()
    {
        return rowId;
    }

    public void setRowId( String rowId )
    {
        this.rowId = rowId;
    }

    // FIXME must be renamed getVersion !!!
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public Organization getOrganization()
    {
        return organization;
    }

    public void setOrganization( Organization organization )
    {
        this.organization = organization;
    }

    public IssueManagement getIssueManagement()
    {
        return issueManagement;
    }

    public void setIssueManagement( IssueManagement issueManagement )
    {
        this.issueManagement = issueManagement;
    }

    public Scm getScm()
    {
        return scm;
    }

    public void setScm( Scm scm )
    {
        this.scm = scm;
    }

    public CiManagement getCiManagement()
    {
        return ciManagement;
    }

    public void setCiManagement( CiManagement ciManagement )
    {
        this.ciManagement = ciManagement;
    }

    public boolean isIncomplete()
    {
        return incomplete;
    }

    public void setIncomplete( boolean incomplete )
    {
        this.incomplete = incomplete;
    }

    public Namespace getNamespace()
    {
        return namespace;
    }

    public void setNamespace( Namespace namespace )
    {
        this.namespace = namespace;
    }

    public List<License> getLicenses()
    {
        return licenses;
    }

    public void setLicenses( List<License> licenses )
    {
        this.licenses = licenses;
    }

    public List<MailingList> getMailingLists()
    {
        return mailingLists;
    }

    public void setMailingLists( List<MailingList> mailingLists )
    {
        this.mailingLists = mailingLists;
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies( List<Dependency> dependencies )
    {
        this.dependencies = dependencies;
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "ProjectVersionMetadataModel{" );
        sb.append( "rowId='" ).append( rowId ).append( '\'' );
        sb.append( ", namespace=" ).append( namespace );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( ", projectId='" ).append( projectId ).append( '\'' );
        sb.append( ", url='" ).append( url ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", organization=" ).append( organization );
        sb.append( ", issueManagement=" ).append( issueManagement );
        sb.append( ", scm=" ).append( scm );
        sb.append( ", ciManagement=" ).append( ciManagement );
        sb.append( ", incomplete=" ).append( incomplete );
        sb.append( '}' );
        return sb.toString();
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

        ProjectVersionMetadataModel that = (ProjectVersionMetadataModel) o;

        if ( !rowId.equals( that.rowId ) )
        {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode()
    {
        return rowId.hashCode();
    }

    public static class KeyBuilder
    {

        private String namespace;

        private String repositoryId;

        private String projectId;

        private String id;

        public KeyBuilder()
        {

        }

        public KeyBuilder withNamespace( Namespace namespace )
        {
            this.namespace = namespace.getName();
            this.repositoryId = namespace.getRepository().getId();
            return this;
        }

        public KeyBuilder withNamespace( String namespace )
        {
            this.namespace = namespace;
            return this;
        }

        public KeyBuilder withRepository( String repositoryId )
        {
            this.repositoryId = repositoryId;
            return this;
        }

        public KeyBuilder withRepository( Repository repository )
        {
            this.repositoryId = repository.getId();
            return this;
        }

        public KeyBuilder withProjectId( String projectId )
        {
            this.projectId = projectId;
            return this;
        }

        public KeyBuilder withId( String id )
        {
            this.id = id;
            return this;
        }

        public String build()
        {
            // FIXME add some controls
            return this.repositoryId + "-" + this.namespace + "-" + this.projectId + ( this.id == null
                ? ""
                : "-" + this.id );
        }
    }
}
