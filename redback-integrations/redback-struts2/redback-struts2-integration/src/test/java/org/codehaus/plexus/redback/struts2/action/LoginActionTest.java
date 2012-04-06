package org.codehaus.plexus.redback.struts2.action;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashMap;

import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.DefaultUserSecurityPolicy;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.policy.UserValidationSettings;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.UserNotFoundException;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.XWorkTestCase;

public class LoginActionTest
    extends XWorkTestCase
{

    LoginAction action;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        action = new LoginAction();
        action.session = new HashMap<String, Object>();
    }

    public void testRedback265()
        throws SecurityException, NoSuchMethodException, AccountLockedException, MustChangePasswordException,
        AuthenticationException, UserNotFoundException
    {
        String principal = "authenticates_but_does_not_exist";

        // Setup authentication success, with no user found
        AuthenticationResult result = new AuthenticationResult( true, principal, null );
        SecuritySession session = new DefaultSecuritySession( result );
        UserSecurityPolicy policy = new DefaultUserSecurityPolicy();

        SecuritySystem system = createMock( SecuritySystem.class );
        UserValidationSettings validationSettings = createMock( UserValidationSettings.class );
        expect( system.authenticate( (AuthenticationDataSource) anyObject() ) ).andReturn( session );
        expect( system.getPolicy() ).andReturn( policy ).anyTimes();
        expect( validationSettings.isEmailValidationRequired() ).andReturn( true ).anyTimes();

        // Hook-up action to mock objects
        action.securitySystem = system;
        action.setUsername( principal );

        replay( system, validationSettings );

        String actionResult = action.login();

        verify( system, validationSettings );

        assertEquals( Action.ERROR, actionResult );
    }
}
