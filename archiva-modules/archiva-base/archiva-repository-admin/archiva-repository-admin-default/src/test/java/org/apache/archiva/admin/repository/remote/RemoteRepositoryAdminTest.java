package org.apache.archiva.admin.repository.remote;
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

import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.junit.Test;

import java.util.List;

/**
 * @author Olivier Lamy
 */
public class RemoteRepositoryAdminTest
    extends AbstractRepositoryAdminTest
{

    @Test
    public void getAll()
        throws Exception
    {
        List<RemoteRepository> remoteRepositories = remoteRepositoryAdmin.getRemoteRepositories();
        assertNotNull( remoteRepositories );
        assertTrue( remoteRepositories.size() > 0 );
        log.info( "remote {}", remoteRepositories );
    }

    @Test
    public void getById()
        throws Exception
    {
        RemoteRepository central = remoteRepositoryAdmin.getRemoteRepository( "central" );
        assertNotNull( central );
        assertEquals( "https://repo.maven.apache.org/maven2", central.getUrl() );
        assertEquals( 60, central.getTimeout() );
        assertNull( central.getUserName() );
        assertNull( central.getPassword() );
    }

    @Test
    public void addAndDelete()
        throws Exception
    {
        mockAuditListener.clearEvents();
        int initialSize = remoteRepositoryAdmin.getRemoteRepositories().size();

        RemoteRepository remoteRepository = getRemoteRepository();

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, getFakeAuditInformation() );

        assertEquals( initialSize + 1, remoteRepositoryAdmin.getRemoteRepositories().size() );

        RemoteRepository repo = remoteRepositoryAdmin.getRemoteRepository( "foo" );
        assertNotNull( repo );
        assertEquals( getRemoteRepository().getPassword(), repo.getPassword() );
        assertEquals( getRemoteRepository().getUrl(), repo.getUrl() );
        assertEquals( getRemoteRepository().getUserName(), repo.getUserName() );
        assertEquals( getRemoteRepository().getName(), repo.getName() );
        assertEquals( getRemoteRepository().getTimeout(), repo.getTimeout() );
        assertEquals( getRemoteRepository().getDescription(), repo.getDescription() );
        assertEquals( 1, remoteRepository.getExtraHeaders().size() );
        assertEquals( "wine", remoteRepository.getExtraHeaders().get( "beer" ) );

        assertEquals( 1, remoteRepository.getExtraParameters().size() );
        assertEquals( "bar", remoteRepository.getExtraParameters().get( "foo" ) );

        remoteRepositoryAdmin.deleteRemoteRepository( "foo", getFakeAuditInformation() );

        assertEquals( initialSize, remoteRepositoryAdmin.getRemoteRepositories().size() );

        repo = remoteRepositoryAdmin.getRemoteRepository( "foo" );
        assertNull( repo );

        assertEquals( 2, mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_REMOTE_REPO, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 0 ).getUserId() );
        assertEquals( "archiva-localhost", mockAuditListener.getAuditEvents().get( 0 ).getRemoteIP() );

        assertEquals( AuditEvent.DELETE_REMOTE_REPO, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 1 ).getUserId() );

    }


    @Test
    public void addAndUpdateAndDelete()
        throws Exception
    {
        mockAuditListener.clearEvents();
        int initialSize = remoteRepositoryAdmin.getRemoteRepositories().size();

        RemoteRepository remoteRepository = getRemoteRepository();

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, getFakeAuditInformation() );

        assertEquals( initialSize + 1, remoteRepositoryAdmin.getRemoteRepositories().size() );

        RemoteRepository repo = remoteRepositoryAdmin.getRemoteRepository( "foo" );
        assertNotNull( repo );
        assertEquals( getRemoteRepository().getPassword(), repo.getPassword() );
        assertEquals( getRemoteRepository().getUrl(), repo.getUrl() );
        assertEquals( getRemoteRepository().getUserName(), repo.getUserName() );
        assertEquals( getRemoteRepository().getName(), repo.getName() );
        assertEquals( getRemoteRepository().getTimeout(), repo.getTimeout() );
        assertEquals( getRemoteRepository().getRemoteDownloadNetworkProxyId(), repo.getRemoteDownloadNetworkProxyId() );

        repo.setUserName( "foo-name-changed" );
        repo.setPassword( "titi" );
        repo.setUrl( "http://foo.com/maven-really-rocks" );
        repo.setRemoteDownloadNetworkProxyId( "toto" );
        repo.setDescription( "archiva rocks!" );

        remoteRepositoryAdmin.updateRemoteRepository( repo, getFakeAuditInformation() );

        repo = remoteRepositoryAdmin.getRemoteRepository( "foo" );

        assertEquals( "foo-name-changed", repo.getUserName() );
        assertEquals( "titi", repo.getPassword() );
        assertEquals( "http://foo.com/maven-really-rocks", repo.getUrl() );
        assertEquals( "toto", repo.getRemoteDownloadNetworkProxyId() );
        assertEquals( "archiva rocks!", repo.getDescription() );

        remoteRepositoryAdmin.deleteRemoteRepository( "foo", getFakeAuditInformation() );

        assertEquals( initialSize, remoteRepositoryAdmin.getRemoteRepositories().size() );

        repo = remoteRepositoryAdmin.getRemoteRepository( "foo" );
        assertNull( repo );

        assertEquals( 3, mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_REMOTE_REPO, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 0 ).getUserId() );
        assertEquals( "archiva-localhost", mockAuditListener.getAuditEvents().get( 0 ).getRemoteIP() );

        assertEquals( AuditEvent.MODIFY_REMOTE_REPO, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 1 ).getUserId() );
        assertEquals( "archiva-localhost", mockAuditListener.getAuditEvents().get( 1 ).getRemoteIP() );

        assertEquals( AuditEvent.DELETE_REMOTE_REPO, mockAuditListener.getAuditEvents().get( 2 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 2 ).getUserId() );

    }


}
