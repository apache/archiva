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

import com.opensymphony.xwork2.Validateable;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.OrganisationInformation;
import org.apache.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * @version $Id: ConfigurationAction.java 480950 2006-11-30 14:58:35Z evenisse $
 * plexus.component role="com.opensymphony.xwork2.Action"
 * role-hint="editOrganisationInfo"
 * instantiation-strategy="per-lookup"
 */
@Controller( "editOrganisationInfo" )
@Scope( "prototype" )
public class EditOrganisationInfoAction
    extends AbstractAppearanceAction
    implements SecureAction, Validateable
{
    @Override
    public String execute()
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration config = configuration.getConfiguration();
        if ( config != null )
        {
            OrganisationInformation orgInfo = config.getOrganisationInfo();
            if ( orgInfo == null )
            {
                config.setOrganisationInfo( orgInfo );
            }

            orgInfo.setLogoLocation( getOrganisationLogo() );
            orgInfo.setName( getOrganisationName() );
            orgInfo.setUrl( getOrganisationUrl() );

            configuration.save( config );
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

    public void validate()
    {
        // trim all unecessary trailing/leading white-spaces; always put this statement before the closing braces(after all validation).
        trimAllRequestParameterValues();
    }

    private void trimAllRequestParameterValues()
    {
        if ( StringUtils.isNotEmpty( super.getOrganisationName() ) )
        {
            super.setOrganisationName( super.getOrganisationName().trim() );
        }

        if ( StringUtils.isNotEmpty( super.getOrganisationUrl() ) )
        {
            super.setOrganisationUrl( super.getOrganisationUrl().trim() );
        }

        if ( StringUtils.isNotEmpty( super.getOrganisationLogo() ) )
        {
            super.setOrganisationLogo( super.getOrganisationLogo().trim() );
        }
    }
}
