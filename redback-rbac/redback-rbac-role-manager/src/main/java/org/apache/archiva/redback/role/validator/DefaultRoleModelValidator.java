package org.apache.archiva.redback.role.validator;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelOperation;
import org.apache.archiva.redback.role.model.ModelPermission;
import org.apache.archiva.redback.role.model.ModelResource;
import org.apache.archiva.redback.role.model.ModelRole;
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.apache.archiva.redback.role.util.RoleModelUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * DefaultRoleModelValidator: validates completeness of the model
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
@Service( "roleModelValidator" )
public class DefaultRoleModelValidator
    implements RoleModelValidator
{
    private List<String> validationErrors;

    public boolean validate( RedbackRoleModel model )
        throws RoleManagerException
    {
        validationErrors = null;

        validateRequiredStructure( model );
        validateResourceClosure( model );
        validateOperationClosure( model );
        validateChildRoleClosure( model );
        validateParentRoleClosure( model );
        validateTemplateClosure( model );
        validateNoRoleCycles( model );
        validateNoTemplateCycles( model );

        if ( validationErrors == null )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public List<String> getValidationErrors()
    {
        return validationErrors;
    }

    private void addValidationError( String error )
    {
        if ( validationErrors == null )
        {
            validationErrors = new ArrayList<String>( 0 );
        }

        validationErrors.add( error );
    }

    /**
     * FIXME this should be taken care of by <required/> in modello, figure out why its not
     * in the meantime, implement the basics
     *
     * @param model
     */
    @SuppressWarnings( "unchecked" )
    private void validateRequiredStructure( RedbackRoleModel model )
    {
        // validate model has name

        for ( ModelApplication application : model.getApplications() )
        {
            if ( application.getId() == null )
            {
                addValidationError( "model is missing application name" );
            }

            // validate model has version
            if ( application.getVersion() == null )
            {
                addValidationError( application.getId() + " is missing version" );
            }

            // validate resource bits
            for ( ModelResource resource : application.getResources() )
            {
                if ( resource.getName() == null )
                {
                    addValidationError( resource.toString() + " missing name" );
                }

                if ( resource.getId() == null )
                {
                    addValidationError( resource.toString() + " missing id" );
                }
            }

            // validate the operations
            for ( ModelOperation operation : application.getOperations() )
            {
                if ( operation.getName() == null )
                {
                    addValidationError( operation.toString() + " missing name" );
                }

                if ( operation.getId() == null )
                {
                    addValidationError( operation.toString() + " missing id" );
                }
            }

            for ( ModelRole role : application.getRoles() )
            {
                if ( role.getId() == null )
                {
                    addValidationError( role.toString() + " missing id" );
                }

                if ( role.getName() == null )
                {
                    addValidationError( role.toString() + " missing name" );
                }

                if ( role.getPermissions() != null )
                {
                    for ( ModelPermission permission : role.getPermissions() )
                    {
                        if ( permission.getName() == null )
                        {
                            addValidationError( permission.toString() + " missing name" );
                        }

                        if ( permission.getId() == null )
                        {
                            addValidationError( permission.toString() + " missing id" );
                        }

                        if ( permission.getOperation() == null )
                        {
                            addValidationError( permission.toString() + " missing operations" );
                        }

                        if ( permission.getResource() == null )
                        {
                            addValidationError( permission.toString() + " missing resource" );
                        }
                    }
                }
            }

            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( template.getId() == null )
                {
                    addValidationError( template.toString() + " missing id" );
                }

                if ( template.getNamePrefix() == null )
                {
                    addValidationError( template.toString() + " missing name prefix" );
                }

                if ( template.getPermissions() != null )
                {
                    for ( ModelPermission permission : template.getPermissions() )
                    {
                        if ( permission.getName() == null )
                        {
                            addValidationError( permission.toString() + " missing name" );
                        }

                        if ( permission.getId() == null )
                        {
                            addValidationError( permission.toString() + " missing id" );
                        }

                        if ( permission.getOperation() == null )
                        {
                            addValidationError( permission.toString() + " missing operations" );
                        }

                        if ( permission.getResource() == null )
                        {
                            addValidationError( permission.toString() + " missing resource" );
                        }
                    }
                }
            }
        }
    }

    /**
     * validate all operations in all declared permissions exist as declared in the operations section
     *
     * @param model
     */
    private void validateOperationClosure( RedbackRoleModel model )
    {
        List<String> operationIdList = RoleModelUtils.getOperationIdList( model );

        // check the operations in role permissions
        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                if ( role.getPermissions() != null )
                {
                    for ( ModelPermission permission : role.getPermissions() )
                    {
                        if ( !operationIdList.contains( permission.getOperation() ) )
                        {
                            addValidationError( "missing operation: " + permission.getOperation() + " in permission "
                                                    + permission.getId() );
                        }
                    }
                }
            }

            // check the operations in template permissions
            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( template.getPermissions() != null )
                {
                    for ( ModelPermission permission : template.getPermissions() )
                    {
                        if ( !operationIdList.contains( permission.getOperation() ) )
                        {
                            addValidationError( "missing operation: " + permission.getOperation() + " in permission "
                                                    + permission.getId() );
                        }
                    }
                }
            }
        }
    }

    private void validateResourceClosure( RedbackRoleModel model )
    {
        List<String> resourceIdList = RoleModelUtils.getResourceIdList( model );
        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                if ( role.getPermissions() != null )
                {
                    for ( ModelPermission permission : role.getPermissions() )
                    {
                        if ( !resourceIdList.contains( permission.getResource() ) )
                        {
                            addValidationError( "missing operation: " + permission.getResource() + " in permission "
                                                    + permission.getId() );
                        }
                    }
                }
            }
        }
    }

    private void validateChildRoleClosure( RedbackRoleModel model )
    {
        List<String> roleIdList = RoleModelUtils.getRoleIdList( model );
        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                if ( role.getChildRoles() != null )
                {
                    for ( String childRoleId : role.getChildRoles() )
                    {
                        if ( !roleIdList.contains( childRoleId ) )
                        {
                            addValidationError(
                                "missing role id: " + childRoleId + " in child roles of role " + role.getId() );
                        }
                    }
                }
            }

            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( template.getChildRoles() != null )
                {
                    for ( String childRoleId : template.getChildRoles() )
                    {
                        if ( !roleIdList.contains( childRoleId ) )
                        {
                            addValidationError(
                                "missing role id: " + childRoleId + " in child roles of template " + template.getId() );
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void validateParentRoleClosure( RedbackRoleModel model )
    {
        List roleIdList = RoleModelUtils.getRoleIdList( model );

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                if ( role.getParentRoles() != null )
                {
                    for ( String parentRoleId : role.getParentRoles() )
                    {
                        if ( !roleIdList.contains( parentRoleId ) )
                        {
                            addValidationError(
                                "missing role id: " + parentRoleId + " in parent roles of role " + role.getId() );
                        }
                    }
                }
            }

            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( template.getParentRoles() != null )
                {
                    for ( String parentRoleId : template.getParentRoles() )
                    {
                        if ( !roleIdList.contains( parentRoleId ) )
                        {
                            addValidationError( "missing role id: " + parentRoleId + " in parent roles of template "
                                                    + template.getId() );
                        }
                    }
                }
            }
        }
    }

    private void validateTemplateClosure( RedbackRoleModel model )
    {
        List templateIdList = RoleModelUtils.getTemplateIdList( model );

        // template name prefix must be unique
        List<String> templateNamePrefixList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( template.getParentTemplates() != null )
                {
                    for ( String parentTemplateId : template.getParentTemplates() )
                    {
                        if ( !templateIdList.contains( parentTemplateId ) )
                        {
                            addValidationError(
                                "missing template id: " + parentTemplateId + " in parent templates of template "
                                    + template.getId() );
                        }
                    }
                }

                if ( template.getChildTemplates() != null )
                {
                    for ( String childTemplateId : template.getChildTemplates() )
                    {
                        if ( !templateIdList.contains( childTemplateId ) )
                        {
                            addValidationError(
                                "missing template id: " + childTemplateId + " in child templates of template "
                                    + template.getId() );
                        }
                    }
                }

                if ( !templateNamePrefixList.contains( template.getNamePrefix() ) )
                {
                    templateNamePrefixList.add( template.getNamePrefix() );
                }
                else
                {
                    addValidationError( "duplicate name prefix detected: " + template.getNamePrefix() );
                }
            }
        }
    }

    /**
     * We are not allowed to have cycles between roles, this method is to detect and raise a red flag when that happens.
     *
     * @param model
     */
    private void validateNoRoleCycles( RedbackRoleModel model )
    {
        try
        {
            RoleModelUtils.generateRoleGraph( model );
        }
        catch ( CycleDetectedException e )
        {
            addValidationError( "cycle detected: " + e.getMessage() );
        }
    }

    /**
     * We are not allowed to have cycles between template either, this method is to detect and
     * raise a red flag when that happens.  Templates are a bit more complex since they have both
     * child and parent roles, as well as runtime parent and child templates
     * <p/>
     * the id should be sufficient to test cycles here even though in runtime the id's do not need to be
     * unique since it is the binding of a namePrefix and a resource that makes them unique
     *
     * @param model
     */
    private void validateNoTemplateCycles( RedbackRoleModel model )
    {
        try
        {
            RoleModelUtils.generateTemplateGraph( model );
        }
        catch ( CycleDetectedException e )
        {
            addValidationError( "template cycle detected: " + e.getMessage() );
        }
    }
}
