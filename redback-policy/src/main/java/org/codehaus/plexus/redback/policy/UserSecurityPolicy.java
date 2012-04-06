package org.codehaus.plexus.redback.policy;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;

import java.util.List;

/**
 * User Security Policy Settings.
 *
 * @version $Id$
 * @todo roll password management into it's own object.
 */
public interface UserSecurityPolicy
{
    /**
     * Get identifying string for the User Security Policy implementation.
     *
     * @return the id for the security policy implementation.
     */
    String getId();

    // ----------------------------------------------------------------------
    // Password Management
    // ----------------------------------------------------------------------

    /**
     * Gets the password encoder to use.
     *
     * @return the PasswordEncoder implementation to use.
     */
    PasswordEncoder getPasswordEncoder();

    /**
     * Add a Specific Rule to the Password Rules List.
     *
     * @param rule the rule to add.
     */
    void addPasswordRule( PasswordRule rule );

    /**
     * Get the Password Rules List.
     *
     * @return the list of {@link PasswordRule} objects.
     */
    List<PasswordRule> getPasswordRules();

    /**
     * Set the Password Rules List.
     *
     * @param rules the list of {@link PasswordRule} objects.
     */
    void setPasswordRules( List<PasswordRule> rules );

    /**
     * Gets the count of Previous Passwords that should be tracked.
     *
     * @return the count of previous passwords to track.
     */
    int getPreviousPasswordsCount();

    /**
     * Sets the count of previous passwords that should be tracked.
     *
     * @param count the count of previous passwords to track.
     */
    void setPreviousPasswordsCount( int count );

    /**
     * Gets the count of login attempts to allow.
     *
     * @return the count of login attempts to allow.
     */
    int getLoginAttemptCount();

    /**
     * Sets the count of login attempts to allow.
     *
     * @param count the count of login attempts to allow.
     */
    void setLoginAttemptCount( int count );

    /**
     * Get the Validation Settings.
     *
     * @return the validation settings.
     */
    UserValidationSettings getUserValidationSettings();

    /**
     * Set the Validation Settings.
     *
     * @param settings the settings.
     */
    void setUserValidationSettings( UserValidationSettings settings );

    /**
     * Get the Single Sign On Settings.
     *
     * @return the single sign on settings.
     */
    CookieSettings getSignonCookieSettings();

    /**
     * Get the Remember Me Settings.
     *
     * @return the remember me settings.
     */
    CookieSettings getRememberMeCookieSettings();

    /**
     * Enable the policies or not.
     * <p/>
     * Useful in code when application startup or application init is being performed.
     *
     * @param enabled true if enabled.
     */
    void setEnabled( boolean enabled );

    /**
     * Determines if the policies are enabled or not.
     *
     * @return true if enabled.
     */
    boolean isEnabled();

    /**
     * Sets the policy of how long a password will be valid until it expires.
     *
     * @param passwordExpiry the number of days until a password expires. (or -1 to disable)
     */
    void setPasswordExpirationDays( int passwordExpiry );

    /**
     * Gets the policy of how long a password will be valid until it expires.
     *
     * @return the number of days until a password expires. (or -1 for disabled)
     */
    int getPasswordExpirationDays();

    /**
     * Gets a list of accounts which should never be locked by security policy
     * @return accounts that should never be locked
     */
    List<String> getUnlockableAccounts();

    /**
     * Sets a list of accounts which should never be locked by security policy
     * @param unlockableAccounts
     */
    void setUnlockableAccounts(List<String> unlockableAccounts);

    /**
     * Extension Point - Change the password of a user.
     * <p/>
     * This method does not check if a user is allowed to change his/her password.
     * Any kind of authorization checks for password change allowed on guest or
     * anonymous users needs to occur before calling this method.
     * <p/>
     * This method does not persist the newly changed user password.
     * That will require a call to {@link UserManager#updateUser(User)}.
     *
     * @param user the user password to validate, remember, and encode.
     * @throws PasswordRuleViolationException if the new password violates the password rules
     */
    void extensionChangePassword( User user )
        throws PasswordRuleViolationException;

    void extensionChangePassword( User user, boolean passwordChangeRequired )
        throws PasswordRuleViolationException;

    /**
     * Extension Point - Test User for Password Expiration.
     *
     * @param user the user to test password expiration against.
     * @throws MustChangePasswordException if the password has expired
     */
    void extensionPasswordExpiration( User user )
        throws MustChangePasswordException;

    /**
     * Extension Point - Test if user has excessive logins
     *
     * @param user the user to test excessive logins against.
     * @throws AccountLockedException if the number of logins was exceeded
     */
    void extensionExcessiveLoginAttempts( User user )
        throws AccountLockedException;

    /**
     * Validate the incoming {@link User#getPassword()} against the specified
     * PasswordRules.
     *
     * @param user the user to validate.
     * @throws PasswordRuleViolationException if the password is not valid
     */
    void validatePassword( User user )
        throws PasswordRuleViolationException;
}
