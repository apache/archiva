package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.beans.FileType;
import org.apache.archiva.admin.model.beans.LegacyArtifactPath;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Olivier Lamy
 */
public class ArchivaAdministrationServiceTest
    extends AbstractArchivaRestTest
{
    @Test
    public void getAllLegacyPaths()
        throws Exception
    {
        assertNotNull( getArchivaAdministrationService().getLegacyArtifactPaths() );
        assertFalse( getArchivaAdministrationService().getLegacyArtifactPaths().isEmpty() );
    }

    @Test
    public void addAndDeleteLegacyPath()
        throws Exception
    {
        int initialSize = getArchivaAdministrationService().getLegacyArtifactPaths().size();
        LegacyArtifactPath legacyArtifactPath = new LegacyArtifactPath();
        legacyArtifactPath.setArtifact( "foo" );
        legacyArtifactPath.setPath( "bar" );
        getArchivaAdministrationService().addLegacyArtifactPath( legacyArtifactPath );
        assertEquals( initialSize + 1, getArchivaAdministrationService().getLegacyArtifactPaths().size() );

        getArchivaAdministrationService().deleteLegacyArtifactPath( "bar" );
        assertEquals( initialSize, getArchivaAdministrationService().getLegacyArtifactPaths().size() );
    }

    @Test
    public void addAndDeleteFileType()
        throws Exception
    {
        int initialSize = getArchivaAdministrationService().getFileTypes().size();
        FileType fileType = new FileType();
        fileType.setId( "footwo" );
        fileType.setPatterns( Arrays.asList( "foo", "bar" ) );
        getArchivaAdministrationService().addFileType( fileType );
        assertEquals( initialSize + 1, getArchivaAdministrationService().getFileTypes().size() );

        assertNotNull( getArchivaAdministrationService().getFileType( "footwo" ) );
        assertEquals( Arrays.asList( "foo", "bar" ),
                      getArchivaAdministrationService().getFileType( "footwo" ).getPatterns() );

        getArchivaAdministrationService().removeFileType( "footwo" );

        assertEquals( initialSize, getArchivaAdministrationService().getFileTypes().size() );

        assertNull( getArchivaAdministrationService().getFileType( "footwo" ) );
    }

    @Test
    public void organisationInformationUpdate()
        throws Exception
    {
        OrganisationInformation organisationInformation =
            getArchivaAdministrationService().getOrganisationInformation();

        // rest return an empty bean
        assertNotNull( organisationInformation );
        assertTrue( StringUtils.isBlank( organisationInformation.getLogoLocation() ) );
        assertTrue( StringUtils.isBlank( organisationInformation.getName() ) );
        assertTrue( StringUtils.isBlank( organisationInformation.getUrl() ) );

        organisationInformation = new OrganisationInformation();
        organisationInformation.setLogoLocation( "http://foo.com/bar.png" );
        organisationInformation.setName( "foo org" );
        organisationInformation.setUrl( "http://foo.com" );

        getArchivaAdministrationService().setOrganisationInformation( organisationInformation );

        organisationInformation = getArchivaAdministrationService().getOrganisationInformation();
        assertNotNull( organisationInformation );
        assertEquals( "http://foo.com/bar.png", organisationInformation.getLogoLocation() );
        assertEquals( "foo org", organisationInformation.getName() );
        assertEquals( "http://foo.com", organisationInformation.getUrl() );
    }
}
