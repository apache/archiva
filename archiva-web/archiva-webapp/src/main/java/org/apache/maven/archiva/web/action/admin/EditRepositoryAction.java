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

import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.admin.models.AdminRepositoryConfiguration;

/**
 * EditRepositoryAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="editRepositoryAction"
 */
public class EditRepositoryAction
    extends AbstractRepositoryAction
{
    public String edit()
    {
        getLogger().info( ".edit()" );

        if ( operationAllowed( ArchivaRoleConstants.OPERATION_EDIT_REPOSITORY, getRepoid() ) )
        {
            addActionError( "You do not have the appropriate permissions to edit the " + getRepoid() + " repository." );
            return ERROR;
        }

        return INPUT;
    }

    public void prepare()
        throws Exception
    {
        String id = getRepoid();
        if ( id == null )
        {
            // TODO: Throw error?
            return;
        }

        RepositoryConfiguration repoconfig = archivaConfiguration.getConfiguration().findRepositoryById( id );
        if ( repoconfig != null )
        {
            this.repository = new AdminRepositoryConfiguration( repoconfig );
        }
    }

    public String getMode()
    {
        return "edit";
    }
}
