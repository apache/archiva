package org.apache.archiva.redback.management;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.keys.jdo.AuthenticationKeyDatabase;
import org.apache.archiva.redback.keys.jdo.io.stax.RedbackKeyManagementJdoStaxReader;
import org.apache.archiva.redback.keys.jdo.io.stax.RedbackKeyManagementJdoStaxWriter;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.jdo.RbacDatabase;
import org.apache.archiva.redback.rbac.jdo.io.stax.RbacJdoModelStaxReader;
import org.apache.archiva.redback.rbac.jdo.io.stax.RbacJdoModelStaxWriter;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.jdo.UserDatabase;
import org.apache.archiva.redback.users.jdo.io.stax.UsersManagementStaxReader;
import org.apache.archiva.redback.users.jdo.io.stax.UsersManagementStaxWriter;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

/**
 * JDO implementation of the data management tool.
 *
 * @todo do we really need JDO specifics here? Could optimize by going straight to JDOFactory
 * @todo check whether this current method logs everything unnecessarily.
 */
@Service("dataManagementTool#jdo")
public class JdoDataManagementTool
    implements DataManagementTool
{
    private static final String USERS_XML_NAME = "users.xml";

    private static final String KEYS_XML_NAME = "keys.xml";

    private static final String RBAC_XML_NAME = "rbac.xml";

    public void backupRBACDatabase( RBACManager manager, File backupDirectory )
        throws RbacManagerException, IOException, XMLStreamException
    {
        RbacDatabase database = new RbacDatabase();
        database.setRoles( manager.getAllRoles() );
        database.setUserAssignments( manager.getAllUserAssignments() );
        database.setPermissions( manager.getAllPermissions() );
        database.setOperations( manager.getAllOperations() );
        database.setResources( manager.getAllResources() );

        RbacJdoModelStaxWriter writer = new RbacJdoModelStaxWriter();
        Writer fileWriter = createWriter( backupDirectory, RBAC_XML_NAME, database.getModelEncoding() );
        try
        {
            writer.write( fileWriter, database );
        }
        finally
        {
            IOUtils.closeQuietly( fileWriter );
        }
    }

    public void backupUserDatabase( UserManager manager, File backupDirectory )
        throws IOException, XMLStreamException, UserManagerException
    {
        UserDatabase database = new UserDatabase();
        database.setUsers( manager.getUsers() );

        UsersManagementStaxWriter writer = new UsersManagementStaxWriter();
        Writer fileWriter = createWriter( backupDirectory, USERS_XML_NAME, database.getModelEncoding() );
        try
        {
            writer.write( fileWriter, database );
        }
        finally
        {
            IOUtils.closeQuietly( fileWriter );
        }
    }

    public void backupKeyDatabase( KeyManager manager, File backupDirectory )
        throws IOException, XMLStreamException
    {
        try
        {
            manager.removeExpiredKeys();
        }
        catch ( KeyManagerException e )
        {
            throw new IOException( "Error removing expired keys" );
        }

        AuthenticationKeyDatabase database = new AuthenticationKeyDatabase();
        database.setKeys( manager.getAllKeys() );

        RedbackKeyManagementJdoStaxWriter writer = new RedbackKeyManagementJdoStaxWriter();
        Writer fileWriter = createWriter( backupDirectory, KEYS_XML_NAME, database.getModelEncoding() );
        try
        {
            writer.write( fileWriter, database );
        }
        finally
        {
            IOUtils.closeQuietly( fileWriter );
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreRBACDatabase( RBACManager manager, File backupDirectory )
        throws IOException, XMLStreamException, RbacManagerException
    {
        RbacJdoModelStaxReader reader = new RbacJdoModelStaxReader();

        FileReader fileReader = new FileReader( new File( backupDirectory, RBAC_XML_NAME ) );

        RbacDatabase database;
        try
        {
            database = reader.read( fileReader );
        }
        finally
        {
            IOUtils.closeQuietly( fileReader );
        }

        Map<String, Permission> permissionMap = new HashMap<String, Permission>();
        Map<String, Resource> resources = new HashMap<String, Resource>();
        Map<String, Operation> operations = new HashMap<String, Operation>();
        for ( Role role : (List<Role>) database.getRoles() )
        {
            // TODO: this could be generally useful and put into saveRole itself as long as the performance penalty isn't too harsh.
            //   Currently it always saves everything where it could pull pack the existing permissions, etc if they exist
            List<Permission> permissions = new ArrayList<Permission>();
            for ( Permission permission : role.getPermissions() )
            {
                if ( permissionMap.containsKey( permission.getName() ) )
                {
                    permission = permissionMap.get( permission.getName() );
                }
                else if ( manager.permissionExists( permission ) )
                {
                    permission = manager.getPermission( permission.getName() );
                    permissionMap.put( permission.getName(), permission );
                }
                else
                {
                    Operation operation = permission.getOperation();
                    if ( operations.containsKey( operation.getName() ) )
                    {
                        operation = operations.get( operation.getName() );
                    }
                    else if ( manager.operationExists( operation ) )
                    {
                        operation = manager.getOperation( operation.getName() );
                        operations.put( operation.getName(), operation );
                    }
                    else
                    {
                        operation = manager.saveOperation( operation );
                        operations.put( operation.getName(), operation );
                    }
                    permission.setOperation( operation );

                    Resource resource = permission.getResource();
                    if ( resources.containsKey( resource.getIdentifier() ) )
                    {
                        resource = resources.get( resource.getIdentifier() );
                    }
                    else if ( manager.resourceExists( resource ) )
                    {
                        resource = manager.getResource( resource.getIdentifier() );
                        resources.put( resource.getIdentifier(), resource );
                    }
                    else
                    {
                        resource = manager.saveResource( resource );
                        resources.put( resource.getIdentifier(), resource );
                    }
                    permission.setResource( resource );

                    permission = manager.savePermission( permission );
                    permissionMap.put( permission.getName(), permission );
                }
                permissions.add( permission );
            }
            role.setPermissions( permissions );

            manager.saveRole( role );
        }

        for ( UserAssignment userAssignment : (List<UserAssignment>) database.getUserAssignments() )
        {
            manager.saveUserAssignment( userAssignment );
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreUsersDatabase( UserManager manager, File backupDirectory )
        throws IOException, XMLStreamException, UserManagerException
    {
        UsersManagementStaxReader reader = new UsersManagementStaxReader();

        FileReader fileReader = new FileReader( new File( backupDirectory, USERS_XML_NAME ) );

        UserDatabase database;
        try
        {
            database = reader.read( fileReader );
        }
        finally
        {
            IOUtils.closeQuietly( fileReader );
        }

        for ( User user : (List<User>) database.getUsers() )
        {
            manager.addUserUnchecked( user );
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreKeysDatabase( KeyManager manager, File backupDirectory )
        throws IOException, XMLStreamException
    {
        RedbackKeyManagementJdoStaxReader reader = new RedbackKeyManagementJdoStaxReader();

        FileReader fileReader = new FileReader( new File( backupDirectory, KEYS_XML_NAME ) );

        AuthenticationKeyDatabase database;
        try
        {
            database = reader.read( fileReader );
        }
        finally
        {
            IOUtils.closeQuietly( fileReader );
        }

        for ( AuthenticationKey key : (List<AuthenticationKey>) database.getKeys() )
        {
            manager.addKey( key );
        }
    }

    public void eraseRBACDatabase( RBACManager manager )
    {
        manager.eraseDatabase();
    }

    public void eraseUsersDatabase( UserManager manager )
    {
        manager.eraseDatabase();
    }

    public void eraseKeysDatabase( KeyManager manager )
    {
        manager.eraseDatabase();
    }

    private Writer createWriter( File directory, String file, String encoding )
        throws FileNotFoundException
    {
        File f = new File( directory, file );
        File parentFile = f.getParentFile();
        parentFile.mkdirs();

        FileOutputStream out = new FileOutputStream( f );
        return new OutputStreamWriter( out, Charset.forName( encoding ) );
    }
}
