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
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
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

    public FileMetadata post( String groupId, String artifactId, String version, String packaging, String classifier,
                              String repositoryId, String generatePom )
        throws ArchivaRestServiceException
    {
        log.info( "uploading file:" + groupId + ":" + artifactId + ":" + version );
        try
        {
            File file = File.createTempFile( "upload-artifact", "tmp" );
            file.deleteOnExit();
            IOUtils.copy( httpServletRequest.getInputStream(), new FileOutputStream( file ) );
            FileMetadata fileMetadata = new FileMetadata( "thefile", file.length(), "theurl" );
            fileMetadata.setDeleteUrl( file.getName() );
            return fileMetadata;
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }
    }

    public FileMetadata post( MultipartBody multipartBody )
        throws ArchivaRestServiceException
    {

        try
        {
            String groupId =
                IOUtils.toString( multipartBody.getAttachment( "groupId" ).getDataHandler().getInputStream() );
            String artifactId =
                IOUtils.toString( multipartBody.getAttachment( "artifactId" ).getDataHandler().getInputStream() );
            String version =
                IOUtils.toString( multipartBody.getAttachment( "version" ).getDataHandler().getInputStream() );
            String packaging =
                IOUtils.toString( multipartBody.getAttachment( "packaging" ).getDataHandler().getInputStream() );

            boolean generatePom = BooleanUtils.toBoolean(
                IOUtils.toString( multipartBody.getAttachment( "generatePom" ).getDataHandler().getInputStream() ) );

            String classifier =
                IOUtils.toString( multipartBody.getAttachment( "classifier" ).getDataHandler().getInputStream() );

            log.info( "uploading file:" + groupId + ":" + artifactId + ":" + version );
            Attachment file = multipartBody.getAttachment( "files[]" );
            File tmpFile = File.createTempFile( "upload-artifact", "tmp" );
            tmpFile.deleteOnExit();
            IOUtils.copy( file.getDataHandler().getInputStream(), new FileOutputStream( tmpFile ) );
            FileMetadata fileMetadata = new FileMetadata( "thefile", tmpFile.length(), "theurl" );
            fileMetadata.setDeleteUrl( tmpFile.getName() );
            return fileMetadata;
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
        }

    }

    public Boolean deleteFile( String fileName )
        throws ArchivaRestServiceException
    {
        File file = new File( SystemUtils.getJavaIoTmpDir(), fileName );
        log.debug( "delete file:{},exists:{}", file.getPath(), file.exists() );
        if ( file.exists() )
        {
            return file.delete();
        }
        return Boolean.FALSE;
    }
}
