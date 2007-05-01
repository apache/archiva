package org.apache.maven.archiva.repository;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * Repository 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class Repository
    implements ArtifactRepository
{
    protected String id;

    protected String name;

    protected String source;

    protected RepositoryURL url;

    protected ArtifactRepositoryLayout layout;

    protected ArtifactRepositoryPolicy releases;

    protected ArtifactRepositoryPolicy snapshots;

    protected boolean blacklisted;

    /* .\ Constructor \.__________________________________________________ */

    /**
     * Construct a Repository.
     * 
     * @param id the unique identifier for this repository.
     * @param name the name for this repository.
     * @param url the base URL for this repository (this should point to the top level URL for the entire repository)
     * @param layout the layout technique for this repository.
     */
    public Repository( String id, String name, String url, ArtifactRepositoryLayout layout )
    {
        this.id = id;
        this.name = name;
        this.url = new RepositoryURL( url );
        this.layout = layout;
    }

    /* .\ Information \.__________________________________________________ */

    /**
     * Get the unique ID for this repository.
     * 
     * @return the unique ID for this repository.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Get the Name of this repository.
     * This is usually the human readable name for the repository.
     * 
     * @return the name of this repository.
     */
    public String getName()
    {
        return name;
    }

    public String getUrl()
    {
        return url.toString();
    }

    public void setLayout( ArtifactRepositoryLayout layout )
    {
        this.layout = layout;
    }

    public ArtifactRepositoryLayout getLayout()
    {
        return layout;
    }

    public void setSource( String source )
    {
        this.source = source;
    }

    public String getSource()
    {
        return source;
    }

    /* .\ Tasks \.________________________________________________________ */

    public String pathOf( Artifact artifact )
    {
        return getLayout().pathOf( artifact );
    }

    /* .\ State \.________________________________________________________ */

    public void setBlacklisted( boolean blacklisted )
    {
        this.blacklisted = blacklisted;
    }

    public boolean isBlacklisted()
    {
        return blacklisted;
    }

    public boolean isManaged()
    {
        return this.url.getProtocol().equals( "file" );
    }

    public boolean isRemote()
    {
        return !this.url.getProtocol().equals( "file" );
    }

    public void setSnapshots( ArtifactRepositoryPolicy snapshots )
    {
        this.snapshots = snapshots;
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        return snapshots;
    }

    public void setReleases( ArtifactRepositoryPolicy releases )
    {
        this.releases = releases;
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        return releases;
    }

    public boolean equals( Object other )
    {
        return ( other == this || ( ( other instanceof Repository ) && ( (Repository) other ).getId().equals( getId() ) ) );
    }

    public int hashCode()
    {
        return getId().hashCode();
    }

    /* .\ ArtifactRepository Requirements \.______________________________ */

    public String getBasedir()
    {
        return url.getPath();
    }

    public String getKey()
    {
        return getId();
    }

    public String getProtocol()
    {
        return url.getProtocol();
    }

    public boolean isUniqueVersion()
    {
        // TODO: Determine Importance
        return false;
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata )
    {
        return layout.pathOfRemoteRepositoryMetadata( artifactMetadata );
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository )
    {
        return layout.pathOfLocalRepositoryMetadata( metadata, repository );
    }

}
