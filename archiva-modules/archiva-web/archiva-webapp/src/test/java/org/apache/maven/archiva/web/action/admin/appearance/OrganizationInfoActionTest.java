package org.apache.maven.archiva.web.action.admin.appearance;

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

import org.apache.maven.archiva.configuration.OrganisationInformation;

/**
 */
public class OrganizationInfoActionTest
    extends AbstractOrganizationInfoActionTest
{
    public void testOrganisationInfoLoads()
        throws Exception
    {
        config.setOrganisationInfo( new OrganisationInformation() );
        OrganisationInformation orginfo = config.getOrganisationInfo();
        orginfo.setLogoLocation( "LOGO" );
        orginfo.setName( "NAME" );
        orginfo.setUrl( "URL" );

        configuration.save( config );

        reloadAction();

        action.prepare();

        assertEquals( "URL", action.getOrganisationUrl() );
        assertEquals( "NAME", action.getOrganisationName() );
        assertEquals( "LOGO", action.getOrganisationLogo() );
    }

    @Override
    protected AbstractAppearanceAction getAction()
    {
        //return (OrganisationInfoAction) lookup( Action.class.getName(), "organisationInfo" );

        return (OrganisationInfoAction) getActionProxy( "/admin/organisationInfo" ).getAction();
    }
}
