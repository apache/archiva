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

import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.apache.archiva.admin.repository.RepositoryAdminException;
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
    }

    @Test
    public void addAndUpdateAndDeleteFileType()
        throws RepositoryAdminException
    {
        int initialSize = archivaAdministration.getRepositoryScanning().getFileTypes().size();

        FileType fileType = new FileType();
        fileType.setId( "foo" );
        fileType.setPatterns( Arrays.asList( "bar", "toto" ) );

        archivaAdministration.addFileType( fileType, getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getRepositoryScanning().getFileTypes().size() );

        archivaAdministration.addFileTypePattern( "foo", "zorro", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getRepositoryScanning().getFileTypes().size() );

        assertEquals( 3, archivaAdministration.getFileType( "foo" ).getPatterns().size() );

        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "bar" ) );
        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "toto" ) );
        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "zorro" ) );

        archivaAdministration.removeFileTypePattern( "foo", "zorro", getFakeAuditInformation() );

        assertEquals( initialSize + 1, archivaAdministration.getRepositoryScanning().getFileTypes().size() );

        assertEquals( 2, archivaAdministration.getFileType( "foo" ).getPatterns().size() );

        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "bar" ) );
        assertTrue( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "toto" ) );
        assertFalse( archivaAdministration.getFileType( "foo" ).getPatterns().contains( "zorro" ) );

        archivaAdministration.removeFileType( "foo", getFakeAuditInformation() );

        assertEquals( initialSize, archivaAdministration.getRepositoryScanning().getFileTypes().size() );
        assertNull( archivaAdministration.getFileType( "foo" ) );
    }
}
