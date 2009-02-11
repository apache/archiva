package org.apache.archiva.repository.api;

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

import org.apache.archiva.repository.api.PathUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class PathUtilsTest
{
    @Test
    public void testRepositoryId() throws Exception
    {
        String path = "/myrepo/foo/bar";
        assertEquals("myrepo", PathUtils.getRepositoryId(path));

        path = "myrepo/foo/bar";
        assertEquals("myrepo", PathUtils.getRepositoryId(path));

        path = "/myrepo";
        assertEquals("myrepo", PathUtils.getRepositoryId(path));

        path = "mypath";
        assertEquals("mypath", PathUtils.getRepositoryId(path));

        path = "";
        assertNull(PathUtils.getRepositoryId(path));

        path = null;
        assertNull("", PathUtils.getRepositoryId(path));
    }

    @Test
    public void testLogicalPath() throws Exception
    {
        String path = "/helloworld/foo/bar/baz/";
        assertEquals("/foo/bar/baz", PathUtils.getLogicalPath(path));

        path = "/helloworld/foo/bar/baz";
        assertEquals("/foo/bar/baz", PathUtils.getLogicalPath(path));

        path = "helloworld/foo/bar/baz";
        assertEquals("/foo/bar/baz", PathUtils.getLogicalPath(path));

        path = "";
        assertEquals("/", PathUtils.getLogicalPath(path));

        path = null;
        assertEquals("/", PathUtils.getLogicalPath(path));
    }
}
