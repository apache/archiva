package org.codehaus.plexus.redback.struts2.interceptor;

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

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.apache.archiva.redback.integration.checks.security.AdminAutoCreateCheck;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.system.SecuritySystemConstants;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.integration.util.AutoLoginCookies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * EnvironmentCheckInterceptor
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redbackForceAdminUserInterceptor" )
@Scope( "prototype" )
public class ForceAdminUserInterceptor
    implements Interceptor
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    private static final String SECURITY_ADMIN_USER_NEEDED = "security-admin-user-needed";

    private static boolean checked = false;

    /**
     *  role-hint="configurable"
     */
    @Inject
    @Named( value = "userManager#configurable" )
    private UserManager userManager;

    /**
     *  role-hint="default"
     */
    @Inject
    private RoleManager roleManager;

    /**
     *  role-hint="default"
     */
    @Inject
    private UserConfiguration config;

    @Inject
    protected SecuritySystem securitySystem;

    @Inject
    private AutoLoginCookies autologinCookies;

    protected Map<String, Object> session;

    public void destroy()
    {
        // no-op
    }

    public void init()
    {

    }

    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        if ( checked )
        {
            return invocation.invoke();
        }

        try
        {
            User user = userManager.findUser( getAdminUid() );
            if ( user == null )
            {
                user = useForceAdminFile();
                if ( user == null )
                {
                    log.info( "No admin user configured - forwarding to admin user creation page." );
                    return SECURITY_ADMIN_USER_NEEDED;
                }
            }

            assignAdminRole( user );

            checked = true;
            log.info( "Admin user found. No need to configure admin user." );

        }
        catch ( UserNotFoundException e )
        {
            User user = useForceAdminFile();
            if ( user != null )
            {
                assignAdminRole( user );

                checked = true;
            }
            else
            {
                log.info( "No admin user found - forwarding to admin user creation page." );
                return SECURITY_ADMIN_USER_NEEDED;
            }
        }

        return invocation.invoke();
    }

    private User useForceAdminFile()
    {
        try
        {
            String forceAdminFilePath = System.getProperty( AdminAutoCreateCheck.FORCE_ADMIN_FILE_PATH );
            if ( StringUtils.isBlank( forceAdminFilePath ) )
            {
                log.info( AdminAutoCreateCheck.FORCE_ADMIN_FILE_PATH + " system props is empty don't use an auto creation admin " );
                return null;
            }
            File file = new File( forceAdminFilePath );
            if ( !file.exists() )
            {
                log.warn( "file set in sysprops " + AdminAutoCreateCheck.FORCE_ADMIN_FILE_PATH + " not exists skip admin auto creation" );
                return null;
            }
            Properties properties = new Properties();
            FileInputStream fis = null;
            try
            {
                properties.load( new FileInputStream( file ) );
            }
            catch ( Exception e )
            {
                log.warn( "error loading properties from file " + forceAdminFilePath + " skip admin auto creation" );
                return null;
            }

            // ensure we have all properties
            String password = properties.getProperty( AdminAutoCreateCheck.ADMIN_PASSWORD_KEY );
            String email = properties.getProperty( AdminAutoCreateCheck.ADMIN_EMAIL_KEY );
            String fullName = properties.getProperty( AdminAutoCreateCheck.ADMIN_FULL_NAME_KEY );

            if ( StringUtils.isBlank( password ) )
            {
                log.warn( "property " + AdminAutoCreateCheck.ADMIN_PASSWORD_KEY + " not set skip auto admin creation" );
                return null;
            }

            if ( StringUtils.isBlank( email ) )
            {
                log.warn( "property " + AdminAutoCreateCheck.ADMIN_EMAIL_KEY + " not set skip auto admin creation" );
                return null;
            }

            if ( StringUtils.isBlank( fullName ) )
            {
                log.warn( "property " + AdminAutoCreateCheck.ADMIN_FULL_NAME_KEY + " not set skip auto admin creation" );
                return null;
            }

            User u = userManager.createUser( getAdminUid(), fullName, email );

            u.setPassword( password );
            u.setLocked( false );
            u.setPasswordChangeRequired( false );
            u.setPermanent( true );

            u = userManager.addUser( u );
            u.setPassword( password );

            PasswordBasedAuthenticationDataSource authdatasource = new PasswordBasedAuthenticationDataSource();
            authdatasource.setPrincipal( u.getUsername() );
            authdatasource.setPassword( u.getPassword() );
            SecuritySession securitySession = securitySystem.authenticate( authdatasource );
            if ( securitySession.getAuthenticationResult().isAuthenticated() )
            {
                // good add various tokens.
                ServletActionContext.getRequest().getSession( true ).setAttribute(
                    SecuritySystemConstants.SECURITY_SESSION_KEY, securitySession );
                autologinCookies.setSignonCookie( authdatasource.getPrincipal(), ServletActionContext.getResponse(),
                                                  ServletActionContext.getRequest() );
                u = securitySession.getUser();
                u.setLastLoginDate( new Date() );
                securitySystem.getUserManager().updateUser( u );
            }

            return u;
        }
        catch ( Exception e )
        {
            log.warn( "failed to automatically create an admin account " + e.getMessage(), e );
        }
        return null;
    }

    private String getAdminUid()
    {
        return config.getString( "redback.default.admin" );
    }

    private void assignAdminRole( User user )
        throws RoleManagerException
    {
        roleManager.assignRole( "system-administrator", user.getPrincipal().toString() );
    }
}
