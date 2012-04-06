package org.codehaus.plexus.redback.struts2.action;

/*
 * Copyright 2005-2006 The Codehaus.
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

import org.codehaus.plexus.redback.policy.PasswordEncoder;
import org.codehaus.plexus.redback.policy.PasswordRuleViolationException;
import org.codehaus.plexus.redback.policy.PasswordRuleViolations;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;

/**
 * PasswordAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-password" )
@Scope( "prototype" )
public class PasswordAction
    extends AbstractSecurityAction
    implements CancellableAction
{
    // ------------------------------------------------------------------
    // Plexus Component Requirements
    // ------------------------------------------------------------------

    protected static final String CHANGE_PASSWORD_SUCCESS = "security-change-password-success";

    /**
     *
     */
    @Inject
    protected SecuritySystem securitySystem;

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private String existingPassword;

    private String newPassword;

    private String newPasswordConfirm;

    private String targetUrl;

    private boolean provideExisting;

    public String show()
    {
        provideExisting = StringUtils.isNotEmpty( getSecuritySession().getUser().getEncodedPassword() );
        return INPUT;
    }

    public String submit()
    {
        final SecuritySession securitySession = getSecuritySession();

        provideExisting = StringUtils.isNotEmpty( securitySession.getUser().getEncodedPassword() );

        if ( StringUtils.isEmpty( newPassword ) )
        {
            addFieldError( "newPassword", getText( "newPassword.cannot.be.empty" ) );
        }

        if ( !StringUtils.equals( newPassword, newPasswordConfirm ) )
        {
            addFieldError( "newPassword", getText( "password.confimation.failed" ) );
        }

        User user = securitySession.getUser();

        // Test existing Password.
        PasswordEncoder encoder = securitySystem.getPolicy().getPasswordEncoder();

        if ( provideExisting )
        {
            if ( !encoder.isPasswordValid( user.getEncodedPassword(), existingPassword ) )
            {
                addFieldError( "existingPassword", getText( "password.provided.does.not.match.existing" ) );
            }
        }

        // Validate the Password.
        try
        {
            User tempUser = securitySystem.getUserManager().createUser( "temp", "temp", "temp" );
            tempUser.setPassword( newPassword );
            securitySystem.getPolicy().validatePassword( tempUser );
        }
        catch ( PasswordRuleViolationException e )
        {
            PasswordRuleViolations violations = e.getViolations();

            if ( violations != null )
            {
                for ( String violation : violations.getLocalizedViolations() )
                {
                    addFieldError( "newPassword", violation );
                }
            }
        }

        // Toss error (if any exists)
        if ( hasActionErrors() || hasFieldErrors() || hasActionMessages() )
        {
            newPassword = "";
            newPasswordConfirm = "";
            existingPassword = "";
            return ERROR;
        }

        // We can save the new password.
        try
        {
            String encodedPassword = encoder.encodePassword( newPassword );
            user.setEncodedPassword( encodedPassword );
            user.setPassword( newPassword );
            // TODO: (address this) check once more for password policy, some policies may require additional information
            // only available in the actual user object, perhaps the thing to do is add a deep cloning mechanism
            // to user so we can validate this with a test user.  Its ok to just set and test it here before 
            // setting the updateUser, but logically its better to maintain a clear separation here
            securitySystem.getPolicy().validatePassword( user );
            securitySystem.getUserManager().updateUser( user );
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "cannot.update.user.not.found", Arrays.asList( (Object) user.getUsername() ) ) );
            addActionError( getText( "admin.deleted.account" ) );

            return ERROR;
        }
        catch ( PasswordRuleViolationException e )
        {
            PasswordRuleViolations violations = e.getViolations();

            if ( violations != null )
            {
                for ( String violation : violations.getLocalizedViolations() )
                {
                    addFieldError( "newPassword", violation );
                }
            }
            // [REDBACK-30] when the password is one of the previous 6, it throws exception here, but since the user
            // object is in the session we need to clear out the encodedPassword otherwise the flow will change and think
            // it needs to have existingPassword which isn't set on some reset password checks
            if ( !provideExisting )
            {
                user.setEncodedPassword( "" );
                user.setPassword( "" );
            }

            return ERROR;
        }

        log.info( "Password Change Request Success." );
        String currentUser = getCurrentUser();
        AuditEvent event = new AuditEvent( getText( "log.password.change" ) );
        event.setAffectedUser( user.getUsername() );
        event.setCurrentUser( currentUser );
        event.log();

        if ( !securitySession.isAuthenticated() )
        {
            log.debug( "User is not authenticated." );
            return REQUIRES_AUTHENTICATION;
        }

        /*
        *  If provide existing is true, then this was a normal password change flow, if it is
        * false then it is changing the password from the registration flow in which case direct to
         * external link
         */
        if ( !provideExisting )
        {
            return CHANGE_PASSWORD_SUCCESS;
        }
        else
        {

            if ( super.session != null )
            {

                Map<String, Object> map = (Map<String, Object>) super.session;
                String url = "";
                if ( map.containsKey( "targetUrl" ) )
                {
                    url = map.remove( "targetUrl" ).toString();
                    log.info( "targetUrl is retrieved and removed from the session: {}", url );
                }
                else
                {
                    log.info( "targetUrl is empty, redirect to change password success page" );
                    return CHANGE_PASSWORD_SUCCESS;
                }
                setTargetUrl( url );
            }
            return SUCCESS;
        }
    }

    public String cancel()
    {
        return CANCEL;
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public String getExistingPassword()
    {
        return existingPassword;
    }

    public void setExistingPassword( String existingPassword )
    {
        this.existingPassword = existingPassword;
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword( String newPassword )
    {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirm()
    {
        return newPasswordConfirm;
    }

    public void setNewPasswordConfirm( String newPasswordConfirm )
    {
        this.newPasswordConfirm = newPasswordConfirm;
    }

    public boolean isProvideExisting()
    {
        return provideExisting;
    }

    public void setProvideExisting( boolean provideExisting )
    {
        // Do nothing.
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        return SecureActionBundle.AUTHONLY;
    }

    public String getTargetUrl()
    {
        return targetUrl;
    }

    public void setTargetUrl( String targetUrl )
    {
        this.targetUrl = targetUrl;
    }
}
