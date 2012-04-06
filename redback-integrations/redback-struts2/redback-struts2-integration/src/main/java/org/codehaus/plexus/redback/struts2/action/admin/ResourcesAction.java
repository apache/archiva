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

import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.action.AbstractSecurityAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.role.RoleConstants;
import org.codehaus.redback.integration.util.ResourceSorter;
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
@Controller( "redback-resources" )
@Scope( "prototype" )
public class ResourcesAction
    extends AbstractSecurityAction
{
    private static final String LIST = "list";

    /**
     *  role-hint="cached"
     */
    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager manager;

    private String resourceIdentifier;

    private boolean isPattern;

    private List<Resource> allResources;

    public String list()
    {
        try
        {
            allResources = manager.getAllResources();

            if ( allResources == null )
            {
                allResources = Collections.emptyList();
            }

            Collections.sort( allResources, new ResourceSorter() );
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.list.all.resources", Arrays.asList( (Object) e.getMessage() ) ) );
            log.error( "System error:", e );
            allResources = Collections.emptyList();
        }

        return LIST;
    }

    public String save()
    {
        try
        {
            Resource temp = manager.createResource( resourceIdentifier );

            temp.setIdentifier( resourceIdentifier );
            temp.setPattern( isPattern );

            manager.saveResource( temp );
        }
        catch ( RbacManagerException e )
        {
            addActionError( getText( "cannot.save.resource", Arrays.asList( (Object) e.getMessage() ) ) );
            log.error( "System error:", e );
            allResources = Collections.emptyList();
        }

        return LIST;
    }

    public String remove()
    {
        try
        {
            manager.removeResource( manager.getResource( resourceIdentifier ) );
        }
        catch ( RbacManagerException ne )
        {
            addActionError( getText( "cannot.remove.resource", Arrays.asList( (Object) resourceIdentifier ) ) );
            return ERROR;
        }
        return LIST;
    }

    public List<Resource> getAllResources()
    {
        return allResources;
    }

    public void setAllResources( List<Resource> allResources )
    {
        this.allResources = allResources;
    }

    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    public void setResourceIdentifier( String resourceIdentifier )
    {
        this.resourceIdentifier = resourceIdentifier;
    }

    public boolean isPattern()
    {
        return isPattern;
    }

    public void setPattern( boolean isPattern )
    {
        this.isPattern = isPattern;
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
