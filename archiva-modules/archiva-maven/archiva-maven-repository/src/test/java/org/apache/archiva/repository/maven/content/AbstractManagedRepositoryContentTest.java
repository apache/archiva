package org.apache.archiva.repository.maven.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.LayoutException;

import static org.junit.Assert.fail;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class AbstractManagedRepositoryContentTest extends AbstractRepositoryContentTest
{

    @Override
    protected void assertBadPathCi( String path, String reason )
    {
        super.assertBadPathCi( path, reason );
        try
        {
            getManaged().toItem( path );
            fail(
                "Should have thrown a LayoutException on the invalid path [" + path + "] because of [" + reason + "]" );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }
}
