package org.apache.maven.archiva.web.action.admin;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.codehaus.plexus.rbac.profile.RoleProfileException;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.security.rbac.RbacManagerException;

import java.io.IOException;

/**
 * SaveRepositoryAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="saveRepositoryAction" 
 */
public class SaveRepositoryAction
    extends AbstractRepositoryAction
{
    public void prepare()
        throws Exception
    {
        /* nothing to do here */
    }

    public String save()
    {
        String mode = getMode();
        String repoId = getRepository().getId();
        
        getLogger().info( "edit(" + mode + ":" + repoId + ")" );
        
        if ( StringUtils.isBlank( repository.getId() ) )
        {
            addFieldError( "id", "A repository with a blank id cannot be saved." );
            return SUCCESS;
        }

        if( StringUtils.equalsIgnoreCase( "edit", mode ) )
        {
            removeRepository( repoId );
        }

        try
        {
            addRepository( getRepository() );
            saveConfiguration();
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
        }
        catch ( RoleProfileException e )
        {
            addActionError( "Role Profile Exception: " + e.getMessage() );
        }
        catch ( InvalidConfigurationException e )
        {
            addActionError( "Invalid Configuration Exception: " + e.getMessage() );
        }
        catch ( RbacManagerException e )
        {
            addActionError( "RBAC Manager Exception: " + e.getMessage() );
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
        }
        
        return SUCCESS;
    }
}
