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
import org.apache.archiva.redback.users.jdo.JdoUser;

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
    Map<String, String> users = new HashMap<String, String>();

    List<String> repoIds = new ArrayList<>();

    public SecuritySystemStub()
    {
        users.put( "user1", "password1" );
        users.put( "user2", "password2" );
        users.put( "user3", "password3" );

        repoIds.add( "test-repo" );
    }

    public SecuritySession authenticate( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        AuthenticationResult result = null;
        SecuritySession session = null;

        if ( users.get( source.getUsername() ) != null )
        {
            result = new AuthenticationResult( true, source.getUsername(), null );

            User user = new JdoUser();
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

    public AuthorizationResult authorize( SecuritySession arg0, String arg1 )
        throws AuthorizationException
    {
        return null;
    }

    public AuthorizationResult authorize( SecuritySession arg0, String arg1, String arg2 )
        throws AuthorizationException
    {
        AuthorizationResult result = new AuthorizationResult( true, arg1, null );

        return result;
    }

    public String getAuthenticatorId()
    {
        return null;
    }

    public String getAuthorizerId()
    {
        return null;
    }

    public KeyManager getKeyManager()
    {
        return null;
    }

    public UserSecurityPolicy getPolicy()
    {
        return null;
    }

    public String getUserManagementId()
    {
        return null;
    }

    public UserManager getUserManager()
    {
        return new UserManager()
        {

            public String getDescriptionKey()
            {
                return "French wine is better than Australian wine !";
            }

            public boolean isFinalImplementation()
            {
                return false;
            }

            public void initialize()
            {
                // no op
            }

            public boolean isReadOnly()
            {
                return false;
            }

            public String getId()
            {
                return null;
            }

            public void addUserManagerListener( UserManagerListener listener )
            {
                // no op
            }

            public void removeUserManagerListener( UserManagerListener listener )
            {
                // no op
            }

            public User createUser( String username, String fullName, String emailAddress )
            {
                return null;
            }

            public User createGuestUser()
            {
                return new User()
                {

                    public String getUsername()
                    {
                        return "guest";
                    }

                    public void setUsername( String name )
                    {

                    }

                    public String getFullName()
                    {
                        return null;
                    }

                    public void setFullName( String name )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getEmail()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setEmail( String address )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPassword( String rawPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getEncodedPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Date getLastPasswordChange()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setLastPasswordChange( Date passwordChangeDate )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public List<String> getPreviousEncodedPasswords()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPreviousEncodedPasswords( List<String> encodedPasswordList )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void addPreviousEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isPermanent()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPermanent( boolean permanent )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isLocked()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setLocked( boolean locked )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isPasswordChangeRequired()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPasswordChangeRequired( boolean changeRequired )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isValidated()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setValidated( boolean valid )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public int getCountFailedLoginAttempts()
                    {
                        return 0;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setCountFailedLoginAttempts( int count )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Date getAccountCreationDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setAccountCreationDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Date getLastLoginDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setLastLoginDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getUserManagerId()
                    {
                        return "mock";
                    }
                };
            }

            public UserQuery createUserQuery()
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public List<User> getUsers()
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public List<User> getUsers( boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public User addUser( User user )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public User updateUser( User user )
                throws UserNotFoundException
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

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

            public User getGuestUser()
                throws UserNotFoundException
            {
                return new User()
                {

                    public String getUsername()
                    {
                        return "guest";
                    }

                    public void setUsername( String name )
                    {

                    }

                    public String getFullName()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setFullName( String name )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getEmail()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setEmail( String address )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPassword( String rawPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getEncodedPassword()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Date getLastPasswordChange()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setLastPasswordChange( Date passwordChangeDate )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public List<String> getPreviousEncodedPasswords()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPreviousEncodedPasswords( List<String> encodedPasswordList )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void addPreviousEncodedPassword( String encodedPassword )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isPermanent()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPermanent( boolean permanent )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isLocked()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setLocked( boolean locked )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isPasswordChangeRequired()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setPasswordChangeRequired( boolean changeRequired )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public boolean isValidated()
                    {
                        return false;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setValidated( boolean valid )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public int getCountFailedLoginAttempts()
                    {
                        return 0;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setCountFailedLoginAttempts( int count )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Date getAccountCreationDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setAccountCreationDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Date getLastLoginDate()
                    {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public void setLastLoginDate( Date date )
                    {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getUserManagerId()
                    {
                        return "mock";
                    }
                };
            }

            public List<User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public List<User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public List<User> findUsersByEmailKey( String emailKey, boolean orderAscending )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public List<User> findUsersByQuery( UserQuery query )
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean userExists( String principal )
            {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void deleteUser( String username )
                throws UserNotFoundException
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void addUserUnchecked( User user )
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void eraseDatabase()
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public User updateUser( User user, boolean passwordChangeRequired )
                throws UserNotFoundException
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    public boolean isAuthenticated( AuthenticationDataSource arg0 )
        throws AuthenticationException, UserNotFoundException, AccountLockedException
    {
        return false;
    }

    public boolean isAuthorized( SecuritySession arg0, String arg1 )
        throws AuthorizationException
    {
        return false;
    }

    public boolean isAuthorized( SecuritySession arg0, String arg1, String arg2 )
        throws AuthorizationException
    {
        if ( repoIds.contains( arg2 ) )
        {
            return true;
        }

        return false;
    }

    public boolean userManagerReadOnly()
    {
        return true;
    }
}
