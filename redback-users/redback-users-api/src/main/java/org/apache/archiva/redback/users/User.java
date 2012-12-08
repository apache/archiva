package org.apache.archiva.redback.users;

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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * The User Object.
 *
 * @author Jason van Zyl
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public interface User
    extends Serializable
{

    // --------------------------------------------------------------------
    // Standard User Requirements.
    // --------------------------------------------------------------------

    /**
     * Gets the User Name for this user.
     * <p/>
     * This field is required, and should never be empty.
     *
     * @return the user name.
     */
    String getUsername();

    /**
     * Sets the User Name for this user.
     * <p/>
     * This field is required, and should never be empty.
     *
     * @param name the user name.
     */
    void setUsername( String name );

    /**
     * Gets the Full Name for this user.
     * <p/>
     * This field is required, and should never be empty.
     *
     * @return the full name.
     */
    String getFullName();

    /**
     * Sets the Full Name for this user.
     * <p/>
     * This field is required, and should never be empty.
     *
     * @param name the full name.
     */
    void setFullName( String name );

    /**
     * Gets the email address for this user.
     * <p/>
     * This field is required, and should never be empty.
     *
     * @return the email address.
     */
    String getEmail();

    /**
     * Sets the email address for this user.
     * <p/>
     * This field is required, and should never be empty.
     *
     * @param address the email address.
     */
    void setEmail( String address );

    // --------------------------------------------------------------------
    // Password Requirements.
    // --------------------------------------------------------------------

    /**
     * Gets the Raw (unencoded) Password.
     * Used only on password change requests.
     * <p/>
     * <p>
     * <b>Notes for User Providers</b>
     * </p>
     * <ol>
     * <li>
     * Providers need to look for a value in here to indicate if the user is
     * intending to change their password.
     * </li>
     * <li>
     * The providers of this interface need to use this field, encode the password, place it's value
     * into the encodedPassword field, and clear out the raw unencoded password field.
     * </li>
     * <li>
     * This field should never be stored on disk.
     * </li>
     * </ol>
     *
     * @return the raw encoded password.
     */
    String getPassword();

    /**
     * Sets the raw (unencoded) password for this user.
     *
     * @param rawPassword the raw unencoded password for this user.
     * @see #getPassword()
     */
    void setPassword( String rawPassword );

    /**
     * Gets the Encoded Password.
     *
     * @return the encoded password.
     */
    String getEncodedPassword();

    /**
     * Sets the Encoded Password.
     * <p/>
     * This field is populated by the {@link UserManager} process.
     *
     * @param encodedPassword
     */
    void setEncodedPassword( String encodedPassword );

    /**
     * Gets the Date of the Last Password Change.
     * <p/>
     * Used by password management policies to enforce password expiration rules.
     *
     * @return the date of the last password change.
     */
    Date getLastPasswordChange();

    /**
     * Sets the Last Password Change Date.
     * <p/>
     * This field is populated by the {@link UserManager} process.
     *
     * @param passwordChangeDate the date that the last password change occured.
     */
    void setLastPasswordChange( Date passwordChangeDate );

    /**
     * Gets the list of previous password (in encoded format).
     * <p/>
     * Used by password management policies to enforce password reuse rules.
     *
     * @return the list of {@link String} objects.  Represents previous passwords (in encoded format).
     */
    List<String> getPreviousEncodedPasswords();

    /**
     * Sets the list of previous passwords (in encoded format)
     *
     * @param encodedPasswordList (list of {@link String} objects.) the previously passwords in encoded format.
     */
    void setPreviousEncodedPasswords( List<String> encodedPasswordList );

    /**
     * Add encoded password to previously passwords in encoded format.
     *
     * @param encodedPassword the encoded password to add.
     */
    void addPreviousEncodedPassword( String encodedPassword );

    // --------------------------------------------------------------------
    // State
    // --------------------------------------------------------------------

    /**
     * Gets the flag indicating if this user is a permanent user or not.
     * <p/>
     * Usually Root / Admin / Guest users are flagged as such.
     */
    boolean isPermanent();

    /**
     * Sets the permanent flag for this user.
     * <p/>
     * Users such as Root / Admin / Guest are typically flagged as permanent.
     *
     * @param permanent true if permanent.
     */
    void setPermanent( boolean permanent );

    /**
     * Determines if this user account is locked from use or not.
     * <p/>
     * This state is set from an administrative point of view, or due to
     * excessive failed login attempts.
     *
     * @return true if account is locked.
     */
    boolean isLocked();

    /**
     * Sets the locked state of this account.
     *
     * @param locked true if account is to be locked.
     */
    void setLocked( boolean locked );

    /**
     * Determines if this user account must change their password on next login.
     *
     * @return true if user must change password on next login.
     */
    boolean isPasswordChangeRequired();

    /**
     * Sets the flag to indicate if this user must change their password on next login.
     *
     * @param changeRequired true if user must change password on next login.
     */
    void setPasswordChangeRequired( boolean changeRequired );

    /**
     * Gets the flag indicating if this user has been validated (or not)
     *
     * @return true if validated.
     */
    boolean isValidated();

    /**
     * Sets the flag indicating if this user has been validated (or not)
     *
     * @param valid true if validated.
     */
    void setValidated( boolean valid );

    // --------------------------------------------------------------------
    // Statistics
    // --------------------------------------------------------------------

    /**
     * Get Count of Failed Login Attempts.
     *
     * @return the count of failed login attempts.
     */
    int getCountFailedLoginAttempts();

    /**
     * Set the count of failed login attempts.
     *
     * @param count the count of failed login attempts.
     */
    void setCountFailedLoginAttempts( int count );

    /**
     * Get the Creation Date for this account.
     *
     * @return the date of creation for this account.
     */
    Date getAccountCreationDate();

    /**
     * Set the Creation Date for this account.
     */
    void setAccountCreationDate( Date date );

    /**
     * Get the Last Successful Login Date for this account.
     *
     * @return the date of the last successful login
     */
    Date getLastLoginDate();

    /**
     * Sets the Last Successful Login Date for this account.
     */
    void setLastLoginDate( Date date );

    /**
     * as we can user multiple userManagers implementation we must track from which one this one comes.
     * @since 2.1
     * @return userManager id
     */
    String getUserManagerId();

}
