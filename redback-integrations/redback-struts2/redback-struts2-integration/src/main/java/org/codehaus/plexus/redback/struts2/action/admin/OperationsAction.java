package org.codehaus.plexus.redback.struts2.action.admin;

/*
 * Copyright 2005-2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.redback.rbac.Operation;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.action.RedbackActionSupport;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.role.RoleConstants;
import org.codehaus.redback.integration.util.OperationSorter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * OperationsAction:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @version $Id$
 */
@Controller( "redback-operations" )
@Scope( "prototype" )
public class OperationsAction
    extends RedbackActionSupport
{
    private static final String LIST = "list";

    /**
     *  role-hint="cached"
     */
    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager manager;

    private String operationName;

    private String description;

    private List<Operation> allOperations;

    public String list()
    {
        try
        {
            allOperations = manager.getAllOperations();

            if ( allOperations == null )
            {
                allOperations = Collections.emptyList();
            }

            Collections.sort( allOperations, new OperationSorter() );
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.list.all.operations", Arrays.asList( (Object) e.getMessage() ) ) );
            log.error( "System error:", e );
            allOperations = Collections.emptyList();
        }

        return LIST;
    }

    public String save()
    {
        try
        {
            Operation temp = manager.createOperation( operationName );

            temp.setDescription( description );

            manager.saveOperation( temp );
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.save.operation", Arrays.asList( (Object) operationName ) ) );
            log.error( "System error:", e );
            allOperations = Collections.emptyList();
        }

        return LIST;
    }

    public String remove()
    {
        try
        {
            manager.removeOperation( manager.getOperation( operationName ) );
        }
        catch ( RbacManagerException ne )
        {
            addActionError( getText( "cannot.remove.operation", Arrays.asList( (Object) operationName ) ) );
            return ERROR;
        }
        return LIST;
    }

    public List<Operation> getAllOperations()
    {
        return allOperations;
    }

    public void setAllOperations( List<Operation> allOperations )
    {
        this.allOperations = allOperations;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getOperationName()
    {
        return operationName;
    }

    public void setOperationName( String operationName )
    {
        this.operationName = operationName;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION, Resource.GLOBAL );
        return bundle;
    }
}
