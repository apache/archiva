package org.apache.archiva.web.rss;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.TokenManager;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserManagerListener;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.UserQuery;
import org.apache.archiva.redback.users.jpa.model.JpaUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SecuritySystem stub used for testing.
 */
public class SecuritySystemStub
    implements SecuritySystem
{
    Map<String, String> users = new HashMap<>();

    List<String> repoIds = new ArrayList<>();

    public SecuritySystemStub()
    {
        users.put( "user1", "password1" );
        users.put( "user2", "password2" );
        users.put( "user3", "password3" );

        repoIds.add( "test-repo" );
    }

    @Override
    public SecuritySession authenticate( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        AuthenticationResult result = null;
        SecuritySession session = null;

        if ( users.get( source.getUsername() ) != null )
        {
            result = new AuthenticationResult( true, source.getUsername(), null );

            User user = new JpaUser();
            user.setUsername( source.getUsername() );
            user.setPassword( users.get( source.getUsername() ) );

            session = new DefaultSecuritySession( result, user );
        }
        else
        {
            result = new AuthenticationResult( false, source.getUsername(), null );
            session = new DefaultSecuritySession( result );
        }
        return session;
    }

    @Override
    public AuthorizationResult authorize( SecuritySession arg0, String arg1 )
        throws AuthorizationException
    {
        return null;
    }

    @Override
    public AuthorizationResult authorize( SecuritySession arg0, String permission, String repositoryId )
        throws AuthorizationException
    {

        AuthorizationResult result = new AuthorizationResult( this.repoIds.contains( repositoryId ), permission, null );
        return result;
    }

    @Override
    public AuthorizationResult authorize( User user, String permission, String resource )
        throws AuthorizationException
    {
        return null;
    }

    public String getAuthenticatorId()
    {
        return null;
    }

    public String getAuthorizerId()
    {
        return null;
    }

    @Override
    public KeyManager getKeyManager()
    {
        return null;
    }

    @Override
    public UserSecurityPolicy getPolicy()
    {
        return null;
    }

    public String getUserManagementId()
    {
        return null;
    }

    @Override
    public UserManager getUserManager()
    {
        return new UserManager()
        {

            @Override
            public String getDescriptionKey()
            {
                return "French wine is better than Australian wine !";
            }

            @Override
            public boolean isFinalImplementation()
            {
                return false;
            }

            @Override
            public void initialize()
            {
                // no op
            }

            @Override
            public boolean isReadOnly()
            {
                return false;
            }

            @Override
            public String getId()
            {
                return null;
            }

            @Override
            public void addUserManagerListener( UserManagerListener listener )
            {
                // no op
            }

            @Override
            public void removeUserManagerListener( UserManagerListener listener )
            {
                // no op
            }

            @Override
            public User createUser( String username, String fullName, String emailAddress )
            {
                return null;
            }

            @Override
            public User createGuestUser()
            {
                return new User()
                {

                    @Override
                    public String getUsername()
                    {
                        return "guest";
                    }

                    @Override
                    public void setUsername( String name )
                    {

                    }

                    @Override
                    public String getFullName()
                    {
                        return null;
                    }

                    @Override
                    public void setFullName( String name )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getEmail()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setEmail( String address )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPassword( String rawPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getEncodedPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public Date getLastPasswordChange()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setLastPasswordChange( Date passwordChangeDate )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public List<String> getPreviousEncodedPasswords()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPreviousEncodedPasswords( List<String> encodedPasswordList )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void addPreviousEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isPermanent()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPermanent( boolean permanent )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isLocked()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setLocked( boolean locked )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isPasswordChangeRequired()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPasswordChangeRequired( boolean changeRequired )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isValidated()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setValidated( boolean valid )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public int getCountFailedLoginAttempts()
                    {
                        return 0;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setCountFailedLoginAttempts( int count )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public Date getAccountCreationDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setAccountCreationDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public Date getLastLoginDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setLastLoginDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getUserManagerId()
                    {
                        return "mock";
                    }
                };
            }

            @Override
            public UserQuery createUserQuery()
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<User> getUsers()
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<User> getUsers( boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public User addUser( User user )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public User updateUser( User user )
                throws UserNotFoundException
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public User findUser( String username )
                throws UserNotFoundException
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public User findUser( String username, boolean useCache )
                throws UserNotFoundException, UserManagerException
            {
                return null;
            }

            @Override
            public User getGuestUser()
                throws UserNotFoundException
            {
                return new User()
                {

                    @Override
                    public String getUsername()
                    {
                        return "guest";
                    }

                    @Override
                    public void setUsername( String name )
                    {

                    }

                    @Override
                    public String getFullName()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setFullName( String name )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getEmail()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setEmail( String address )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPassword( String rawPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getEncodedPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public Date getLastPasswordChange()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setLastPasswordChange( Date passwordChangeDate )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public List<String> getPreviousEncodedPasswords()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPreviousEncodedPasswords( List<String> encodedPasswordList )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void addPreviousEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isPermanent()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPermanent( boolean permanent )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isLocked()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setLocked( boolean locked )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isPasswordChangeRequired()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setPasswordChangeRequired( boolean changeRequired )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public boolean isValidated()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setValidated( boolean valid )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public int getCountFailedLoginAttempts()
                    {
                        return 0;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setCountFailedLoginAttempts( int count )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public Date getAccountCreationDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setAccountCreationDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public Date getLastLoginDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void setLastLoginDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public String getUserManagerId()
                    {
                        return "mock";
                    }
                };
            }

            @Override
            public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<User> findUsersByQuery( UserQuery query )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean userExists( String principal )
            {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void deleteUser( String username )
                throws UserNotFoundException
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void addUserUnchecked( User user )
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void eraseDatabase()
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public User updateUser( User user, boolean passwordChangeRequired )
                throws UserNotFoundException
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    @Override
    public boolean isAuthenticated( AuthenticationDataSource arg0 )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        return false;
    }

    @Override
    public boolean isAuthorized( SecuritySession arg0, String arg1 )
        throws AuthorizationException
    {
        return false;
    }

    @Override
    public boolean isAuthorized( SecuritySession arg0, String arg1, String arg2 )
        throws AuthorizationException
    {
        if ( repoIds.contains( arg2 ) )
        {
            return true;
        }

        return false;
    }

    @Override
    public boolean userManagerReadOnly()
    {
        return true;
    }

    @Override
    public TokenManager getTokenManager() {
        return null;
    }
}
