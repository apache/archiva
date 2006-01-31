package org.apache.maven.repository.proxy.configuration;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.repository.proxy.repository.ProxyRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.proxy.configuration.ProxyConfiguration"
 */
public class ProxyConfiguration
{
    public static final String ROLE = ProxyConfiguration.class.getName();

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private boolean browsable;

    private ArtifactRepository repoCache;
    private List repositories = new ArrayList();

    public void setBrowsable( boolean browsable )
    {
        this.browsable = browsable;
    }

    public boolean isBrowsable()
    {
        return browsable;
    }

    public void setRepositoryCachePath( String repoCacheURL )
    {
        ArtifactRepositoryPolicy standardPolicy;
        standardPolicy = new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                                       ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );

        ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();

        repoCache = artifactRepositoryFactory.createArtifactRepository( "localCache", repoCacheURL, layout,
                                                                        standardPolicy, standardPolicy );
    }

    public ArtifactRepository getRepositoryCache()
    {
        return repoCache;
    }

    public String getRepositoryCachePath()
    {
        return repoCache.getBasedir();
    }

    public void addRepository( ProxyRepository repository )
    {
        repositories.add( repository );
    }

    public List getRepositories()
    {
        return Collections.unmodifiableList( repositories );
    }

    public void setRepositories( List repositories )
    {
        this.repositories = repositories;
    }
}
