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
package org.apache.maven.archiva.xmlrpc.security;

import junit.framework.TestCase;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.XmlRpcRequestConfig;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.keys.KeyManager;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;

public class XmlRpcAuthenticatorTest
    extends TestCase
{
    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    public void testAuthentication()
        throws Exception
    {
        MockSecuritySystem securitySystem = new MockSecuritySystem( true, true, USERNAME, PASSWORD );
        XmlRpcAuthenticator authenticator = new XmlRpcAuthenticator( securitySystem );
        MockXmlRpcRequest request = new MockXmlRpcRequest( USERNAME, PASSWORD );

        assertTrue( authenticator.isAuthorized( request ) );
    }

    class MockXmlRpcRequest
        implements XmlRpcRequest
    {
        private final XmlRpcHttpRequestConfigImpl configImpl;

        public MockXmlRpcRequest( String username, String password )
        {
            configImpl = new XmlRpcHttpRequestConfigImpl();
            configImpl.setBasicUserName( username );
            configImpl.setBasicPassword( password );
        }

        public XmlRpcRequestConfig getConfig()
        {
            return configImpl;
        }

        public String getMethodName()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public Object getParameter( int pIndex )
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public int getParameterCount()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }
    }

    class MockSecuritySystem
        implements SecuritySystem
    {
        private final boolean authorized;

        private final boolean authenticate;

        private final String username;

        private final String password;

        public MockSecuritySystem( boolean authorized, boolean authenticate, String username, String password )
        {
            this.authorized = authorized;
            this.authenticate = authenticate;
            this.username = username;
            this.password = password;
        }

        public SecuritySession authenticate( AuthenticationDataSource dataSource )
            throws AuthenticationException, UserNotFoundException, AccountLockedException
        {
            return new SecuritySession()
            {

                public AuthenticationResult getAuthenticationResult()
                {
                    throw new UnsupportedOperationException( "Not supported yet." );
                }

                public User getUser()
                {
                    throw new UnsupportedOperationException( "Not supported yet." );
                }

                public boolean isAuthenticated()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public AuthorizationResult authorize( SecuritySession session, Object arg1 )
            throws AuthorizationException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public AuthorizationResult authorize( SecuritySession session, Object arg1, Object arg2 )
            throws AuthorizationException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public String getAuthenticatorId()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public String getAuthorizerId()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public KeyManager getKeyManager()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public UserSecurityPolicy getPolicy()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public String getUserManagementId()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public UserManager getUserManager()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public boolean isAuthenticated( AuthenticationDataSource dataSource )
            throws AuthenticationException, UserNotFoundException, AccountLockedException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public boolean isAuthorized( SecuritySession session, Object arg1 )
            throws AuthorizationException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public boolean isAuthorized( SecuritySession session, Object arg1, Object arg2 )
            throws AuthorizationException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }
    }
}
