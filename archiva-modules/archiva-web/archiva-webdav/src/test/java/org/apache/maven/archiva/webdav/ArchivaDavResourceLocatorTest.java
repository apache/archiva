package org.apache.maven.archiva.webdav;

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

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class ArchivaDavResourceLocatorTest extends TestCase
{
    ArchivaDavLocatorFactory factory;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        factory = new ArchivaDavLocatorFactory();
    }

    public void testLocatorWithPrefixHref()
        throws Exception
    {
        String prefix = "http://myproxy/";
        String href = "/repository/internal";
        ArchivaDavResourceLocator locator = (ArchivaDavResourceLocator)factory.createResourceLocator(prefix, href);

        assertEquals("internal", locator.getRepositoryId());
        assertEquals("", locator.getWorkspaceName());
        assertEquals("", locator.getWorkspacePath());
        assertEquals("http://myproxy/", locator.getPrefix());
        assertEquals("http://myproxy/repository/internal", locator.getHref(false));
        assertEquals("http://myproxy/repository/internal/", locator.getHref(true));
        assertEquals("/repository/internal", locator.getResourcePath());
        assertEquals("/repository/internal", locator.getRepositoryPath());
    }

    public void testLocatorWithHrefThatContainsPrefix()
        throws Exception
    {
        String prefix = "http://myproxy/";
        String href = "http://myproxy/repository/internal";
        ArchivaDavResourceLocator locator = (ArchivaDavResourceLocator)factory.createResourceLocator(prefix, href);

        assertEquals("internal", locator.getRepositoryId());
        assertEquals("", locator.getWorkspaceName());
        assertEquals("", locator.getWorkspacePath());
        assertEquals("http://myproxy/", locator.getPrefix());
        assertEquals("http://myproxy/repository/internal", locator.getHref(false));
        assertEquals("http://myproxy/repository/internal/", locator.getHref(true));
        assertEquals("repository/internal", locator.getResourcePath());
        assertEquals("repository/internal", locator.getRepositoryPath());
    }

    public void testLocatorWithRootHref()
        throws Exception
    {
        String prefix = "http://myproxy/";
        String href = "/";
        ArchivaDavResourceLocator locator = (ArchivaDavResourceLocator)factory.createResourceLocator(prefix, href);

        assertEquals("", locator.getRepositoryId());
        assertEquals("", locator.getWorkspaceName());
        assertEquals("", locator.getWorkspacePath());
        assertEquals("http://myproxy/", locator.getPrefix());
        assertEquals("http://myproxy/", locator.getHref(false));
        assertEquals("http://myproxy/", locator.getHref(true));
        assertEquals("/", locator.getResourcePath());
        assertEquals("/", locator.getRepositoryPath());
    }
}
