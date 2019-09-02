package org.apache.archiva.security;

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

import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * DefaultUserRepositoriesTest
 *
 *
 */
public class DefaultUserRepositoriesTest
    extends AbstractSecurityTest
{


    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        restoreGuestInitialValues( USER_ALPACA );
        restoreGuestInitialValues( USER_GUEST );
        restoreGuestInitialValues( USER_ADMIN );
    }

    @Test
    public void testGetObservableRepositoryIds()
        throws Exception
    {
        // create some users.
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );

        assertEquals( "Expected users", 3, securitySystem.getUserManager().getUsers().size() );

        // some unassigned repo observer roles.
        setupRepository( "central" );
        setupRepository( "corporate" );
        setupRepository( "internal" );
        setupRepository( "snapshots" );
        setupRepository( "secret" );

        // some assigned repo observer roles.
        assignRepositoryObserverRole( USER_ALPACA, "corporate" );
        assignRepositoryObserverRole( USER_ALPACA, "central" );
        assignRepositoryObserverRole( USER_GUEST, "corporate" );
        // the global repo observer role.
        assignGlobalRepositoryObserverRole( USER_ADMIN );

        assertRepoIds( new String[]{ "central", "corporate" }, userRepos.getObservableRepositoryIds( USER_ALPACA ) );
        assertRepoIds( new String[]{ "coporate" }, userRepos.getObservableRepositoryIds( USER_GUEST ) );
        assertRepoIds( new String[]{ "central", "internal","corporate", "snapshots", "secret" },
                       userRepos.getObservableRepositoryIds( USER_ADMIN ) );

    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        restoreGuestInitialValues( USER_ALPACA );
        restoreGuestInitialValues( USER_GUEST );
        restoreGuestInitialValues( USER_ADMIN );
    }

    private void assertRepoIds( String[] expectedRepoIds, List<String> observableRepositoryIds )
    {
        assertNotNull( "Observable Repository Ids cannot be null.", observableRepositoryIds );

        if ( expectedRepoIds.length != observableRepositoryIds.size() )
        {
            fail( "Size of Observable Repository Ids wrong, expected <" + expectedRepoIds.length + "> but got <"
                      + observableRepositoryIds.size() + "> instead. \nExpected: ["
                      + StringUtils.join( expectedRepoIds, "," ) + "]\nActual: ["
                      + StringUtils.join( observableRepositoryIds.iterator(), "," ) + "]" );
        }
    }

    private void assignGlobalRepositoryObserverRole( String principal )
        throws Exception
    {
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_GLOBAL_REPOSITORY_OBSERVER, principal );
    }
}
