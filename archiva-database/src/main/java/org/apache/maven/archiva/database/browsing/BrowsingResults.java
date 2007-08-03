package org.apache.maven.archiva.database.browsing;

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

import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * BrowsingResults 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BrowsingResults
{
    private String selectedGroupId;
    
    private String selectedArtifactId;

    private List groupIds = null;

    private List artifacts = null;

    private List versions = null;

    public BrowsingResults()
    {
        /* do nothing, this is the results of the root */
    }

    public BrowsingResults( String groupId )
    {
        this.selectedGroupId = groupId;
    }
    
    public BrowsingResults( String groupId, String artifactId )
    {
        this.selectedGroupId = groupId;
        this.selectedArtifactId = artifactId;
    }

    public List getArtifacts()
    {
        return artifacts;
    }

    public List getGroupIds()
    {
        return groupIds;
    }

    public String getSelectedArtifactId()
    {
        return selectedArtifactId;
    }

    public String getSelectedGroupId()
    {
        return selectedGroupId;
    }

    public List getVersions()
    {
        return versions;
    }

    public boolean hasArtifacts()
    {
        return CollectionUtils.isNotEmpty( artifacts );
    }

    public boolean hasGroupIds()
    {
        return CollectionUtils.isNotEmpty( groupIds );
    }

    public boolean hasVersions()
    {
        return CollectionUtils.isNotEmpty( versions );
    }

    public void setArtifacts( List artifacts )
    {
        this.artifacts = artifacts;
    }

    public void setGroupIds( List groupIds )
    {
        this.groupIds = groupIds;
    }

    public void setVersions( List versions )
    {
        this.versions = versions;
    }
}
