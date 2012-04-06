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
import org.codehaus.plexus.redback.tests.AbstractRbacManagerPerformanceTestCase;
import org.junit.After;
import org.junit.Before;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * CachedRbacManagerPerformanceTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CachedRbacManagerPerformanceTest
    extends AbstractRbacManagerPerformanceTestCase
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
        super.setUp();

        RBACManager store = rbacManager;
        assertTrue( store instanceof CachedRbacManager );
        setRbacManager( store );
    }

    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }
}
