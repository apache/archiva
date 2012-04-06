package org.codehaus.plexus.redback.rbac.cached;

/*
 * Copyright 2001-2006 The Codehaus.
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

import net.sf.ehcache.CacheManager;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.tests.AbstractRbacManagerTestCase;
import org.junit.Before;

import javax.inject.Inject;
import javax.inject.Named;

public class CachedRbacManagerTest
    extends AbstractRbacManagerTestCase
{

    @Inject
    @Named( value = "rBACManager#cached" )
    RBACManager rbacManager;

    /**
     * Creates a new RbacStore which contains no data.
     */
    @Before
    public void setUp()
        throws Exception
    {
        /*
        CacheManager.getInstance().removeCache( "usersCache" );
        CacheManager.getInstance().removalAll();
        CacheManager.getInstance().shutdown();
        */
        super.setUp();
        CacheManager.getInstance().clearAll();
        setRbacManager( rbacManager );

        assertTrue( getRbacManager() instanceof CachedRbacManager );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    @Override
    public void testStoreInitialization()
        throws Exception
    {
        CacheManager.getInstance().clearAll();
        rbacManager.eraseDatabase();
        //eventTracker.rbacInit( true );
        super.testStoreInitialization();
    }
}
