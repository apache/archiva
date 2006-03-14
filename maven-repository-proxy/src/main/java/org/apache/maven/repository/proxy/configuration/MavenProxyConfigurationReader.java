package org.apache.maven.repository.proxy.configuration;

import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Edwin Punzalan
 */
public class MavenProxyConfigurationReader
{
    /**
     * Uses maven-proxy classes to read a maven-proxy properties configuration
     *
     * @param mavenProxyConfigurationFile The location of the maven-proxy configuration file
     * @throws ValidationException When a problem occured while processing the properties file
     * @throws java.io.IOException         When a problem occured while reading the property file
     */
    public ProxyConfiguration loadMavenProxyConfiguration( File mavenProxyConfigurationFile )
        throws ValidationException, IOException
    {
        ProxyConfiguration configuration = new ProxyConfiguration();

        MavenProxyPropertyLoader loader = new MavenProxyPropertyLoader();
        RetrievalComponentConfiguration rcc = loader.load( new FileInputStream( mavenProxyConfigurationFile ) );

        configuration.setRepositoryCachePath( rcc.getLocalStore() );

        List repoList = new ArrayList();
        for ( Iterator repos = rcc.getRepos().iterator(); repos.hasNext(); )
        {
            RepoConfiguration repoConfig = (RepoConfiguration) repos.next();

            //skip local store repo
            if ( !repoConfig.getKey().equals( "global" ) )
            {
                ProxyRepository repo = new ProxyRepository( repoConfig.getKey(), repoConfig.getUrl(), new DefaultRepositoryLayout() );
                repo.setCacheFailures( repoConfig.getCacheFailures() );
                repo.setCachePeriod( repoConfig.getCachePeriod() );
                repo.setHardfail( repoConfig.getHardFail() );

                if ( repoConfig instanceof HttpRepoConfiguration )
                {
                    HttpRepoConfiguration httpRepo = (HttpRepoConfiguration) repoConfig;
                    MavenProxyConfiguration httpProxy = httpRepo.getProxy();
                    //todo put into the configuration the proxy
                    if ( httpProxy != null )
                    {
                        repo.setProxied( true );
                    }
                }

                repoList.add( repo );
            }
        }

        configuration.setRepositories( repoList );

        return configuration;
    }
}
