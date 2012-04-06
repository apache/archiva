package org.codehaus.plexus.redback.users.cached;

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

import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.provider.test.AbstractUserManagerTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * CachedUserManagerTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CachedUserManagerTest
    extends AbstractUserManagerTestCase
{

    @Inject @Named(value = "userManager#cached")
    UserManager userManager;


    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        setUserManager( userManager );

        assertTrue( getUserManager() instanceof CachedUserManager );
    }

    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    @AfterClass
    public static void cleanCache()
    {
        CacheManager.getInstance().removalAll();
    }
}
