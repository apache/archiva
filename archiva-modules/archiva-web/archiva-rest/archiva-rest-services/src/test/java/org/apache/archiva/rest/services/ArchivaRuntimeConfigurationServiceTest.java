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

import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;
import org.apache.archiva.rest.api.model.UserManagerImplementationInformation;
import org.apache.archiva.rest.api.services.RedbackRuntimeConfigurationService;
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
        RedbackRuntimeConfiguration redbackRuntimeConfiguration =
            getArchivaRuntimeConfigurationService().getRedbackRuntimeConfiguration();
        assertEquals( "jdo", redbackRuntimeConfiguration.getUserManagerImpls().get( 0 ) );
    }

    @Test
    public void update()
        throws Exception
    {
        RedbackRuntimeConfiguration redbackRuntimeConfiguration =
            getArchivaRuntimeConfigurationService().getRedbackRuntimeConfiguration();
        assertEquals( "jdo", redbackRuntimeConfiguration.getUserManagerImpls().get( 0 ) );

        redbackRuntimeConfiguration.setUserManagerImpls( Arrays.asList( "foo" ) );

        getArchivaRuntimeConfigurationService().updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );

        redbackRuntimeConfiguration = getArchivaRuntimeConfigurationService().getRedbackRuntimeConfiguration();
        assertEquals( "foo", redbackRuntimeConfiguration.getUserManagerImpls().get( 0 ) );

    }


    @Test
    public void usermanagersinformations()
        throws Exception
    {
        RedbackRuntimeConfigurationService service = getArchivaRuntimeConfigurationService();
        List<UserManagerImplementationInformation> infos = service.getUserManagerImplementationInformations();
        Assertions.assertThat( infos ).isNotNull().isNotEmpty().contains(
            new UserManagerImplementationInformation( "jdo", null, false ) );

    }

}
