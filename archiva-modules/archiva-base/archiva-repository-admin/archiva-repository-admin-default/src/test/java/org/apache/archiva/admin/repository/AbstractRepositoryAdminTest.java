package org.apache.archiva.admin.repository;
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

import junit.framework.TestCase;
import org.apache.archiva.admin.mock.MockAuditListener;
import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.admin.model.proxyconnectorrule.ProxyConnectorRuleAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.memory.SimpleUser;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
@RunWith (ArchivaSpringJUnit4ClassRunner.class)
@ContextConfiguration (locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" })
public abstract class AbstractRepositoryAdminTest
    extends TestCase
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    public static final String APPSERVER_BASE_PATH =
        AbstractRepositoryAdminTest.fixPath( System.getProperty( "appserver.base" ) );

    @Inject
    protected MockAuditListener mockAuditListener;

    @Inject
    protected RoleManager roleManager;

    @Inject
    protected RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    protected ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    protected ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    protected ProxyConnectorRuleAdmin proxyConnectorRuleAdmin;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Before
    public void initialize() {
        Path confFile = Paths.get(APPSERVER_BASE_PATH, "conf/archiva.xml");
        try
        {
            Files.deleteIfExists( confFile );
            archivaConfiguration.reload();
        }
        catch ( IOException e )
        {
            // ignore
        }
    }

    protected AuditInformation getFakeAuditInformation()
    {
        AuditInformation auditInformation = new AuditInformation( getFakeUser(), "archiva-localhost" );
        return auditInformation;
    }

    // make a nice repo path to allow unit test to run
    private static String fixPath( String path )
    {
        String SPACE = " ";
        if ( path.contains( SPACE ) )
        {
            LoggerFactory.getLogger( AbstractRepositoryAdminTest.class.getName() ).error(
                "You are building and testing  with {appserver.base}: \n {}"
                    + " containing space. Consider relocating.", path );
        }
        return path.replaceAll( SPACE, "&amp;20" );
    }

    protected User getFakeUser()
    {
        SimpleUser user = new SimpleUser();

        user.setUsername( "root" );
        user.setFullName( "The top user" );
        return user;
    }

    protected ManagedRepository getTestManagedRepository( String repoId, String repoLocation )
    {
        String repoLocationStr = Paths.get(repoLocation, ".index").toString();
        return new ManagedRepository( Locale.getDefault( ), repoId, "test repo", repoLocation, "default", false, true, true, "0 0 * * * ?",
                                      repoLocationStr, false, 1, 2, true, false );
    }

    protected Path clearRepoLocation(String path )
        throws Exception
    {
        Path repoDir = Paths.get( path );
        if ( Files.exists(repoDir) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( repoDir );
        }
        assertFalse( Files.exists(repoDir) );
        return repoDir;
    }

    protected ManagedRepository findManagedRepoById( List<ManagedRepository> repos, String id )
    {
        for ( ManagedRepository repo : repos )
        {
            if ( StringUtils.equals( id, repo.getId() ) )
            {
                return repo;
            }
        }
        return null;
    }

    protected RemoteRepository getRemoteRepository()
    {
        return getRemoteRepository( "foo" );
    }

    protected RemoteRepository getRemoteRepository( String id )
    {
        RemoteRepository remoteRepository = new RemoteRepository(Locale.getDefault());
        remoteRepository.setUrl( "http://foo.com/maven-it-rocks" );
        remoteRepository.setTimeout( 10 );
        remoteRepository.setName( "maven foo" );
        remoteRepository.setUserName( "foo-name" );
        remoteRepository.setPassword( "toto" );
        remoteRepository.setId( id );
        remoteRepository.setRemoteDownloadNetworkProxyId( "foo" );
        remoteRepository.setDescription( "cool apache repo" );
        Map<String, String> extraParameters = new HashMap<>();
        extraParameters.put( "foo", "bar" );
        remoteRepository.setExtraParameters( extraParameters );
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put( "beer", "wine" );
        remoteRepository.setExtraHeaders( extraHeaders );
        return remoteRepository;
    }
}
