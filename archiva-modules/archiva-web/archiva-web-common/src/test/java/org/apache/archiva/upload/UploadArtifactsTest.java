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
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.redback.rest.services.AbstractRestServicesTest;
import org.apache.archiva.redback.rest.services.FakeCreateAdminService;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.archiva.web.api.FileUploadService;
import org.apache.archiva.web.model.FileMetadata;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.AttachmentBuilder;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.message.Message;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.ClientErrorException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Olivier Lamy
 */
@RunWith(ArchivaBlockJUnit4ClassRunner.class)
public class UploadArtifactsTest
        extends AbstractRestServicesTest {

    private static String PREVIOUS_ARCHIVA_PATH;
    private AtomicReference<Path> projectDir = new AtomicReference<>( );

    @BeforeClass
    public static void initConfigurationPath()
            throws Exception
    {
        PREVIOUS_ARCHIVA_PATH = System.getProperty(ArchivaConfiguration.USER_CONFIG_PROPERTY);
        System.setProperty( ArchivaConfiguration.USER_CONFIG_PROPERTY,
                System.getProperty( "test.resources.path" ) + "/archiva.xml" );
    }


    @AfterClass
    public static void restoreConfigurationPath()
            throws Exception
    {
        System.setProperty( ArchivaConfiguration.USER_CONFIG_PROPERTY, PREVIOUS_ARCHIVA_PATH );
    }
    @Override
    protected String getSpringConfigLocation() {
        return "classpath*:META-INF/spring-context.xml,classpath:/spring-context-test-upload.xml";
    }

    protected Path getProjectDirectory() {
        if ( projectDir.get()==null) {
            String propVal = System.getProperty("mvn.project.base.dir");
            Path newVal;
            if (StringUtils.isEmpty(propVal)) {
                newVal = Paths.get("").toAbsolutePath();
            } else {
                newVal = Paths.get(propVal).toAbsolutePath();
            }
            projectDir.compareAndSet(null, newVal);
        }
        return projectDir.get();
    }

    @Override
    protected String getRestServicesPath() {
        return "restServices";
    }

    protected String getBaseUrl() {
        String baseUrlSysProps = System.getProperty("archiva.baseRestUrl");
        return StringUtils.isBlank(baseUrlSysProps) ? "http://localhost:" + getServerPort() : baseUrlSysProps;
    }

    private FileUploadService getUploadService() {
        FileUploadService service =
                JAXRSClientFactory.create(getBaseUrl() + "/" + getRestServicesPath() + "/archivaUiServices/",
                        FileUploadService.class,
                        Collections.singletonList(new JacksonJaxbJsonProvider()));
        log.debug("Service class {}", service.getClass().getName());
        WebClient.client(service).header("Authorization", authorizationHeader);
        WebClient.client(service).header("Referer", "http://localhost:" + getServerPort());

        WebClient.client(service).header("Referer", "http://localhost");
        WebClient.getConfig(service).getRequestContext().put(Message.MAINTAIN_SESSION, true);
        WebClient.getConfig(service).getRequestContext().put(Message.EXCEPTION_MESSAGE_CAUSE_ENABLED, true);
        WebClient.getConfig(service).getRequestContext().put(Message.FAULT_STACKTRACE_ENABLED, true);
        WebClient.getConfig(service).getRequestContext().put(Message.PROPOGATE_EXCEPTION, true);
        WebClient.getConfig(service).getRequestContext().put("org.apache.cxf.transport.no_io_exceptions", true);

        // WebClient.client( service ).
        return service;
    }

    @Test
    public void clearUploadedFiles()
            throws Exception {
        FileUploadService service = getUploadService();
        service.clearUploadedFiles();
    }

    @Test
    public void uploadFile() throws IOException, ArchivaRestServiceException {
        FileUploadService service = getUploadService();
        try {
            Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");
            final Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"" + file.getFileName().toString() + "\"; name=\"files[]\"")).build();
            MultipartBody body = new MultipartBody(fileAttachment);
            service.post(body);
        } finally {
            service.clearUploadedFiles();
        }
    }

    @Test
    public void failUploadFileWithBadFileName() throws IOException, ArchivaRestServiceException {
        FileUploadService service = getUploadService();
        try {
            Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");
            final Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"/../TestFile.testext\"; name=\"files[]\"")).build();
            MultipartBody body = new MultipartBody(fileAttachment);
            try {
                service.post(body);
                fail("FileNames with path contents should not be allowed.");
            } catch (ClientErrorException e) {
                assertEquals(422, e.getResponse().getStatus());
            }
        } finally {
            service.clearUploadedFiles();
        }
    }

    @Test
    public void uploadAndDeleteFile() throws IOException, ArchivaRestServiceException {
        FileUploadService service = getUploadService();
        try {
            Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");
            final Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"" + file.getFileName().toString() + "\"; name=\"files[]\"")).build();
            MultipartBody body = new MultipartBody(fileAttachment);
            service.post(body);
            service.deleteFile(file.getFileName().toString());
        } finally {
            service.clearUploadedFiles();
        }
    }

    @Test
    public void failUploadAndDeleteWrongFile() throws IOException, ArchivaRestServiceException {
        FileUploadService service = getUploadService();
        try {
            Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");
            final Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"" + file.getFileName().toString() + "\"; name=\"files[]\"")).build();
            MultipartBody body = new MultipartBody(fileAttachment);
            service.post(body);
            assertFalse(service.deleteFile("file123" + file.getFileName().toString()));
        } finally {
            service.clearUploadedFiles();
        }
    }

    @Test
    public void failUploadAndDeleteFileInOtherDir() throws IOException, ArchivaRestServiceException {
        Path testFile = null;
        try {
            FileUploadService service = getUploadService();
            Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");
            Path targetDir = Paths.get("target/testDelete").toAbsolutePath();
            if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
            Path tempDir = SystemUtils.getJavaIoTmpDir().toPath();
            testFile = Files.createTempFile(targetDir, "TestFile", ".txt");
            log.debug("Test file {}", testFile.toAbsolutePath());
            log.debug("Tmp dir {}", tempDir.toAbsolutePath());
            assertTrue(Files.exists(testFile));
            Path relativePath = tempDir.relativize(testFile.toAbsolutePath());
            final Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"" + file.getFileName().toString() + "\"; name=\"files[]\"")).build();
            MultipartBody body = new MultipartBody(fileAttachment);
            service.post(body);
            String relativePathEncoded = URLEncoder.encode("../target/" + relativePath.toString(), "UTF-8");
            log.debug("Trying to delete with path traversal: {}, {}", relativePath, relativePathEncoded);
            try {
                service.deleteFile(relativePathEncoded);
            } catch (ArchivaRestServiceException ex) {
                // Expected exception
            }
            assertTrue("File in another directory may not be deleted", Files.exists(testFile));
        } finally {
            if (testFile != null) {
                Files.deleteIfExists(testFile);
            }
        }
    }

    @Test
    public void failSaveFileWithBadParams() throws IOException, ArchivaRestServiceException {
        Path path = Paths.get("target/appserver-base/repositories/internal/org/apache/archiva/archiva-model/1.2/archiva-model-1.2.jar");
        Files.deleteIfExists(path);
        FileUploadService service = getUploadService();
        Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");

        Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"archiva-model.jar\"; name=\"files[]\"")).build();
        MultipartBody body = new MultipartBody(fileAttachment);
        service.post(body);
        assertTrue(service.save("internal", "org.apache.archiva", "archiva-model", "1.2", "jar", true));

        fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"TestFile.FileExt\"; name=\"files[]\"")).build();
        body = new MultipartBody(fileAttachment);
        FileMetadata meta = service.post(body);
        log.debug("Metadata {}", meta.toString());
        try {
            service.save("internal", "org", URLEncoder.encode("../../../test", "UTF-8"), URLEncoder.encode("testSave", "UTF-8"), "4", true);
            fail("Error expected, if the content contains bad characters.");
        } catch (ClientErrorException e) {
            assertEquals(422, e.getResponse().getStatus());
        }
        assertFalse(Files.exists(Paths.get("target/test-testSave.4")));
    }

    @Test
    public void saveFile() throws IOException, ArchivaRestServiceException {
        log.debug("Starting saveFile()");

        Path path = Paths.get("target/appserver-base/repositories/internal/org/apache/archiva/archiva-model/1.2/archiva-model-1.2.jar");
        log.debug("Jar exists: {}",Files.exists(path));
        Files.deleteIfExists(path);
        path = Paths.get("target/appserver-base/repositories/internal/org/apache/archiva/archiva-model/1.2/archiva-model-1.2.pom");
        Files.deleteIfExists(path);
        FileUploadService service = getUploadService();
        service.clearUploadedFiles();
        Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");
        log.debug("Upload file exists: {}", Files.exists(file));
        final Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"archiva-model.jar\"; name=\"files[]\"")).build();
        MultipartBody body = new MultipartBody(fileAttachment);
        service.post(body);
        service.save("internal", "org.apache.archiva", "archiva-model", "1.2", "jar", true);
    }

    @Test
    public void saveFileWithOtherExtension() throws IOException, ArchivaRestServiceException {
        log.debug("Starting saveFileWithOtherExtension()");

        Path path = Paths.get("target/appserver-base/repositories/internal/org/apache/archiva/archiva-model/1.2/archiva-model-1.2.bin");
        log.debug("Jar exists: {}",Files.exists(path));
        Files.deleteIfExists(path);
        Path pomPath = Paths.get("target/appserver-base/repositories/internal/org/apache/archiva/archiva-model/1.2/archiva-model-1.2.pom");
        Files.deleteIfExists(pomPath);
        FileUploadService service = getUploadService();
        service.clearUploadedFiles();
        Path file = getProjectDirectory().resolve("src/test/repositories/snapshot-repo/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-20130425.081822-1.jar");
        log.debug("Upload file exists: {}", Files.exists(file));
        final Attachment fileAttachment = new AttachmentBuilder().object(Files.newInputStream(file)).contentDisposition(new ContentDisposition("form-data; filename=\"archiva-model.bin\"; name=\"files[]\"")).build();
        MultipartBody body = new MultipartBody(fileAttachment);
        service.post(body);
        assertTrue(service.save("internal", "org.apache.archiva", "archiva-model", "1.2", "bin", false));
        assertTrue(Files.exists(path));
    }


}
