package org.codehaus.plexus.redback.struts2.model;
/*
 * Copyright 2008 The Codehaus.
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

import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.role.model.ModelApplication;
import org.codehaus.plexus.redback.role.model.ModelRole;
import org.codehaus.plexus.redback.role.model.ModelTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @todo incredibly ugly population of the table, needs to be more concise
 */
public class ApplicationRoleDetails
{
    private String name;

    private String description;

    private List<String> assignedRoles;

    private List<String> availableRoles;

    private List<ModelTemplate> tableHeader;

    private List<List<RoleTableCell>> table;

    @SuppressWarnings("unchecked")
    public ApplicationRoleDetails( ModelApplication application, Collection<Role> effectivelyAssignedRoles,
                                   Collection<Role> allAssignedRoles, List<Role> assignableRoles )
    {
        name = application.getId();
        description = application.getDescription();

        List<ModelTemplate> templates = application.getTemplates();
        List<ModelRole> roles = application.getRoles();

        tableHeader = new LinkedList<ModelTemplate>( templates );

        computeRoles( roles, assignableRoles, effectivelyAssignedRoles, allAssignedRoles );

        computeTable( gatherResources( templates, assignableRoles ), effectivelyAssignedRoles, allAssignedRoles );
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public List<String> getAssignedRoles()
    {
        return assignedRoles;
    }

    public List<String> getAvailableRoles()
    {
        return availableRoles;
    }

    public List<ModelTemplate> getTableHeader()
    {
        return tableHeader;
    }

    public List<List<RoleTableCell>> getTable()
    {
        return table;
    }

    private void computeRoles( Collection<ModelRole> applicationRoles, Collection<Role> assignableRoles,
                               Collection<Role> effectivelyAssignedRoles, Collection<Role> allAssignedRoles )
    {
        assignedRoles = new ArrayList<String>();
        availableRoles = new ArrayList<String>();
        for ( Iterator<ModelRole> i = applicationRoles.iterator(); i.hasNext(); )
        {
            ModelRole role =  i.next();

            if ( isInList( role.getName(), allAssignedRoles ) )
            {
                if ( role.isAssignable() )
                {
                    assignedRoles.add( role.getName() );
                }
            }
            else if ( isInList( role.getName(), effectivelyAssignedRoles ) )
            {
                // nothing
            }
            else if ( isInList( role.getName(), assignableRoles ) )
            {
                if ( role.isAssignable() )
                {
                    availableRoles.add( role.getName() );
                }
            }
        }

        Collections.sort( assignedRoles, String.CASE_INSENSITIVE_ORDER );
        Collections.sort( availableRoles, String.CASE_INSENSITIVE_ORDER );
    }

    private Set<String> gatherResources( List<ModelTemplate> applicationTemplates, List<Role> roles )
    {
        Set<String> resources = new HashSet<String>();
        for ( ModelTemplate modelTemplate : applicationTemplates )
        {
            for ( Role role : roles )
            {
                String roleName = role.getName();
                if ( roleName.startsWith( modelTemplate.getNamePrefix() ) )
                {
                    String delimiter = modelTemplate.getDelimiter();
                    resources.add( roleName.substring( roleName.indexOf( delimiter ) + delimiter.length() ) );
                }
            }
        }
        return resources;
    }

    private void computeTable( Collection<String> resources, Collection<Role> effectivelyAssignedRoles,
                               Collection<Role> allAssignedRoles )
    {
        table = new LinkedList<List<RoleTableCell>>();

        List<String> resourcesList = new ArrayList<String>( resources );
        Collections.sort( resourcesList, String.CASE_INSENSITIVE_ORDER );

        for ( String resource : resourcesList )
        {
            LinkedList<RoleTableCell> tableRow = new LinkedList<RoleTableCell>();

            RoleTableCell resourceCell = new RoleTableCell();
            resourceCell.setName( resource );
            resourceCell.setLabel( true );
            tableRow.add( resourceCell );

            for ( ModelTemplate modelTemplate : tableHeader )
            {
                RoleTableCell cell = new RoleTableCell();

                cell.setName( modelTemplate.getNamePrefix() + modelTemplate.getDelimiter() + resource );
                cell.setEffectivelyAssigned( isInList( cell.getName(), effectivelyAssignedRoles ) );
                cell.setAssigned( isInList( cell.getName(), allAssignedRoles ) );
                cell.setLabel( false );

                tableRow.add( cell );
            }

            table.add( tableRow );
        }
    }

    private boolean isInList( String roleName, Collection<Role> effectivelyAssignedRoles )
    {
        for ( Role role : effectivelyAssignedRoles )
        {
            if ( roleName.equals( role.getName() ) )
            {
                return true;
            }
        }
        return false;
    }

    public class RoleTableCell
    {
        private String name;

        private boolean effectivelyAssigned;

        private boolean assigned;

        private boolean label;

        public String getName()
        {
            return name;
        }

        public void setName( String name )
        {
            this.name = name;
        }

        public boolean isEffectivelyAssigned()
        {
            return effectivelyAssigned;
        }

        public void setEffectivelyAssigned( boolean effectivelyAssigned )
        {
            this.effectivelyAssigned = effectivelyAssigned;
        }

        public boolean isAssigned()
        {
            return assigned;
        }

        public void setAssigned( boolean assigned )
        {
            this.assigned = assigned;
        }

        public boolean isLabel()
        {
            return label;
        }

        public void setLabel( boolean label )
        {
            this.label = label;
        }
    }
}
