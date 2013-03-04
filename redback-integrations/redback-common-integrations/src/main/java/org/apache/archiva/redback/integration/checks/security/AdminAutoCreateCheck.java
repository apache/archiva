package org.apache.archiva.redback.integration.checks.security;

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

import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
@Service("environmentCheck#adminAutoCreateCheck")
public class AdminAutoCreateCheck
    implements EnvironmentCheck
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    public static final String FORCE_ADMIN_FILE_PATH = "redback.admin.creation.file";

    public static final String ADMIN_FULL_NAME_KEY = "redback.admin.fullname";

    public static final String ADMIN_EMAIL_KEY = "redback.admin.email";

    public static final String ADMIN_PASSWORD_KEY = "redback.admin.password";

    @Inject
    @Named(value = "userManager#default")
    private UserManager userManager;

    @Inject
    @Named(value = "userConfiguration#default")
    private UserConfiguration config;

    @Inject
    protected SecuritySystem securitySystem;

    @Inject
    private RoleManager roleManager;

    @Inject
    @Named(value = "rbacManager#default")
    private RBACManager rbacManager;

    public void validateEnvironment( List<String> violations )
    {
        try
        {
            User user = userManager.findUser( getAdminUid() );
            if ( user == null )
            {
                useForceAdminCreationFile();
            }
        }
        catch ( UserNotFoundException e )
        {
            useForceAdminCreationFile();
        }
        catch ( UserManagerException e )
        {
            useForceAdminCreationFile();
        }
    }

    private void useForceAdminCreationFile()
    {
        try
        {
            String forceAdminFilePath = System.getProperty( FORCE_ADMIN_FILE_PATH );
            if ( StringUtils.isBlank( forceAdminFilePath ) )
            {
                log.info( "{} system props is empty don't use an auto creation admin ", FORCE_ADMIN_FILE_PATH );
                return;
            }
            File file = new File( forceAdminFilePath );
            if ( !file.exists() )
            {
                log.warn( "file set in sysprops {} not exists skip admin auto creation", FORCE_ADMIN_FILE_PATH );
                return;
            }
            log.debug( "user {} not found try auto creation", getAdminUid() );
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream( file );
            try
            {
                properties.load( fis );
            }
            catch ( Exception e )
            {
                log.warn( "error loading properties from file {} skip admin auto creation", forceAdminFilePath );
                return;
            }
            finally
            {
                IOUtils.closeQuietly( fis );
            }

            // ensure we have all properties
            String password = properties.getProperty( ADMIN_PASSWORD_KEY );
            String email = properties.getProperty( ADMIN_EMAIL_KEY );
            String fullName = properties.getProperty( ADMIN_FULL_NAME_KEY );

            if ( StringUtils.isBlank( password ) )
            {
                log.warn( "property {} not set skip auto admin creation", ADMIN_PASSWORD_KEY );
                return;
            }

            if ( StringUtils.isBlank( email ) )
            {
                log.warn( "property not set skip auto admin creation", ADMIN_EMAIL_KEY );
                return;
            }

            if ( StringUtils.isBlank( fullName ) )
            {
                log.warn( "property {} not set skip auto admin creation", ADMIN_FULL_NAME_KEY );
                return;
            }

            User u = userManager.createUser( getAdminUid(), fullName, email );

            u.setPassword( password );
            u.setLocked( false );
            u.setPasswordChangeRequired( false );
            u.setPermanent( true );
            u.setValidated( true );

            u = userManager.addUser( u );
            u.setPassword( password );

            PasswordBasedAuthenticationDataSource authdatasource = new PasswordBasedAuthenticationDataSource();
            authdatasource.setPrincipal( u.getUsername() );
            authdatasource.setPassword( u.getPassword() );
            SecuritySession securitySession = securitySystem.authenticate( authdatasource );
            if ( securitySession.getAuthenticationResult().isAuthenticated() )
            {
                // good add various tokens.
                u = securitySession.getUser();
                u.setLastLoginDate( new Date() );
                securitySystem.getUserManager().updateUser( u );
            }
            assignAdminRole( u );

        }
        catch ( Exception e )
        {
            log.warn( "failed to automatically create an admin account {}", e.getMessage(), e );
        }
    }

    private void assignAdminRole( User user )
        throws RoleManagerException
    {
        roleManager.assignRole( "system-administrator", user.getUsername() );
    }

    private String getAdminUid()
    {
        return config.getString( UserConfigurationKeys.DEFAULT_ADMIN );
    }
}
