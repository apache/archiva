package org.apache.maven.archiva.web.repository;

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

import org.apache.maven.archiva.webdav.ArchivaDavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.DavException;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class UnauthenticatedDavSessionProvider extends ArchivaDavSessionProvider
{
    public UnauthenticatedDavSessionProvider(WebApplicationContext applicationContext)
    {
        super(applicationContext);
    }

    @Override
    protected boolean isAuthorized( WebdavRequest request, String repositoryId )
        throws DavException
    {
        return true;    
    }

    @Override
    protected boolean isAuthenticated( WebdavRequest request, String repositoryId )
        throws DavException
    {
        return true;
    }
}
