package org.apache.archiva.redback.tests;

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

import org.apache.archiva.redback.rbac.RBACManagerListener;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RbacManagerEventTracker
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class RbacManagerEventTracker
    implements RBACManagerListener
{
    public long initCount = 0;

    public Boolean lastDbFreshness;

    public List<String> addedRoleNames = new ArrayList<String>();

    public List<String> removedRoleNames = new ArrayList<String>();

    public List<String> addedPermissionNames = new ArrayList<String>();

    public List<String> removedPermissionNames = new ArrayList<String>();

    protected Logger logger = LoggerFactory.getLogger( getClass() );

    public void rbacInit( boolean freshdb )
    {
        log( "Init - freshdb: " + freshdb );
        initCount++;
        lastDbFreshness = Boolean.valueOf( freshdb );
    }

    public void rbacPermissionRemoved( Permission permission )
    {
        log( "Permission Removed: " + permission.getName() );
        String obj = permission.getName();
        if ( !removedPermissionNames.contains( obj ) )
        {
            removedPermissionNames.add( obj );
        }
    }

    public void rbacPermissionSaved( Permission permission )
    {
        log( "Permission Saved: " + permission.getName() );
        String obj = permission.getName();
        if ( !addedPermissionNames.contains( obj ) )
        {
            addedPermissionNames.add( obj );
        }
    }

    public void rbacRoleRemoved( Role role )
    {
        log( "Role Removed: " + role.getName() );
        String obj = role.getName();
        if ( !removedRoleNames.contains( obj ) )
        {
            removedRoleNames.add( obj );
        }
    }

    public void rbacRoleSaved( Role role )
    {
        log( "Role Saved: " + role.getName() );
        String obj = role.getName();
        if ( !addedRoleNames.contains( obj ) )
        {
            addedRoleNames.add( obj );
        }
    }

    public void rbacUserAssignmentRemoved( UserAssignment userAssignment )
    {

    }

    public void rbacUserAssignmentSaved( UserAssignment userAssignment )
    {

    }

    private void log( String msg )
    {
        logger.info( "[RBAC Event Tracker] : {}", msg );
    }
}
