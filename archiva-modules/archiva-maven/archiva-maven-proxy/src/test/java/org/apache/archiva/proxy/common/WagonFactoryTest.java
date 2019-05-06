package org.apache.archiva.proxy.common;

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

import junit.framework.TestCase;
import org.apache.archiva.proxy.maven.WagonFactory;
import org.apache.archiva.proxy.maven.WagonFactoryRequest;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.maven.wagon.Wagon;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;

/**
 * Test the WagonFactory works through Spring to be bound into the RepositoryProxyHandler implementation.
 */
@RunWith ( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration ( locations = { "classpath*:/META-INF/spring-context.xml" } )
public class WagonFactoryTest
    extends TestCase
{

    @Inject
    WagonFactory factory;

    @Test
    public void testLookupSuccessiveWagons()
        throws Exception
    {

        Wagon first = factory.getWagon( new org.apache.archiva.proxy.maven.WagonFactoryRequest().protocol( "wagon#file" ) );

        Wagon second = factory.getWagon( new org.apache.archiva.proxy.maven.WagonFactoryRequest().protocol( "wagon#file" ) );

        // ensure we support only protocol name too
        Wagon third = factory.getWagon( new WagonFactoryRequest().protocol( "file" ) );

        assertNotSame( first, second );

        assertNotSame( first, third );
    }
}
