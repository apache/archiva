package org.apache.archiva.webapp.ui.services.api;
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

import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.webapp.ui.services.model.FileMetadata;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author Olivier Lamy
 */
@Service( "fileUploadService#rest" )
public class DefaultFileUploadService
    implements FileUploadService
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    public FileMetadata post()
        throws ArchivaRestServiceException
    {
        log.info( "uploading file" );
        try
        {
            byte[] bytes = IOUtils.toByteArray( httpServletRequest.getInputStream() );
            return new FileMetadata( "thefile", bytes.length, "theurl" );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
    }
}
