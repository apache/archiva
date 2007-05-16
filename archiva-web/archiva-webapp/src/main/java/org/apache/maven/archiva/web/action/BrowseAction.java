package org.apache.maven.archiva.web.action;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.browsing.BrowsingResults;
import org.apache.maven.archiva.database.browsing.RepositoryBrowsing;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

/**
 * Browse the repository.
 *
 * @todo cache browsing results.
 * @todo implement repository selectors (all or specific repository)
 * @todo implement security around browse (based on repository id at first)
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="browseAction"
 */
public class BrowseAction
    extends PlexusActionSupport
{
    /**
     * @plexus.requirement role-hint="default"
     */
    private RepositoryBrowsing repoBrowsing;

    private BrowsingResults results;

    private String groupId;

    private String artifactId;

    public String browse()
    {
        getLogger().info( ".browse()" );
        this.results = repoBrowsing.getRoot();
        return SUCCESS;
    }

    public String browseGroup()
    {
        getLogger().info( ".browseGroup( " + groupId + " )" );
        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        this.results = repoBrowsing.selectGroupId( groupId );
        return SUCCESS;
    }

    public String browseArtifact()
    {
        getLogger().info( ".browseArtifact( " + groupId + "," + artifactId + " )" );
        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            return ERROR;
        }

        if ( StringUtils.isEmpty( artifactId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a artifact ID to browse" );
            return ERROR;
        }

        this.results = repoBrowsing.selectArtifactId( groupId, artifactId );
        return SUCCESS;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public BrowsingResults getResults()
    {
        return results;
    }
}
