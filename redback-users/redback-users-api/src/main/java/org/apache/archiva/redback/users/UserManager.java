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

import java.util.List;

/**
 * User Manager Interface
 *
 * @author Jason van Zyl
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public interface UserManager
{
    
    static final String GUEST_USERNAME = "guest";

    /**
     * Is the UserManager read only?  if so then create and modify actions are to be disabled
     *
     * @return boolean true if user manager is disabled
     */
    boolean isReadOnly();

    /**
     * An Identifier for the UserManager.
     *
     * @return the user manager identifier.
     */
    String getId();

    /**
     * Add a {@link UserManagerListener} to track major events in the
     * UserManager.
     *
     * @param listener the listener to add.
     */
    void addUserManagerListener( UserManagerListener listener );

    /**
     * Remove a {@link UserManagerListener} from the collection of listeners.
     *
     * @param listener the listener to remove.
     */
    void removeUserManagerListener( UserManagerListener listener );

    /**
     * Factory method to create new User Objects based on provider specific
     * implementation.
     * <p/>
     * User objects created this way do not exist in the provider's underlying
     * data store until a call to {@link #addUser(User)} is made.
     *
     * @param username     the username for this user.
     * @param fullName     the full name for this user.
     * @param emailAddress the email address for this user.
     * @return the new user object ready to use.
     */
    User createUser( String username, String fullName, String emailAddress );

    /**
     * Factory method to create the guest user.
     *
     * @return The guest user
     */
    User createGuestUser();

    /**
     * Factory method to create {@link UserQuery}s based on provider specific
     * implementations.
     *
     * @return the provider implementation of UserQuery
     */
    UserQuery createUserQuery();

    /**
     * Get the List of {@link User} objects.
     *
     * @return the List of {@link User} Objects.
     */
    List<User> getUsers();

    List<User> getUsers( boolean orderAscending );

    /**
     * Add a User.
     *
     * @param user the user to add.
     * @return the user that was just added.
     */
    User addUser( User user );

    /**
     * Update a User.
     *
     * @param user the user to update.
     * @return the user that was just updated.
     * @throws UserNotFoundException if the user was not found to update.
     */
    User updateUser( User user )
        throws UserNotFoundException;

    /**
     * Find a User using a User name.
     *
     * @param username the username to find.
     * @return the user.
     * @throws UserNotFoundException if the user was not found.
     */
    User findUser( String username )
        throws UserNotFoundException;

    /**
     * Get the guest user.
     *
     * @return the guest user.
     */
    User getGuestUser()
        throws UserNotFoundException;

    List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending );

    List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending );

    List<User> findUsersByEmailKey( String emailKey, boolean orderAscending );

    /**
     * Find users matching properties, ordering and range as specified by the
     * {@link UserQuery}.
     *
     * @param query the query.
     * @return a List of {@link User} objects.
     */
    List<User> findUsersByQuery( UserQuery query );

    /**
     * Find a User using the principal.
     *
     * @param principal the principal to look for.
     * @return the user.
     * @throws UserNotFoundException if the user was not found.
     */
    User findUser( Object principal )
        throws UserNotFoundException;

    /**
     * true if the user exists, false if it doesn't
     *
     * @param principal
     * @return true, if user exists
     */
    boolean userExists( Object principal );

    /**
     * Delete a user using the principal.
     *
     * @param principal the principal to look for.
     * @throws UserNotFoundException the user was not found.
     */
    void deleteUser( Object principal )
        throws UserNotFoundException;

    /**
     * Delete a user using the username.
     *
     * @param username the username to look for.
     * @throws UserNotFoundException the user was not found.
     */
    void deleteUser( String username )
        throws UserNotFoundException;

    /**
     * Add a user to the database without checking for consistency or adjusting the password. Should only be used for
     * re-importing known-good data.
     *
     * @param user the user to add
     */
    void addUserUnchecked( User user );

    void eraseDatabase();

    User updateUser( User user, boolean passwordChangeRequired )
        throws UserNotFoundException;


    /**
     * consumer of user manager can use it to reload various configuration
     * with the configurable implementation is possible to change dynamically the real implementation used.
     * @since 2.1
     */
    void initialize();
}
