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

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * UnauthenticatedDavResourceFactory
 *
 * @version $Id:
 */
@Service("archivaDavResourceFactory#unauthenticated")
public class UnauthenticatedDavResourceFactory
    extends ArchivaDavResourceFactory
{

    @Inject
    public UnauthenticatedDavResourceFactory( ApplicationContext applicationContext, PlexusSisuBridge plexusSisuBridge,
                                              ArchivaConfiguration archivaConfiguration )
        throws PlexusSisuBridgeException
    {
        super( applicationContext, plexusSisuBridge, archivaConfiguration );
    }

    @Override
    protected boolean isAuthorized( DavServletRequest request, String repositoryId )
        throws DavException
    {
        return true;
    }
}
