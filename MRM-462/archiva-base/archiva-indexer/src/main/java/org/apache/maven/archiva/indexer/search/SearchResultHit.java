package org.apache.maven.archiva.indexer.search;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchResultHit 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
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

    private String version = "";

    // Advanced hit, if artifact, all versions of artifact
    private List artifacts = new ArrayList();

    private List versions = new ArrayList();

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

    public void addArtifact( ArchivaArtifact artifact )
    {
        this.artifacts.add( artifact );
                
        String ver = artifact.getVersion();        

        if ( !this.versions.contains( ver ) )
        {
            this.versions.add( ver );
        }

        if ( StringUtils.isBlank( this.groupId ) )
        {
            this.groupId = artifact.getGroupId();
        }

        if ( StringUtils.isBlank( this.artifactId ) )
        {
            this.artifactId = artifact.getArtifactId();
        }

        if ( StringUtils.isBlank( this.version ) )
        {
            this.version = ver;            
        }
    }

    public List getArtifacts()
    {
        return artifacts;
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

    public List getVersions()
    {
        return versions;
    }
}
