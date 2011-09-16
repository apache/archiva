package org.apache.maven.archiva.web.action.admin.repositories;

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

import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.RepositoryCommonValidator;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

import javax.inject.Inject;

/**
 * Abstract AdminRepositories Action base.
 * <p/>
 * Base class for all repository administrative functions.
 * This should be neutral to the type of action (add/edit/delete) and type of repo (managed/remote)
 *
 * @version $Id$
 */
public abstract class AbstractRepositoriesAdminAction
    extends AbstractActionSupport
    implements SecureAction, Auditable
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private RepositoryCommonValidator repositoryCommonValidator;


    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }



    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }

    public RepositoryCommonValidator getRepositoryCommonValidator()
    {
        return repositoryCommonValidator;
    }

    public void setRepositoryCommonValidator( RepositoryCommonValidator repositoryCommonValidator )
    {
        this.repositoryCommonValidator = repositoryCommonValidator;
    }
}
