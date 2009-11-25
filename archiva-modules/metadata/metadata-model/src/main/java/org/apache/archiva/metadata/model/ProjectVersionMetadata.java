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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectVersionMetadata
{
    private String id;

    private String url;

    private String name;

    private String description;

    private Organization organization;

    private IssueManagement issueManagement;

    private Scm scm;

    private CiManagement ciManagement;

    private List<License> licenses;

    private Map<String, ProjectVersionFacet> facets = new HashMap<String, ProjectVersionFacet>();;

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public String getUrl()
    {
        return url;
    }

    public String getName()
    {
        return name;
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

    public List<License> getLicenses()
    {
        return licenses;
    }

    public void setLicenses( List<License> licenses )
    {
        this.licenses = licenses;
    }

    public void addLicense( License license )
    {
        if ( this.licenses == null )
        {
            this.licenses = new ArrayList<License>();
        }
        this.licenses.add( license );
    }

    public void addFacet( ProjectVersionFacet mavenProjectFacet )
    {
        this.facets.put( mavenProjectFacet.getFacetId(), mavenProjectFacet );
    }

    public ProjectVersionFacet getFacet( String facetId )
    {
        return this.facets.get( facetId );
    }

    public Collection<ProjectVersionFacet> getAllFacets()
    {
        return this.facets.values();
    }

    public Collection<String> getAllFacetIds()
    {
        return this.facets.keySet();
    }
}
