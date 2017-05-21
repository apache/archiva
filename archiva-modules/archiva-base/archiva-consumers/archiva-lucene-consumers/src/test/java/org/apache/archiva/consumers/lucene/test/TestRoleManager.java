package org.apache.archiva.consumers.lucene.test;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.springframework.stereotype.Service;

import java.net.URL;

/**
 * @author Olivier Lamy
 */
@Service( "roleManager#test" )
public class TestRoleManager
    implements RoleManager
{
    @Override
    public void loadRoleModel( URL resourceLocation )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void loadRoleModel( RedbackRoleModel model )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createTemplatedRole( String templateId, String resource )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeTemplatedRole( String templateId, String resource )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateRole( String templateId, String oldResource, String newResource )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void assignRole( String roleId, String principal )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void assignRoleByName( String roleName, String principal )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void assignTemplatedRole( String templateId, String resource, String principal )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void unassignRole( String roleId, String principal )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void unassignRoleByName( String roleName, String principal )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean roleExists( String roleId )
        throws RoleManagerException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean templatedRoleExists( String templateId, String resource )
        throws RoleManagerException
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RedbackRoleModel getModel()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void verifyTemplatedRole( String templateID, String resource )
        throws RoleManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initialize()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
