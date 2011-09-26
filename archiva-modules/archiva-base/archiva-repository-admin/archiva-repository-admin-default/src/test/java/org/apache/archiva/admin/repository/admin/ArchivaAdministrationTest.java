package org.apache.archiva.admin.repository.admin;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.FileType;
import org.apache.archiva.admin.model.beans.LegacyArtifactPath;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.archiva.admin.model.beans.UiConfiguration;
import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.apache.archiva.audit.AuditEvent;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * @author Olivier Lamy
 */
public class ArchivaAdministrationTest
    extends AbstractRepositoryAdminTest
{
    @Inject
    ArchivaAdministration archivaAdministration;


    @Test
    public void getAllLegacyPaths()
        throws Exception
    {
        assertNotNull( archivaAdministration.getLegacyArtifactPaths() );
        assertFalse( archivaAdministration.getLegacyArtifactPaths().isEmpty() );
        assertEquals( 1, archivaAdministration.getLegacyArtifactPaths().size() );
        log.info( "all legacy paths {}", archivaAdministration.getLegacyArtifactPaths() );
    }

    public void addAndDeleteLegacyPath()
        throws Exception
    {
        int initialSize = archivaAdministration.getLegacyArtifactPaths().size();

        LegacyArtifactPath legacyArtifactPath = new LegacyArtifactPath( "foo", "bar" );
        archivaAdministration.addLegacyArtifactPath( legacyArtifactPath, getFakeAuditInformation() );

        assertTrue( archivaAdministration.getLegacyArtifactPaths().contains( new LegacyArtifactPath( "foo", "bar" ) ) );
        assertEquals( initialSize + 1, archivaAdministration.getLegacyArtifactPaths().size() );

        archivaAdministration.deleteLegacyArtifactPath( legacyArtifactPath.getPath(), getFakeAuditInformation() );

        assertFalse(
            archivaAdministration.getLegacyArtifactPaths().contains( new LegacyArtifactPath( "foo", "bar" ) ) );
        assertEquals( initialSize, archivaAdministration.getLegacyArtifactPaths().size() );
        mockAuditListener.clearEvents();
    }

    @Test
    public void addAndUpdateAndDeleteFileType()
        throws RepositoryAdminException
    {
        int initialSize = archivaAdministration.getFileTypes().size();

        FileType fileType = new FileType();
        fileType.setId( "foo" );
        fileType.setPatterns( Arrays.asList( "bar", "toto" ) );

        archivaAdministration.addFileType( fileType, getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getFileTypes().size() );

        archivaAdministration.addFileTypePattern( "foo", "zorro", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getFileTypes().size() );

        assertEquals( 3, archivaAdministration.getFileType( "foo" ).getPatterns().size() );

        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "bar" ) );
        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "toto" ) );
        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "zorro" ) );

        archivaAdministration.removeFileTypePattern( "foo", "zorro", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getFileTypes().size() );

        assertEquals( 2, archivaAdministration.getFileType( "foo" ).getPatterns().size() );

        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "bar" ) );
        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "toto" ) );
        assertFalse( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "zorro" ) );

        archivaAdministration.removeFileType( "foo", getFakeAuditInformation() );

        assertEquals( initialSize, archivaAdministration.getFileTypes().size() );
        assertNull( archivaAdministration.getFileType( "foo" ) );
        mockAuditListener.clearEvents();
    }

    @Test
    public void knownContentConsumersTest()
        throws Exception
    {
        int initialSize = archivaAdministration.getKnownContentConsumers().size();

        archivaAdministration.addKnownContentConsumer( "foo", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getKnownContentConsumers().size() );
        assertTrue( archivaAdministration.getKnownContentConsumers().contains( "foo" ) );

        // ensure we don't add it twice as it's an ArrayList as storage
        archivaAdministration.addKnownContentConsumer( "foo", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getKnownContentConsumers().size() );
        assertTrue( archivaAdministration.getKnownContentConsumers().contains( "foo" ) );

        archivaAdministration.removeKnownContentConsumer( "foo", getFakeAuditInformation() );

        assertEquals( initialSize, archivaAdministration.getKnownContentConsumers().size() );
        assertFalse( archivaAdministration.getKnownContentConsumers().contains( "foo" ) );

        assertEquals( 2, mockAuditListener.getAuditEvents().size() );
        assertEquals( AuditEvent.ENABLE_REPO_CONSUMER, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( AuditEvent.DISABLE_REPO_CONSUMER, mockAuditListener.getAuditEvents().get( 1 ).getAction() );

        mockAuditListener.clearEvents();

    }

    @Test
    public void invalidContentConsumersTest()
        throws Exception
    {
        int initialSize = archivaAdministration.getInvalidContentConsumers().size();

        archivaAdministration.addInvalidContentConsumer( "foo", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getInvalidContentConsumers().size() );
        assertTrue( archivaAdministration.getInvalidContentConsumers().contains( "foo" ) );

        // ensure we don't add it twice as it's an ArrayList as storage
        archivaAdministration.addInvalidContentConsumer( "foo", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getInvalidContentConsumers().size() );
        assertTrue( archivaAdministration.getInvalidContentConsumers().contains( "foo" ) );

        archivaAdministration.removeInvalidContentConsumer( "foo", getFakeAuditInformation() );

        assertEquals( initialSize, archivaAdministration.getInvalidContentConsumers().size() );
        assertFalse( archivaAdministration.getInvalidContentConsumers().contains( "foo" ) );

        assertEquals( 2, mockAuditListener.getAuditEvents().size() );
        assertEquals( AuditEvent.ENABLE_REPO_CONSUMER, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( AuditEvent.DISABLE_REPO_CONSUMER, mockAuditListener.getAuditEvents().get( 1 ).getAction() );

        mockAuditListener.clearEvents();

    }

    @Test
    public void organisationInfoUpdate()
        throws Exception
    {
        OrganisationInformation organisationInformation = archivaAdministration.getOrganisationInformation();
        assertNotNull( organisationInformation );
        assertNull( organisationInformation.getLogoLocation() );
        assertNull( organisationInformation.getName() );
        assertNull( organisationInformation.getUrl() );

        organisationInformation = new OrganisationInformation();
        organisationInformation.setLogoLocation( "http://foo.com/bar.png" );
        organisationInformation.setName( "foo org" );
        organisationInformation.setUrl( "http://foo.com" );

        archivaAdministration.setOrganisationInformation( organisationInformation );

        organisationInformation = archivaAdministration.getOrganisationInformation();
        assertNotNull( organisationInformation );
        assertEquals( "http://foo.com/bar.png", organisationInformation.getLogoLocation() );
        assertEquals( "foo org", organisationInformation.getName() );
        assertEquals( "http://foo.com", organisationInformation.getUrl() );

    }

    @Test
    public void uiConfiguration()
        throws Exception
    {
        UiConfiguration ui = archivaAdministration.getUiConfiguration();
        assertNotNull( ui );
        // assert default values
        assertFalse( ui.isDisableEasterEggs() );
        assertTrue( ui.isAppletFindEnabled() );
        assertTrue( ui.isShowFindArtifacts() );

        ui.setAppletFindEnabled( false );
        ui.setShowFindArtifacts( false );
        ui.setDisableEasterEggs( true );

        archivaAdministration.updateUiConfiguration( ui );

        ui = archivaAdministration.getUiConfiguration();

        assertTrue( ui.isDisableEasterEggs() );
        assertFalse( ui.isAppletFindEnabled() );
        assertFalse( ui.isShowFindArtifacts() );
    }
}
