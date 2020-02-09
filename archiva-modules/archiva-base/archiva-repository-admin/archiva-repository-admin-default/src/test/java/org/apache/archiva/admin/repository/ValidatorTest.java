package org.apache.archiva.admin.repository;
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

import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

/**
 * @author Eric Barboni
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
public  class ValidatorTest
    extends AbstractRepositoryAdminTest
{
    @Test
    public void testGenericValidator() 
    {
        Pattern pattern = Pattern.compile( ManagedRepositoryAdmin.REPOSITORY_LOCATION_VALID_EXPRESSION );
        // Checks only the pattern
        assertFalse("A repo location cannot contains space",pattern.matcher( "/opt/ testme/" ).matches());
        
    }
}
