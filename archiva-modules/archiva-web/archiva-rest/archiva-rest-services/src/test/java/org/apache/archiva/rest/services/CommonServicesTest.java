package org.apache.archiva.rest.services;
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

import org.apache.archiva.rest.api.services.CommonServices;
import org.junit.Test;

/**
 * @author Olivier Lamy
 */
public class CommonServicesTest
    extends AbstractArchivaRestTest
{
    @Test
    public void validCronExpression()
        throws Exception
    {
        CommonServices commonServices = getCommonServices( null );
        assertTrue( commonServices.validateCronExpression( "0 0,30 * * * ?" ) );
    }

    @Test
    public void nonValidCronExpression()
        throws Exception
    {
        CommonServices commonServices = getCommonServices( null );
        assertFalse( commonServices.validateCronExpression( "0,30 * * * ?" ) );
    }
}
