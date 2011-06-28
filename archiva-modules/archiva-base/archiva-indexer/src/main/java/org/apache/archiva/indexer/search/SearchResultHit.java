package org.apache.archiva.indexer.search;

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

import java.util.ArrayList;
import java.util.List;

/**
 * SearchResultHit 
 *
 * @version $Id: SearchResultHit.java 740552 2009-02-04 01:09:17Z oching $
 */
public class SearchResultHit
{
    // The (optional) context for this result.
    private String context;

    // Basic hit, direct to non-artifact resource.
    private String url;

    // Advanced hit, reference to groupId.
    private String groupId;

    //  Advanced hit, reference to artifactId.
    private String artifactId;

    // TODO: remove/deprecate this field!
    private String version = "";
    
    private String repositoryId = "";

    private List<String> versions = new ArrayList<String>();

    public String getContext()
    {
        return context;
    }

    public void setContext( String context )
    {
        this.context = context;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getUrlFilename()
    {
        return this.url.substring( this.url.lastIndexOf( '/' ) );
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public List<String> getVersions()
    {
        return versions;
    }

    public void setVersions(List<String> versions)
    {
        this.versions = versions;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }
    
    public void addVersion( String version )
    {
        versions.add( version );
    }

    @Override
    public String toString()
    {
        return "SearchResultHit{" + "context='" + context + '\'' + ", url='" + url + '\'' + ", groupId='" + groupId
            + '\'' + ", artifactId='" + artifactId + '\'' + ", version='" + version + '\'' + ", repositoryId='"
            + repositoryId + '\'' + ", versions=" + versions + '}';
    }
}
