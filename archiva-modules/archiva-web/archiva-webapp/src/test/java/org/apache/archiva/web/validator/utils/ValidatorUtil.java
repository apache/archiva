package org.apache.archiva.web.validator.utils;

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

import junit.framework.Assert;
import org.apache.commons.lang.SystemUtils;

import java.util.List;
import java.util.Map;

public class ValidatorUtil
{
    public static void assertFieldErrors( Map<String, List<String>> expectedFieldErrors,
                                          Map<String, List<String>> actualFieldErrors )
    {
        if ( expectedFieldErrors != null )
        {
            Assert.assertNotNull( actualFieldErrors );
            // checks the number of field errors
            Assert.assertEquals(
                "expected " + expectedFieldErrors + SystemUtils.LINE_SEPARATOR + ", found " + actualFieldErrors,
                expectedFieldErrors.size(), actualFieldErrors.size() );

            // check every content of the field error
            for ( Map.Entry<String, List<String>> expectedEntry : expectedFieldErrors.entrySet() )
            {
                if ( expectedEntry.getValue() != null )
                {
                    Assert.assertNotNull( "actual with key " + expectedEntry.getKey() + " is null",
                                          actualFieldErrors.get( expectedEntry.getKey() ) );
                    // checks the error message count per error field
                    Assert.assertEquals( expectedEntry.getValue().size(),
                                         actualFieldErrors.get( expectedEntry.getKey() ).size() );

                    // check the contents of error messages per field error
                    for ( int i = 0; i < expectedEntry.getValue().size(); i++ )
                    {
                        String expected = expectedEntry.getValue().get( i );
                        String actual = actualFieldErrors.get( expectedEntry.getKey() ).get( i );
                        Assert.assertEquals( expected, actual );
                    }
                }
                else
                {
                    Assert.assertNull( actualFieldErrors.get( expectedEntry.getKey() ) );
                }
            }
        }
        else
        {
            Assert.assertNull( actualFieldErrors );
        }
    }
}
