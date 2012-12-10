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

import junit.framework.TestCase;
import org.apache.archiva.redback.common.jdo.UserConfigurableJdoFactory;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.tests.utils.RBACDefaults;
import org.apache.archiva.redback.users.User;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" })
public class DataManagementTest
    extends TestCase
{
    @Inject
    private DataManagementTool dataManagementTool;

    private File targetDirectory;

    @Inject
    @Named(value = "jdoFactory#users")
    UserConfigurableJdoFactory jdoFactory;

    @Inject
    @Named(value = "userManager#jdo")
    UserManager userManager;

    @Inject
    @Named(value = "keyManager#jdo")
    KeyManager keyManager;


    @Inject
    @Named(value = "rBACManager#jdo")
    RBACManager rbacManager;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        targetDirectory = createBackupDirectory();
    }

    @Test
    public void testEraseUsers()
        throws Exception
    {

        dataManagementTool.eraseUsersDatabase( userManager );

        createUserDatabase( userManager );

        dataManagementTool.eraseUsersDatabase( userManager );

        assertEmpty( userManager );
    }

    @Test
    public void testEraseKeys()
        throws Exception
    {

        createKeyDatabase( keyManager );

        dataManagementTool.eraseKeysDatabase( keyManager );

        assertEmpty( keyManager );
    }

    @Test
    public void testBackupRbac()
        throws Exception
    {
        RBACManager manager = rbacManager;

        dataManagementTool.eraseRBACDatabase( manager );

        createRbacDatabase( manager );

        dataManagementTool.backupRBACDatabase( manager, targetDirectory );

        File backupFile = new File( targetDirectory, "rbac.xml" );

        assertTrue( "Check database exists", backupFile.exists() );

        StringWriter sw = new StringWriter();

        IOUtils.copy( getClass().getResourceAsStream( "/expected-rbac.xml" ), sw );

        XMLAssert.assertXMLEqual( new StringReader( sw.toString() ),
                                  new StringReader( FileUtils.readFileToString( backupFile ) ) );

    }

    private void createRbacDatabase( RBACManager manager )
        throws RbacManagerException
    {
        RBACDefaults defaults = new RBACDefaults( manager );

        defaults.createDefaults();

        UserAssignment assignment = manager.createUserAssignment( "bob" );
        assignment.addRoleName( "Developer" );
        manager.saveUserAssignment( assignment );

        assignment = manager.createUserAssignment( "betty" );
        assignment.addRoleName( "System Administrator" );
        manager.saveUserAssignment( assignment );
    }

    @Test
    public void testBackupUsers()
        throws Exception
    {
        UserManager manager = userManager;

        createUserDatabase( manager );

        dataManagementTool.backupUserDatabase( manager, targetDirectory );

        File backupFile = new File( targetDirectory, "users.xml" );

        assertTrue( "Check database exists", backupFile.exists() );

        StringWriter sw = new StringWriter();

        IOUtils.copy( getClass().getResourceAsStream( "/expected-users.xml" ), sw );

        String actual = FileUtils.readFileToString( backupFile ).trim();
        String expected = sw.toString().trim();

        XMLAssert.assertXMLEqual( removeTimestampVariance( expected ), removeTimestampVariance( actual ) );

    }

    private void createUserDatabase( UserManager manager )
        throws UserManagerException
    {
        User user = manager.createUser( "smcqueen", "Steve McQueen", "the cooler king" );
        user.setPassword( "abc123" );
        manager.addUser( user );

        user = manager.createUser( "bob", "Sideshow Bob", "bob_862@hotmail.com" );
        user.setPassword( "bobby862" );
        manager.addUser( user );

        user = manager.createUser( "betty", "Betty", "betty@aol.com" );
        user.setPassword( "rover2" );
        manager.addUser( user );
    }

    @Test
    public void testBackupKeys()
        throws Exception
    {
        KeyManager manager = keyManager;

        createKeyDatabase( manager );

        Thread.sleep( 60000 );

        dataManagementTool.backupKeyDatabase( manager, targetDirectory );

        File backupFile = new File( targetDirectory, "keys.xml" );

        assertTrue( "Check database exists", backupFile.exists() );

        StringWriter sw = new StringWriter();

        IOUtils.copy( getClass().getResourceAsStream( "/expected-keys.xml" ), sw );

        String actual = FileUtils.readFileToString( backupFile ).trim();
        String expected = sw.toString().trim();

        XMLAssert.assertXMLEqual( removeKeyAndTimestampVariance( expected ), removeKeyAndTimestampVariance( actual ) );

    }

    private static void createKeyDatabase( KeyManager manager )
        throws KeyManagerException
    {
        manager.createKey( "bob", "Testing", 15 );
        manager.createKey( "betty", "Something", 25 );
        manager.createKey( "fred", "Else", 30 );
        manager.createKey( "tony", "Expired", 0 );
    }

    @Test
    public void testRestoreRbac()
        throws Exception
    {
        RBACManager manager = rbacManager;

        dataManagementTool.eraseRBACDatabase( manager );

        assertEmpty( manager );

        File backupFile = new File( targetDirectory, "rbac.xml" );

        InputStream is = getClass().getResourceAsStream( "/expected-rbac.xml" );

        FileWriter fw = new FileWriter( backupFile );

        IOUtils.copy( is, fw );

        is.close();

        fw.close();

        dataManagementTool.restoreRBACDatabase( manager, targetDirectory );

        List<Role> roles = manager.getAllRoles();
        List<UserAssignment> assignments = manager.getAllUserAssignments();
        assertEquals( 4, roles.size() );
        assertEquals( 2, assignments.size() );
        assertEquals( 6, manager.getAllOperations().size() );
        assertEquals( 1, manager.getAllResources().size() );
        assertEquals( 6, manager.getAllPermissions().size() );

        Role role = roles.get( 0 );
        assertEquals( "User Administrator", role.getName() );
        assertTrue( role.isAssignable() );
        assertEquals( 2, role.getPermissions().size() );
        assertPermission( role.getPermissions().get( 0 ), "Edit All Users", "edit-all-users", "*" );
        assertPermission( role.getPermissions().get( 1 ), "Remove Roles", "remove-roles", "*" );

        role = roles.get( 1 );
        assertEquals( "System Administrator", role.getName() );
        assertTrue( role.isAssignable() );
        assertEquals( 1, role.getChildRoleNames().size() );
        assertEquals( "User Administrator", role.getChildRoleNames().get( 0 ) );
        assertEquals( 4, role.getPermissions().size() );
        assertPermission( role.getPermissions().get( 0 ), "Edit Configuration", "edit-configuration", "*" );
        assertPermission( role.getPermissions().get( 1 ), "Run Indexer", "run-indexer", "*" );
        assertPermission( role.getPermissions().get( 2 ), "Add Repository", "add-repository", "*" );
        assertPermission( role.getPermissions().get( 3 ), "Regenerate Index", "regenerate-index", "*" );

        role = roles.get( 2 );
        assertEquals( "Trusted Developer", role.getName() );
        assertTrue( role.isAssignable() );
        assertEquals( 1, role.getChildRoleNames().size() );
        assertEquals( "System Administrator", role.getChildRoleNames().get( 0 ) );
        assertEquals( 1, role.getPermissions().size() );
        assertPermission( role.getPermissions().get( 0 ), "Run Indexer", "run-indexer", "*" );

        role = roles.get( 3 );
        assertEquals( "Developer", role.getName() );
        assertTrue( role.isAssignable() );
        assertEquals( 1, role.getChildRoleNames().size() );
        assertEquals( "Trusted Developer", role.getChildRoleNames().get( 0 ) );
        assertEquals( 1, role.getPermissions().size() );
        assertPermission( role.getPermissions().get( 0 ), "Run Indexer", "run-indexer", "*" );

        UserAssignment assignment = assignments.get( 0 );
        assertEquals( "bob", assignment.getPrincipal() );
        assertEquals( 1, assignment.getRoleNames().size() );
        assertEquals( "Developer", assignment.getRoleNames().get( 0 ) );

        assignment = assignments.get( 1 );
        assertEquals( "betty", assignment.getPrincipal() );
        assertEquals( 1, assignment.getRoleNames().size() );
        assertEquals( "System Administrator", assignment.getRoleNames().get( 0 ) );
    }

    private void assertEmpty( RBACManager manager )
        throws RbacManagerException
    {
        assertEquals( 0, manager.getAllRoles().size() );
        assertEquals( 0, manager.getAllUserAssignments().size() );
        assertEquals( 0, manager.getAllOperations().size() );
        assertEquals( 0, manager.getAllResources().size() );
        assertEquals( 0, manager.getAllPermissions().size() );
    }

    @Test
    public void testRestoreUsers()
        throws Exception
    {
        UserManager manager = userManager;

        dataManagementTool.eraseUsersDatabase( manager );

        assertEmpty( manager );

        File backupFile = new File( targetDirectory, "users.xml" );

        FileWriter fw = new FileWriter( backupFile );

        IOUtils.copy( getClass().getResourceAsStream( "/expected-users.xml" ), fw );

        fw.close();

        dataManagementTool.restoreUsersDatabase( manager, targetDirectory );

        List<User> users = manager.getUsers();
        assertEquals( 3, users.size() );

        User user = users.get( 0 );
        assertEquals( "smcqueen", user.getUsername() );
        assertEquals( "bKE9UspwyIPg8LsQHkJaiehiTeUdstI5JZOvaoQRgJA=", user.getEncodedPassword() );
        assertEquals( "Steve McQueen", user.getFullName() );
        assertEquals( "the cooler king", user.getEmail() );
        assertEquals( 1164424661686L, user.getLastPasswordChange().getTime() );
        assertEquals( Arrays.asList( new String[]{ "bKE9UspwyIPg8LsQHkJaiehiTeUdstI5JZOvaoQRgJA=" } ),
                      user.getPreviousEncodedPasswords() );

        user = users.get( 1 );
        assertEquals( "bob", user.getUsername() );
        assertEquals( "A0MR+q0lm554bD6Uft60ztlYZ8N1pEqXhKNM9H7SlS8=", user.getEncodedPassword() );
        assertEquals( "Sideshow Bob", user.getFullName() );
        assertEquals( "bob_862@hotmail.com", user.getEmail() );
        assertEquals( 1164424669526L, user.getLastPasswordChange().getTime() );
        assertEquals( Arrays.asList( new String[]{ "A0MR+q0lm554bD6Uft60ztlYZ8N1pEqXhKNM9H7SlS8=" } ),
                      user.getPreviousEncodedPasswords() );

        user = users.get( 2 );
        assertEquals( "betty", user.getUsername() );
        assertEquals( "L/mA/suWallwvYzw4wyRYkn5y8zWxAITuv4sLhJLN1E=", user.getEncodedPassword() );
        assertEquals( "Betty", user.getFullName() );
        assertEquals( "betty@aol.com", user.getEmail() );
        assertEquals( 1164424669536L, user.getLastPasswordChange().getTime() );
        assertEquals( Arrays.asList( new String[]{ "L/mA/suWallwvYzw4wyRYkn5y8zWxAITuv4sLhJLN1E=" } ),
                      user.getPreviousEncodedPasswords() );
    }

    private void assertEmpty( UserManager manager )
        throws UserManagerException
    {
        List<User> users = manager.getUsers();
        assertEquals( 0, users.size() );
    }

    @Test
    public void testRestoreKeys()
        throws Exception
    {
        KeyManager manager = keyManager;

        dataManagementTool.eraseKeysDatabase( manager );

        assertEmpty( manager );

        File backupFile = new File( targetDirectory, "keys.xml" );

        FileWriter fw = new FileWriter( backupFile );

        IOUtils.copy( getClass().getResourceAsStream( "/expected-keys.xml" ), fw );

        fw.close();

        dataManagementTool.restoreKeysDatabase( manager, targetDirectory );

        List<AuthenticationKey> keys = manager.getAllKeys();
        assertEquals( 3, keys.size() );

        AuthenticationKey key = keys.get( 0 );
        assertEquals( "248df0fec5d54e3eb11339f5e81d8bd7", key.getKey() );
        assertEquals( "bob", key.getForPrincipal() );
        assertEquals( "Testing", key.getPurpose() );
        assertEquals( 1164426311921L, key.getDateCreated().getTime() );
        assertEquals( 1164427211921L, key.getDateExpires().getTime() );

        key = keys.get( 1 );
        assertEquals( "a98dddc2ae614a7c82f8afd3ba6e39fb", key.getKey() );
        assertEquals( "betty", key.getForPrincipal() );
        assertEquals( "Something", key.getPurpose() );
        assertEquals( 1164426315657L, key.getDateCreated().getTime() );
        assertEquals( 1164427815657L, key.getDateExpires().getTime() );

        key = keys.get( 2 );
        assertEquals( "1428d2ca3a0246f0a1d979504e351388", key.getKey() );
        assertEquals( "fred", key.getForPrincipal() );
        assertEquals( "Else", key.getPurpose() );
        assertEquals( 1164426315664L, key.getDateCreated().getTime() );
        assertEquals( 1164428115664L, key.getDateExpires().getTime() );
    }

    private void assertEmpty( KeyManager manager )
    {
        assertEquals( 0, manager.getAllKeys().size() );
    }

    private String removeKeyAndTimestampVariance( String content )
    {
        return removeTagContent( removeTagContent( removeTagContent( content, "dateCreated" ), "dateExpires" ), "key" );
    }

    private static String removeTimestampVariance( String content )
    {
        return removeTagContent( removeTagContent( content, "lastPasswordChange" ), "accountCreationDate" );
    }

    private static String removeTagContent( String content, String field )
    {
        return content.replaceAll( "<" + field + ">.*</" + field + ">", "<" + field + "></" + field + ">" );
    }

    private static void assertPermission( Permission permission, String name, String operation, String resource )
    {
        assertEquals( name, permission.getName() );
        assertEquals( operation, permission.getOperation().getName() );
        assertEquals( resource, permission.getResource().getIdentifier() );
    }

    private static File createBackupDirectory()
    {
        String timestamp = new SimpleDateFormat( "yyyyMMdd.HHmmss", Locale.US ).format( new Date() );

        File targetDirectory = new File( SystemUtils.getJavaIoTmpDir(), "./target/backups/" + timestamp );
        targetDirectory.mkdirs();

        return targetDirectory;
    }

}
