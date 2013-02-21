package org.apache.archiva.redback.role;

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

import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelRole;
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.apache.archiva.redback.role.model.io.stax.RedbackRoleModelStaxReader;
import org.apache.archiva.redback.role.processor.RoleModelProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.role.template.RoleTemplateProcessor;
import org.apache.archiva.redback.role.util.RoleModelUtils;
import org.apache.archiva.redback.role.validator.RoleModelValidator;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RoleProfileManager:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@Service("roleManager")
public class DefaultRoleManager
    implements RoleManager
{
    private Logger log = LoggerFactory.getLogger( DefaultRoleManager.class );

    /**
     * the blessed model that has been validated as complete
     */
    private RedbackRoleModel blessedModel;

    /**
     * the merged model that can be validated as complete
     */
    private RedbackRoleModel unblessedModel;

    /**
     * a map of the resources, and the model that they loaded
     */
    private Map<String, ModelApplication> knownResources = new HashMap<String, ModelApplication>();

    @Inject
    @Named(value = "roleModelValidator")
    private RoleModelValidator modelValidator;

    @Inject
    @Named(value = "roleModelProcessor")
    private RoleModelProcessor modelProcessor;

    @Inject
    @Named(value = "roleTemplateProcessor")
    private RoleTemplateProcessor templateProcessor;

    @Inject
    @Named(value = "rbacManager#default")
    private RBACManager rbacManager;


    public void loadRoleModel( URL resource )
        throws RoleManagerException
    {
        RedbackRoleModelStaxReader reader = new RedbackRoleModelStaxReader();

        InputStreamReader inputStreamReader = null;

        try
        {

            inputStreamReader = new InputStreamReader( resource.openStream() );

            RedbackRoleModel roleModel = reader.read( inputStreamReader );

            for ( ModelApplication app : roleModel.getApplications() )
            {
                if ( !knownResources.containsKey( app.getId() ) )
                {
                    log.info( "loading {}", app.getId() );
                    loadApplication( app );
                }
            }
        }
        catch ( MalformedURLException e )
        {
            throw new RoleManagerException( "error locating redback profile", e );
        }
        catch ( IOException e )
        {
            throw new RoleManagerException( "error reading redback profile", e );
        }
        catch ( XMLStreamException e )
        {
            throw new RoleManagerException( "error parsing redback profile", e );
        }
        finally
        {
            IOUtils.closeQuietly( inputStreamReader );
        }
    }

    public void loadRoleModel( RedbackRoleModel roleModel )
        throws RoleManagerException
    {
        for ( ModelApplication app : roleModel.getApplications() )
        {
            if ( !knownResources.containsKey( app.getId() ) )
            {
                loadApplication( app );
            }
        }

    }

    public void loadApplication( ModelApplication app )
        throws RoleManagerException
    {
        if ( unblessedModel == null )
        {
            unblessedModel = new RedbackRoleModel();
        }

        unblessedModel.addApplication( app );

        if ( modelValidator.validate( unblessedModel ) )
        {
            blessedModel = unblessedModel;
        }
        else
        {
            StringBuilder stringBuilder = new StringBuilder( "Role Model Validation Errors:" );

            for ( String error : modelValidator.getValidationErrors() )
            {
                stringBuilder.append( error ).append( SystemUtils.LINE_SEPARATOR );
            }

            log.error( stringBuilder.toString() );

            throw new RoleManagerException(
                "Role Model Validation Error " + SystemUtils.LINE_SEPARATOR + stringBuilder.toString() );
        }

        modelProcessor.process( blessedModel );

        knownResources.put( app.getId(), app );
    }

    /**
     * create a role for the given roleName using the resource passed in for
     * resolving the ${resource} expression
     */
    public void createTemplatedRole( String templateId, String resource )
        throws RoleManagerException
    {
        templateProcessor.create( blessedModel, templateId, resource );
    }

    /**
     * remove the role corresponding to the role using the resource passed in for resolving the
     * ${resource} expression
     */
    public void removeTemplatedRole( String templateId, String resource )
        throws RoleManagerException
    {
        ModelTemplate template = RoleModelUtils.getModelTemplate( blessedModel, templateId );

        String roleName = template.getNamePrefix() + template.getDelimiter() + resource;

        try
        {
            Role role = rbacManager.getRole( roleName );

            for ( UserAssignment assignment : rbacManager.getUserAssignmentsForRoles(
                Arrays.asList( role.getName() ) ) )
            {
                assignment.removeRoleName( role );
                rbacManager.saveUserAssignment( assignment );
            }

        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "unable to remove role", e );
        }

        templateProcessor.remove( blessedModel, templateId, resource );
    }

    /**
     * update the role from templateId from oldResource to newResource
     * <p/>
     * NOTE: this requires removal and creation of the role since the jdo store does not tolerate renaming
     * because of the use of the name as an identifier
     */
    public void updateRole( String templateId, String oldResource, String newResource )
        throws RoleManagerException
    {
        // make the new role
        templateProcessor.create( blessedModel, templateId, newResource );

        ModelTemplate template = RoleModelUtils.getModelTemplate( blessedModel, templateId );

        String oldRoleName = template.getNamePrefix() + template.getDelimiter() + oldResource;
        String newRoleName = template.getNamePrefix() + template.getDelimiter() + newResource;

        try
        {
            Role role = rbacManager.getRole( oldRoleName );

            // remove the user assignments
            for ( UserAssignment assignment : rbacManager.getUserAssignmentsForRoles(
                Arrays.asList( role.getName() ) ) )
            {
                assignment.removeRoleName( oldRoleName );
                assignment.addRoleName( newRoleName );
                rbacManager.saveUserAssignment( assignment );
            }
        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "unable to update role", e );
        }

        templateProcessor.remove( blessedModel, templateId, oldResource );
    }

    public void assignRole( String roleId, String principal )
        throws RoleManagerException
    {
        ModelRole modelRole = RoleModelUtils.getModelRole( blessedModel, roleId );

        if ( modelRole == null )
        {
            throw new RoleManagerException( "Unable to assign role: " + roleId + " does not exist." );
        }

        try
        {
            UserAssignment userAssignment;

            if ( rbacManager.userAssignmentExists( principal ) )
            {
                userAssignment = rbacManager.getUserAssignment( principal );
            }
            else
            {
                userAssignment = rbacManager.createUserAssignment( principal );
            }

            userAssignment.addRoleName( modelRole.getName() );
            rbacManager.saveUserAssignment( userAssignment );
        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "Unable to assign role: unable to manage user assignment", e );
        }
    }

    public void assignRoleByName( String roleName, String principal )
        throws RoleManagerException
    {
        try
        {
            UserAssignment userAssignment;

            if ( rbacManager.userAssignmentExists( principal ) )
            {
                userAssignment = rbacManager.getUserAssignment( principal );
            }
            else
            {
                userAssignment = rbacManager.createUserAssignment( principal );
            }

            if ( !rbacManager.roleExists( roleName ) )
            {
                throw new RoleManagerException( "Unable to assign role: " + roleName + " does not exist." );
            }

            userAssignment.addRoleName( roleName );
            rbacManager.saveUserAssignment( userAssignment );
        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "Unable to assign role: unable to manage user assignment", e );
        }
    }

    public void assignTemplatedRole( String templateId, String resource, String principal )
        throws RoleManagerException
    {
        ModelTemplate modelTemplate = RoleModelUtils.getModelTemplate( blessedModel, templateId );

        if ( modelTemplate == null )
        {
            throw new RoleManagerException( "Unable to assign role: " + templateId + " does not exist." );
        }
        try
        {
            if ( !rbacManager.resourceExists( resource ) )
            {
                Resource newResource = rbacManager.createResource( resource );
                rbacManager.saveResource( newResource );
            }

            UserAssignment userAssignment;

            if ( rbacManager.userAssignmentExists( principal ) )
            {
                userAssignment = rbacManager.getUserAssignment( principal );
            }
            else
            {
                userAssignment = rbacManager.createUserAssignment( principal );
            }

            userAssignment.addRoleName( modelTemplate.getNamePrefix() + modelTemplate.getDelimiter() + resource );
            rbacManager.saveUserAssignment( userAssignment );
        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "Unable to assign role: unable to manage user assignment", e );
        }
    }

    public void unassignRole( String roleId, String principal )
        throws RoleManagerException
    {
        ModelRole modelRole = RoleModelUtils.getModelRole( blessedModel, roleId );

        if ( modelRole == null )
        {
            throw new RoleManagerException( "Unable to assign role: " + roleId + " does not exist." );
        }

        try
        {
            UserAssignment userAssignment;

            if ( rbacManager.userAssignmentExists( principal ) )
            {
                userAssignment = rbacManager.getUserAssignment( principal );
            }
            else
            {
                throw new RoleManagerException(
                    "UserAssignment for principal " + principal + "does not exist, can't unassign role." );
            }

            userAssignment.removeRoleName( modelRole.getName() );
            rbacManager.saveUserAssignment( userAssignment );
        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "Unable to unassign role: unable to manage user assignment", e );
        }
    }

    public void unassignRoleByName( String roleName, String principal )
        throws RoleManagerException
    {
        try
        {
            UserAssignment userAssignment;

            if ( rbacManager.userAssignmentExists( principal ) )
            {
                userAssignment = rbacManager.getUserAssignment( principal );
            }
            else
            {
                throw new RoleManagerException(
                    "UserAssignment for principal " + principal + "does not exist, can't unassign role." );
            }

            if ( !rbacManager.roleExists( roleName ) )
            {
                throw new RoleManagerException( "Unable to unassign role: " + roleName + " does not exist." );
            }

            userAssignment.removeRoleName( roleName );
            rbacManager.saveUserAssignment( userAssignment );
        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "Unable to unassign role: unable to manage user assignment", e );
        }
    }

    public boolean roleExists( String roleId )
        throws RoleManagerException
    {
        ModelRole modelRole = RoleModelUtils.getModelRole( blessedModel, roleId );

        if ( modelRole == null )
        {
            return false;
        }
        else
        {
            try
            {
                if ( rbacManager.roleExists( modelRole.getName() ) )
                {
                    return true;
                }
                else
                {
                    // perhaps try and reload the model here?
                    throw new RoleManagerException( "breakdown in role management, role '" + modelRole.getName()
                                                        + "' exists in configuration but was not created in underlying store" );
                }
            }
            catch ( RbacManagerException e )
            {
                throw new RoleManagerException( e.getMessage(), e );
            }
        }
    }

    public boolean templatedRoleExists( String templateId, String resource )
        throws RoleManagerException
    {
        ModelTemplate modelTemplate = RoleModelUtils.getModelTemplate( blessedModel, templateId );

        // template not existing is valid to check, it will throw exception on trying to create
        if ( modelTemplate == null )
        {
            return false;
        }
        else
        {
            try
            {
                if ( rbacManager.roleExists( modelTemplate.getNamePrefix() + modelTemplate.getDelimiter() + resource ) )
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            catch ( RbacManagerException e )
            {
                throw new RoleManagerException( e.getMessage(), e );
            }
        }
    }

    @PostConstruct
    public void initialize()
    {

        knownResources = new HashMap<String, ModelApplication>();
        this.unblessedModel = new RedbackRoleModel();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try
        {
            URL baseResource = RoleManager.class.getResource( "/META-INF/redback/redback-core.xml" );

            if ( baseResource == null )
            {
                throw new RuntimeException( "unable to initialize role manager, missing redback-core.xml" );
            }

            loadRoleModel( baseResource );

            Enumeration<URL> enumerator =
                RoleManager.class.getClassLoader().getResources( "META-INF/redback/redback.xml" );

            while ( enumerator.hasMoreElements() )
            {
                URL redbackResource = enumerator.nextElement();

                loadRoleModel( redbackResource );
            }
        }
        catch ( RoleManagerException e )
        {
            throw new RuntimeException( "unable to initialize RoleManager", e );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "unable to initialize RoleManager, problem with redback.xml loading", e );
        }

        stopWatch.stop();
        log.info( "DefaultRoleManager initialize time {}", stopWatch.getTime() );
    }

    public RedbackRoleModel getModel()
    {
        return blessedModel;
    }

    public void verifyTemplatedRole( String templateId, String resource )
        throws RoleManagerException
    {
        // create also serves as update
        templateProcessor.create( blessedModel, templateId, resource );
    }

    public RedbackRoleModel getBlessedModel()
    {
        return blessedModel;
    }

    public void setBlessedModel( RedbackRoleModel blessedModel )
    {
        this.blessedModel = blessedModel;
    }

    public RedbackRoleModel getUnblessedModel()
    {
        return unblessedModel;
    }

    public void setUnblessedModel( RedbackRoleModel unblessedModel )
    {
        this.unblessedModel = unblessedModel;
    }

    public Map<String, ModelApplication> getKnownResources()
    {
        return knownResources;
    }

    public void setKnownResources( Map<String, ModelApplication> knownResources )
    {
        this.knownResources = knownResources;
    }

    public RoleModelValidator getModelValidator()
    {
        return modelValidator;
    }

    public void setModelValidator( RoleModelValidator modelValidator )
    {
        this.modelValidator = modelValidator;
    }

    public RoleModelProcessor getModelProcessor()
    {
        return modelProcessor;
    }

    public void setModelProcessor( RoleModelProcessor modelProcessor )
    {
        this.modelProcessor = modelProcessor;
    }

    public RoleTemplateProcessor getTemplateProcessor()
    {
        return templateProcessor;
    }

    public void setTemplateProcessor( RoleTemplateProcessor templateProcessor )
    {
        this.templateProcessor = templateProcessor;
    }

    public RBACManager getRbacManager()
    {
        return rbacManager;
    }

    public void setRbacManager( RBACManager rbacManager )
    {
        this.rbacManager = rbacManager;
    }
}
