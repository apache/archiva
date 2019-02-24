package org.apache.archiva.upload;
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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.redback.rest.api.services.RoleManagementService;
import org.apache.archiva.redback.rest.services.AbstractRestServicesTest;
import org.apache.archiva.remotedownload.AbstractDownloadTest;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.archiva.web.api.FileUploadService;
import org.apache.archiva.web.api.RuntimeInfoService;
import org.apache.archiva.web.model.ApplicationRuntimeInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.AttachmentBuilder;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.repository.Repository;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class UploadArtifactsTest
    extends AbstractRestServicesTest
{
    @Override
    @Before
    public void startServer()
        throws Exception
    {
        File appServerBase = new File( System.getProperty( "appserver.base" ) );
        File confDir = new File(appServerBase, "conf");
        if (!confDir.exists()) {
            confDir.mkdir();
        }
        Path log4jCfg = Paths.get("src/test/resources/log4j2-test.xml");
        Path log4jCfgDst = confDir.toPath().resolve(log4jCfg.getFileName());
        Files.copy( log4jCfg, log4jCfgDst, StandardCopyOption.REPLACE_EXISTING );

        File jcrDirectory = new File( appServerBase, "jcr" );

        if ( jcrDirectory.exists() )
        {
            FileUtils.deleteDirectory( jcrDirectory );
        }
        // We have to activate this to verify the bad path traversal protection. We cannot rely on
        // the application server only.
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH","true");

        super.startServer();
    }

    @After
    public void stop() {
        System.clearProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH" );
    }

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml,classpath:/spring-context-with-jcr.xml";
    }

    @Override
    protected String getRestServicesPath()
    {
        return "restServices";
    }

    protected String getBaseUrl()
    {
        String baseUrlSysProps = System.getProperty( "archiva.baseRestUrl" );
        return StringUtils.isBlank( baseUrlSysProps ) ? "http://localhost:" + getServerPort() : baseUrlSysProps;
    }

    private FileUploadService getUploadService()
    {
        FileUploadService service =
            JAXRSClientFactory.create( getBaseUrl( ) + "/" + getRestServicesPath( ) + "/archivaUiServices/",
                FileUploadService.class,
                Collections.singletonList( new JacksonJaxbJsonProvider( ) ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client( service ).header( "Referer", "http://localhost:" + getServerPort() );

        WebClient.client( service ).header( "Referer", "http://localhost" );
        return service;
    }

    @Test
    public void clearUploadedFiles()
        throws Exception
    {
        FileUploadService service = getUploadService( );
        service.clearUploadedFiles();
    }

    @Test
    public void uploadFile() throws IOException, ArchivaRestServiceException
    {
        FileUploadService service = getUploadService( );
        try
        {
            Path file = Paths.get( "src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar" );
            final Attachment fileAttachment = new AttachmentBuilder( ).object( Files.newInputStream( file ) ).contentDisposition( new ContentDisposition( "form-data; filename=\"" + file.getFileName( ).toString( ) + "\"; name=\"files[]\"" ) ).build( );
            MultipartBody body = new MultipartBody( fileAttachment );
            service.post( body );
        } finally
        {
            service.clearUploadedFiles( );
        }
    }

    @Test
    public void uploadAndDeleteFile() throws IOException, ArchivaRestServiceException
    {
        FileUploadService service = getUploadService( );
        try
        {
            Path file = Paths.get( "src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar" );
            final Attachment fileAttachment = new AttachmentBuilder( ).object( Files.newInputStream( file ) ).contentDisposition( new ContentDisposition( "form-data; filename=\"" + file.getFileName( ).toString( ) + "\"; name=\"files[]\"" ) ).build( );
            MultipartBody body = new MultipartBody( fileAttachment );
            service.post( body );
            service.deleteFile( file.getFileName( ).toString( ) );
        } finally
        {
            service.clearUploadedFiles();
        }
    }

    @Test
    public void uploadAndDeleteWrongFile() throws IOException, ArchivaRestServiceException
    {
        FileUploadService service = getUploadService( );
        try
        {
            Path file = Paths.get( "src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar" );
            final Attachment fileAttachment = new AttachmentBuilder( ).object( Files.newInputStream( file ) ).contentDisposition( new ContentDisposition( "form-data; filename=\"" + file.getFileName( ).toString( ) + "\"; name=\"files[]\"" ) ).build( );
            MultipartBody body = new MultipartBody( fileAttachment );
            service.post( body );
            assertFalse( service.deleteFile( "file123" + file.getFileName( ).toString( ) ) );
        } finally {
            service.clearUploadedFiles();
        }
    }

    @Test
    public void uploadAndDeleteFileInOtherDir() throws IOException, ArchivaRestServiceException
    {
        Path testFile = null;
        try
        {
            FileUploadService service = getUploadService( );
            Path file = Paths.get( "src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar" );
            Path targetDir = Paths.get( "target/testDelete" ).toAbsolutePath( );
            if ( !Files.exists( targetDir ) ) Files.createDirectory( targetDir );
            Path tempDir = SystemUtils.getJavaIoTmpDir( ).toPath( );
            testFile = Files.createTempFile( targetDir, "TestFile", ".txt" );
            log.debug( "Test file {}", testFile.toAbsolutePath( ) );
            log.debug( "Tmp dir {}", tempDir.toAbsolutePath( ) );
            assertTrue( Files.exists( testFile ) );
            Path relativePath = tempDir.relativize( testFile.toAbsolutePath( ) );
            final Attachment fileAttachment = new AttachmentBuilder( ).object( Files.newInputStream( file ) ).contentDisposition( new ContentDisposition( "form-data; filename=\"" + file.getFileName( ).toString( ) + "\"; name=\"files[]\"" ) ).build( );
            MultipartBody body = new MultipartBody( fileAttachment );
            service.post( body );
            String relativePathEncoded = URLEncoder.encode( "../target/" + relativePath.toString( ), "UTF-8" );
            log.debug( "Trying to delete with path traversal: {}, {}", relativePath, relativePathEncoded );
            try
            {
                service.deleteFile( relativePathEncoded );
            }
            catch ( ArchivaRestServiceException ex )
            {
                // Expected exception
            }
            assertTrue( "File in another directory may not be deleted", Files.exists( testFile ) );
        } finally
        {
            if (testFile!=null) {
                Files.deleteIfExists( testFile );
            }
        }
    }
}
