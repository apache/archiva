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

import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.OrganisationInformation;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.registry.RegistryException;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: ConfigurationAction.java 480950 2006-11-30 14:58:35Z evenisse $
 * 
 * @plexus.component role="com.opensymphony.xwork.Action"
 *                   role-hint="editOrganisationInfo"
 */
public class EditOrganisationInfoAction
    extends AbstractAppearanceAction
    implements SecureAction
{
    @Override
    public String execute()
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration config = configuration.getConfiguration();
        if (config != null)
        {
            OrganisationInformation orgInfo = config.getOrganisationInfo();
            if (orgInfo == null)
            {
                config.setOrganisationInfo(orgInfo);
            }
            
            orgInfo.setLogoLocation(getOrganisationLogo());
            orgInfo.setName(getOrganisationName());
            orgInfo.setUrl(getOrganisationUrl());
            
            configuration.save(config);
        }
        return SUCCESS;
    }
    
    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );
        return bundle;
    }
}
