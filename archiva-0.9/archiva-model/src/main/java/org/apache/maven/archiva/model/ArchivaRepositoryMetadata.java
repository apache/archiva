package org.apache.maven.archiva.model;

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

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ArchivaRepositoryMetadata 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaRepositoryMetadata
    implements RepositoryContent
{
    private List availableVersions = new ArrayList();
    
    private RepositoryContentKey key;
    
    private String releasedVersion;

    public ArchivaRepositoryMetadata( ArtifactRepository repository, String groupId, String artifactId, String version )
    {
        this.key = new RepositoryContentKey( repository, groupId, artifactId, version );
    }

    public List getAvailableVersions()
    {
        return availableVersions;
    }

    public String getReleasedVersion()
    {
        return releasedVersion;
    }

    public RepositoryContentKey getRepositoryContentKey()
    {
        return this.key;
    }

    public void setAvailableVersions( List availableVersions )
    {
        this.availableVersions = availableVersions;
    }

    public void setReleasedVersion( String releasedVersion )
    {
        this.releasedVersion = releasedVersion;
    }

    public void setRepositoryContentKey( RepositoryContentKey key )
    {
        this.key = key;
    }

}
