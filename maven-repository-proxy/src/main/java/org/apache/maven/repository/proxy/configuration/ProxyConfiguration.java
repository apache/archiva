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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class to represent the configuration file for the proxy
 *
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

    /**
     * Method to set/unset the web-view of the repository cache
     *
     * @param browsable set to true to enable the web-view of the proxy repository cache
     */
    public void setBrowsable( boolean browsable )
    {
        this.browsable = browsable;
    }

    /**
     * Used to determine if the repsented configuration allows web view of the repository cache
     *
     * @return true if the repository cache is configured for web view.
     */
    public boolean isBrowsable()
    {
        return browsable;
    }

    /**
     * Used to set the location where the proxy should cache the configured repositories
     *
     * @param path
     */
    public void setRepositoryCachePath( String path )
    {
        ArtifactRepositoryPolicy standardPolicy;
        standardPolicy = new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                                       ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );

        ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();

        repoCache = artifactRepositoryFactory.createArtifactRepository( "localCache",
                                                                        "file://" + new File( path ).getAbsolutePath(),
                                                                        layout, standardPolicy, standardPolicy );
    }

    /**
     * Used to retrieve an ArtifactRepository Object of the proxy cache
     *
     * @return the ArtifactRepository representation of the proxy cache
     */
    public ArtifactRepository getRepositoryCache()
    {
        return repoCache;
    }

    /**
     * Used to retrieved the absolute path of the repository cache
     *
     * @return path to the proxy cache
     */
    public String getRepositoryCachePath()
    {
        return repoCache.getBasedir();
    }

    /**
     * Used to add proxied repositories.
     *
     * @param repository the repository to be proxied
     */
    public void addRepository( ProxyRepository repository )
    {
        repositories.add( repository );
    }

    /**
     * Used to retrieve an unmodifyable list of proxied repositories. They returned list determines the search sequence
     * for retrieving artifacts.
     *
     * @return a list of ProxyRepository objects representing proxied repositories
     */
    public List getRepositories()
    {
        return Collections.unmodifiableList( repositories );
    }

    /**
     * Used to set the list of repositories to be proxied.  This replaces any repositories already added to this
     * configuraion instance.  Useful for re-arranging an existing proxied list.
     *
     * @param repositories
     */
    public void setRepositories( List repositories )
    {
        this.repositories = repositories;
    }

    /**
     * Uses maven-proxy classes to read a maven-proxy properties configuration
     *
     * @param mavenProxyConfigurationFile The location of the maven-proxy configuration file
     * @throws ValidationException When a problem occured while processing the properties file
     * @throws IOException         When a problem occured while reading the property file
     */
    public void loadMavenProxyConfiguration( File mavenProxyConfigurationFile )
        throws ValidationException, IOException
    {
        MavenProxyPropertyLoader loader = new MavenProxyPropertyLoader();
        RetrievalComponentConfiguration rcc = loader.load( new FileInputStream( mavenProxyConfigurationFile ) );

        this.setRepositoryCachePath( rcc.getLocalStore() );
        this.setBrowsable( rcc.isBrowsable() );

        List repoList = new ArrayList();
        ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();
        for ( Iterator repos = rcc.getRepos().iterator(); repos.hasNext(); )
        {
            RepoConfiguration repoConfig = (RepoConfiguration) repos.next();

            //skip local store repo
            if ( !repoConfig.getKey().equals( "global" ) )
            {
                ProxyRepository repo = new ProxyRepository( repoConfig.getKey(), repoConfig.getUrl(), layout );

                repoList.add( repo );
            }
        }

        this.setRepositories( repoList );
    }
}
