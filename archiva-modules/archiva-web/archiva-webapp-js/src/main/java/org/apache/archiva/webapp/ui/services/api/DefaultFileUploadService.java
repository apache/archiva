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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private String getStringValue( MultipartBody multipartBody, String attachmentId )
        throws IOException
    {
        Attachment attachment = multipartBody.getAttachment( attachmentId );
        return attachment == null ? "" : IOUtils.toString( attachment.getDataHandler().getInputStream() );
    }

    public FileMetadata post( MultipartBody multipartBody )
        throws ArchivaRestServiceException
    {

        try
        {
            String groupId = getStringValue( multipartBody, "groupId" );

            String artifactId = getStringValue( multipartBody, "artifactId" );

            String version = getStringValue( multipartBody, "version" );

            String packaging = getStringValue( multipartBody, "packaging" );

            String repositoryId = getStringValue( multipartBody, "repositoryId" );

            boolean generatePom = BooleanUtils.toBoolean( getStringValue( multipartBody, "generatePom" ) );

            String classifier = getStringValue( multipartBody, "classifier" );
            boolean pomFile = BooleanUtils.toBoolean( getStringValue( multipartBody, "pomFile" ) );

            Attachment file = multipartBody.getAttachment( "files[]" );

            //Content-Disposition: form-data; name="files[]"; filename="org.apache.karaf.features.command-2.2.2.jar"
            String fileName = file.getContentDisposition().getParameter( "filename" );

            File tmpFile = File.createTempFile( "upload-artifact", "tmp" );
            tmpFile.deleteOnExit();
            IOUtils.copy( file.getDataHandler().getInputStream(), new FileOutputStream( tmpFile ) );
            FileMetadata fileMetadata = new FileMetadata( fileName, tmpFile.length(), "theurl" );
            fileMetadata.setServerFileName( tmpFile.getName() );
            fileMetadata.setGroupId( groupId );
            fileMetadata.setArtifactId( artifactId );
            fileMetadata.setVersion( version );
            fileMetadata.setVersion( version );
            fileMetadata.setPackaging( packaging );
            fileMetadata.setGeneratePom( generatePom );
            fileMetadata.setClassifier( classifier );
            fileMetadata.setDeleteUrl( tmpFile.getName() );
            fileMetadata.setRepositoryId( repositoryId );
            fileMetadata.setPomFile( pomFile );

            log.info( "uploading file:{}", fileMetadata );

            List<FileMetadata> fileMetadatas =
                (List<FileMetadata>) httpServletRequest.getSession().getAttribute( FILES_SESSION_KEY );

            if ( fileMetadatas == null )
            {
                fileMetadatas = new ArrayList<FileMetadata>( 1 );
            }
            fileMetadatas.add( fileMetadata );
            httpServletRequest.getSession().setAttribute( FILES_SESSION_KEY, fileMetadatas );
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
        boolean removed = getSessionFileMetadatas().remove( new FileMetadata( fileName ) );
        if ( file.exists() )
        {
            return file.delete();
        }
        return Boolean.FALSE;
    }

    public List<FileMetadata> getSessionFileMetadatas()
        throws ArchivaRestServiceException
    {
        List<FileMetadata> fileMetadatas =
            (List<FileMetadata>) httpServletRequest.getSession().getAttribute( FILES_SESSION_KEY );

        return fileMetadatas == null ? Collections.<FileMetadata>emptyList() : fileMetadatas;
    }

}
