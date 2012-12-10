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

import org.apache.archiva.admin.model.beans.ArchivaRuntimeConfiguration;
import org.apache.archiva.rest.api.model.UserManagerImplementationInformation;
import org.apache.archiva.rest.api.services.ArchivaRuntimeConfigurationService;
import org.fest.assertions.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class ArchivaRuntimeConfigurationServiceTest
    extends AbstractArchivaRestTest
{
    @Test
    public void nonnullConfiguration()
        throws Exception
    {
        ArchivaRuntimeConfiguration archivaRuntimeConfiguration =
            getArchivaRuntimeConfigurationService().getArchivaRuntimeConfigurationAdmin();
        assertEquals( "jdo", archivaRuntimeConfiguration.getUserManagerImpls().get( 0 ) );
    }

    @Test
    public void update()
        throws Exception
    {
        ArchivaRuntimeConfiguration archivaRuntimeConfiguration =
            getArchivaRuntimeConfigurationService().getArchivaRuntimeConfigurationAdmin();
        assertEquals( "jdo", archivaRuntimeConfiguration.getUserManagerImpls().get( 0 ) );

        archivaRuntimeConfiguration.setUserManagerImpls( Arrays.asList( "foo" ) );

        getArchivaRuntimeConfigurationService().updateArchivaRuntimeConfiguration( archivaRuntimeConfiguration );

        archivaRuntimeConfiguration = getArchivaRuntimeConfigurationService().getArchivaRuntimeConfigurationAdmin();
        assertEquals( "foo", archivaRuntimeConfiguration.getUserManagerImpls().get( 0 ) );

    }


    @Test
    public void usermanagersinformations()
        throws Exception
    {
        ArchivaRuntimeConfigurationService service = getArchivaRuntimeConfigurationService();
        List<UserManagerImplementationInformation> infos = service.getUserManagerImplementationInformations();
        Assertions.assertThat( infos ).isNotNull().isNotEmpty().contains(
            new UserManagerImplementationInformation( "jdo", null, false ) );

    }

}
