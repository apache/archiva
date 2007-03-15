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

import org.apache.maven.archiva.common.utils.RepositoryURL;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * AbstractArchivaRepository 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractArchivaRepository
{
    protected ArtifactRepositoryLayout layout;

    protected ArtifactRepositoryPolicy releases;

    protected ArtifactRepositoryPolicy snapshots;

    protected boolean blacklisted;

    public AbstractArchivaRepository()
    {

    }

    /**
     * Construct a Repository.
     * 
     * @param id the unique identifier for this repository.
     * @param name the name for this repository.
     * @param url the base URL for this repository (this should point to the top level URL for the entire repository)
     * @param layout the layout technique for this repository.
     */
    public AbstractArchivaRepository( String id, String name, String url, ArtifactRepositoryLayout layout )
    {
        setId( id );
        setName( name );
        setUrl( url );
        setLayout( layout );
    }

    public abstract void setUrl( String url );

    public abstract String getUrl();

    public abstract void setName( String name );

    public abstract void setId( String id );

    public boolean isBlacklisted()
    {
        return blacklisted;
    }

    public void setBlacklisted( boolean blacklisted )
    {
        this.blacklisted = blacklisted;
    }

    public ArtifactRepositoryLayout getLayout()
    {
        return layout;
    }

    public void setLayout( ArtifactRepositoryLayout layout )
    {
        this.layout = layout;
    }

    public ArtifactRepositoryPolicy getReleases()
    {
        return releases;
    }

    public void setReleases( ArtifactRepositoryPolicy releases )
    {
        this.releases = releases;
    }

    public ArtifactRepositoryPolicy getSnapshots()
    {
        return snapshots;
    }

    public void setSnapshots( ArtifactRepositoryPolicy snapshots )
    {
        this.snapshots = snapshots;
    }

    public boolean isRemote()
    {
        return !getRepositoryURL().getProtocol().equals( "file" );
    }

    public boolean isManaged()
    {
        return getRepositoryURL().getProtocol().equals( "file" );
    }

    public RepositoryURL getRepositoryURL()
    {
        return new RepositoryURL( getUrl() );
    }
}
