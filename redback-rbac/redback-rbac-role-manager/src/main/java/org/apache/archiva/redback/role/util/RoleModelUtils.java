package org.apache.archiva.redback.role.util;

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

import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelOperation;
import org.apache.archiva.redback.role.model.ModelResource;
import org.apache.archiva.redback.role.model.ModelRole;
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * RoleModelUtils:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
public class RoleModelUtils
{


    public static List<ModelRole> getRoles( RedbackRoleModel model )
    {
        List<ModelRole> roleList = new ArrayList<ModelRole>( );

        for ( ModelApplication application : model.getApplications() )
        {
            roleList.addAll( application.getRoles() );
        }

        return roleList;
    }

    public static List<ModelTemplate> getTemplates( RedbackRoleModel model )
    {
        List<ModelTemplate> templateList = new ArrayList<ModelTemplate>();

        for ( ModelApplication application : model.getApplications() )
        {
            templateList.addAll( application.getTemplates() );
        }

        return templateList;
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> getOperationIdList( RedbackRoleModel model )
    {
        List<String> operationsIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelOperation operation : application.getOperations() )
            {
                operationsIdList.add( operation.getId() );
            }
        }

        return operationsIdList;
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> getResourceIdList( RedbackRoleModel model )
    {
        List<String> resourceIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelResource resource : application.getResources() )
            {
                resourceIdList.add( resource.getId() );
            }
        }

        return resourceIdList;
    }

    public static List<String> getRoleIdList( RedbackRoleModel model )
    {
        List<String> roleIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                roleIdList.add( role.getId() );
            }
        }

        return roleIdList;
    }


    public static List<String> getTemplateIdList( RedbackRoleModel model )
    {
        List<String> templateIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                templateIdList.add( template.getId() );
            }
        }

        return templateIdList;

    }

    /**
     * WARNING: can return null
     *
     * @param model
     * @param roleId
     * @return
     */
    @SuppressWarnings( "unchecked" )
    public static ModelRole getModelRole( RedbackRoleModel model, String roleId )
    {
        ModelRole mrole = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                if ( roleId.equals( role.getId() ) )
                {
                    mrole = role;
                }
            }
        }

        return mrole;
    }

    /**
     * WARNING: can return null
     *
     * @param model
     * @param templateId
     * @return
     */
    @SuppressWarnings( "unchecked" )
    public static ModelTemplate getModelTemplate( RedbackRoleModel model, String templateId )
    {
        ModelTemplate mtemplate = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( templateId.equals( template.getId() ) )
                {
                    mtemplate = template;
                }
            }
        }

        return mtemplate;
    }

    /**
     * WARNING: can return null
     *
     * @param model
     * @param operationId
     * @return
     */
    @SuppressWarnings( "unchecked" )
    public static ModelOperation getModelOperation( RedbackRoleModel model, String operationId )
    {
        ModelOperation moperation = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelOperation operation : application.getOperations() )
            {
                if ( operationId.equals( operation.getId() ) )
                {
                    moperation = operation;
                }
            }
        }

        return moperation;
    }

    @SuppressWarnings( "unchecked" )
    public static ModelResource getModelResource( RedbackRoleModel model, String resourceId )
    {
        ModelResource mresource = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelResource resource : application.getResources() )
            {
                if ( resourceId.equals( resource.getId() ) )
                {
                    mresource = resource;
                }
            }
        }

        return mresource;
    }

    @SuppressWarnings( "unchecked" )
    public static DAG generateRoleGraph( RedbackRoleModel model )
        throws CycleDetectedException
    {
        DAG roleGraph = new DAG();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                roleGraph.addVertex( role.getId() );

                if ( role.getChildRoles() != null )
                {
                    for ( String childRole : role.getChildRoles() )
                    {
                        roleGraph.addVertex( childRole );

                        roleGraph.addEdge( role.getId(), childRole );
                    }
                }

                if ( role.getParentRoles() != null )
                {
                    for ( String parentRole : role.getParentRoles() )
                    {
                        roleGraph.addVertex( parentRole );

                        roleGraph.addEdge( parentRole, role.getId() );
                    }
                }
            }
        }

        return roleGraph;
    }

    @SuppressWarnings( "unchecked" )
    public static DAG generateTemplateGraph( RedbackRoleModel model )
        throws CycleDetectedException
    {
        DAG templateGraph = generateRoleGraph( model );

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                templateGraph.addVertex( template.getId() );

                if ( template.getChildRoles() != null )
                {
                    for ( String childRole : template.getChildRoles() )
                    {
                        templateGraph.addVertex( childRole );

                        templateGraph.addEdge( template.getId(), childRole );
                    }
                }

                if ( template.getParentRoles() != null )
                {
                    for ( String parentRole : template.getParentRoles() )
                    {
                        templateGraph.addVertex( parentRole );

                        templateGraph.addEdge( parentRole, template.getId() );
                    }
                }

                if ( template.getChildTemplates() != null )
                {
                    for ( String childTemplate : template.getChildTemplates() )
                    {
                        templateGraph.addVertex( childTemplate );

                        templateGraph.addEdge( template.getId(), childTemplate );
                    }
                }

                if ( template.getParentTemplates() != null )
                {
                    for ( String parentTemplate : template.getParentTemplates() )
                    {
                        templateGraph.addVertex( parentTemplate );

                        templateGraph.addEdge( parentTemplate, template.getId() );
                    }
                }
            }
        }

        return templateGraph;
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> reverseTopologicalSortedRoleList( RedbackRoleModel model )
        throws CycleDetectedException
    {
        LinkedList<String> sortedGraph =
            (LinkedList<String>) TopologicalSorter.sort( RoleModelUtils.generateRoleGraph( model ) );
        List<String> resortedGraph = new LinkedList<String>();

        while ( !sortedGraph.isEmpty() )
        {
            resortedGraph.add( sortedGraph.removeLast() );
        }

        return resortedGraph;
    }

}
