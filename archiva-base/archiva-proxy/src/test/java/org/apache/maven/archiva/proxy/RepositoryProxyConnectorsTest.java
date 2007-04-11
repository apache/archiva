package org.apache.maven.archiva.proxy;

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusTestCase;
import org.easymock.MockControl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * RepositoryProxyConnectorsTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryProxyConnectorsTest
    extends PlexusTestCase
{
    private MockControl wagonMockControl;

    private Wagon wagonMock;

    private RepositoryProxyConnectors proxyHandler;

    private ArchivaRepository createRepository( String repoPath, String id, String name, String layout )
    {
        File repoDir = getTestFile( repoPath );
        String repoUrl = "file://" + StringUtils.replaceChars( repoDir.getAbsolutePath(), '\\', '/' );
        ArchivaRepository repo = new ArchivaRepository( id, name, repoUrl );
        repo.getModel().setLayoutName( layout );

        return repo;
    }

    private ArchivaRepository createManagedLegacyRepository()
    {
        return createRepository( "src/test/repositories/legacy-managed", "testManagedLegacyRepo",
                                 "Test Managed (Legacy) Repository", "legacy" );
    }
    
    private ArchivaRepository createProxiedLegacyRepository()
    {
        return createRepository( "src/test/repositories/legacy-proxied", "testProxiedLegacyRepo",
                                 "Test Proxied (Legacy) Repository", "legacy" );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        proxyHandler = (RepositoryProxyConnectors) lookup( RepositoryProxyConnectors.class.getName() );

        File repoLocation = getTestFile( "target/test-repository/managed" );
        // faster only to delete this one before copying, the others are done case by case
        FileUtils.deleteDirectory( new File( repoLocation, "org/apache/maven/test/get-merged-metadata" ) );
        copyDirectoryStructure( getTestFile( "src/test/repositories/managed" ), repoLocation );

        defaultLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        defaultManagedRepository = createRepository( "managed-repository", repoLocation );

        repoLocation = getTestFile( "target/test-repository/legacy-managed" );
        FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( getTestFile( "src/test/repositories/legacy-managed" ), repoLocation );

        ArtifactRepositoryLayout legacyLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                   "legacy" );

        legacyManagedRepository = createRepository( "managed-repository", repoLocation, legacyLayout );

        File location = getTestFile( "src/test/repositories/proxied1" );
        proxiedRepository1 = createRepository( "proxied1", location );

        location = getTestFile( "src/test/repositories/proxied2" );
        proxiedRepository2 = createRepository( "proxied2", location );

        proxiedRepositories = new ArrayList( 2 );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository1 ) );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );

        location = getTestFile( "src/test/repositories/legacy-proxied" );
        legacyProxiedRepository = createRepository( "legacy-proxied", location, legacyLayout );

        legacyProxiedRepositories = Collections.singletonList( createProxiedRepository( legacyProxiedRepository ) );

        wagonMockControl = MockControl.createNiceControl( Wagon.class );
        wagonMock = (Wagon) wagonMockControl.getMock();
        WagonDelegate delegate = (WagonDelegate) lookup( Wagon.ROLE, "test" );
        delegate.setDelegate( wagonMock );
    }
}
