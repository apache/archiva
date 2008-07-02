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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.OrganisationInformation;

/**
 * Stores the organisation information for displaying on the page.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="organisationInfo"
 */
public class OrganisationInfoAction
    extends AbstractAppearanceAction
{
    @Override
    public String execute()
        throws Exception
    {        
        Configuration config = configuration.getConfiguration();
        if (config != null)
        {
            OrganisationInformation orgInfo = config.getOrganisationInfo();
            if (orgInfo != null)
            {
                setOrganisationLogo(orgInfo.getLogoLocation());
                setOrganisationName(orgInfo.getName());
                setOrganisationUrl(orgInfo.getUrl());
            }
        }
        return SUCCESS;
    }
}