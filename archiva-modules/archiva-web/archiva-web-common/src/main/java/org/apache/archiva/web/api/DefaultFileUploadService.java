package org.apache.archiva.web.api;
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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksumUtil;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.content.base.ArtifactUtil;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.metadata.base.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.base.RepositoryMetadataWriter;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.services.AbstractRestService;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.web.model.FileMetadata;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * Service for uploading files to the repository.
 *
 * @author Olivier Lamy
 * @author Martin Stockhammer
 */
@Service("fileUploadService#rest")
public class DefaultFileUploadService
        extends AbstractRestService
        implements FileUploadService {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Context
    private HttpServletRequest httpServletRequest;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private ArtifactUtil artifactUtil;

    @Inject
    private ArchivaAdministration archivaAdministration;

    @Inject
    ArchivaConfiguration configuration;

    private List<ChecksumAlgorithm> algorithms;

    private final String FS = FileSystems.getDefault().getSeparator();

    @Inject
    @Named(value = "archivaTaskScheduler#repository")
    private ArchivaTaskScheduler<RepositoryTask> scheduler;

    @Inject
    private RepositoryRegistry repositoryRegistry;

    private String getStringValue(MultipartBody multipartBody, String attachmentId)
            throws IOException {
        Attachment attachment = multipartBody.getAttachment(attachmentId);
        return attachment == null ? "" :
                StringUtils.trim(URLDecoder.decode(IOUtils.toString(attachment.getDataHandler().getInputStream(), "UTF-8"), "UTF-8"));
    }

    @PostConstruct
    private void initialize() {
        algorithms = ChecksumUtil.getAlgorithms(configuration.getConfiguration().getArchivaRuntimeConfiguration().getChecksumTypes());
    }

    @Override
    public FileMetadata post(MultipartBody multipartBody)
            throws ArchivaRestServiceException {

        try {

            String classifier = getStringValue(multipartBody, "classifier");
            String packaging = getStringValue(multipartBody, "packaging");

            checkParamChars("classifier", classifier);
            checkParamChars("packaging", packaging);

            // skygo: http header form pomFile was once sending 1 for true and void for false
            // leading to permanent false value for pomFile if using toBoolean(); use , "1", ""

            boolean pomFile = false;
            try {
                pomFile = BooleanUtils.toBoolean(getStringValue(multipartBody, "pomFile"));
            } catch (IllegalArgumentException ex) {
                ArchivaRestServiceException e = new ArchivaRestServiceException("Bad value for boolean pomFile field.", null);
                e.setHttpErrorCode(422);
                e.setFieldName("pomFile");
                e.setErrorKey("fileupload.malformed.pomFile");
                throw e;
            }

            Attachment file = multipartBody.getAttachment("files[]");

            //Content-Disposition: form-data; name="files[]"; filename="org.apache.karaf.features.command-2.2.2.jar"
            String fileName = file.getContentDisposition().getParameter("filename");
            Path fileNamePath = Paths.get(fileName);
            if (!fileName.equals(fileNamePath.getFileName().toString())) {
                ArchivaRestServiceException e = new ArchivaRestServiceException("Bad filename in upload content: " + fileName + " - File traversal chars (..|/) are not allowed"
                        , null);
                e.setHttpErrorCode(422);
                e.setErrorKey("fileupload.malformed.filename");
                throw e;
            }

            Path tmpFile = Files.createTempFile("upload-artifact", ".tmp");
            tmpFile.toFile().deleteOnExit();
            IOUtils.copy(file.getDataHandler().getInputStream(), new FileOutputStream(tmpFile.toFile()));
            FileMetadata fileMetadata = new FileMetadata(fileName, Files.size(tmpFile), "theurl");
            fileMetadata.setServerFileName(tmpFile.toString());
            fileMetadata.setClassifier(classifier);
            fileMetadata.setDeleteUrl(tmpFile.getFileName().toString());
            fileMetadata.setPomFile(pomFile);
            fileMetadata.setPackaging(packaging);

            log.info("uploading file: {}", fileMetadata);

            List<FileMetadata> fileMetadatas = getSessionFilesList();

            fileMetadatas.add(fileMetadata);

            return fileMetadata;
        } catch (IOException e) {
            throw new ArchivaRestServiceException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
        }

    }

    /**
     * @return The file list from the session.
     */
    @SuppressWarnings("unchecked")
    protected List<FileMetadata> getSessionFilesList() {
        final HttpSession session = httpServletRequest.getSession();
        List<FileMetadata> fileMetadata = (List<FileMetadata>) session.getAttribute(FILES_SESSION_KEY);
        // Double check with synchronization, we assume, that httpServletRequest is
        // fully initialized (no volatile)
        if (fileMetadata == null) {
            synchronized (session) {
                fileMetadata = (List<FileMetadata>) session.getAttribute(FILES_SESSION_KEY);
                if (fileMetadata == null) {
                    fileMetadata = new CopyOnWriteArrayList<>();
                    session.setAttribute(FILES_SESSION_KEY, fileMetadata);
                }
            }
        }
        return fileMetadata;
    }

    @Override
    public Boolean deleteFile(String fileName)
            throws ArchivaRestServiceException {
        log.debug("Deleting file {}", fileName);
        // we make sure, that there are no other path components in the filename:
        String checkedFileName = Paths.get(fileName).getFileName().toString();
        Path file = SystemUtils.getJavaIoTmpDir().toPath().resolve(checkedFileName);
        log.debug("delete file:{},exists:{}", file, Files.exists(file));
        boolean removed = getSessionFileMetadatas().remove(new FileMetadata(fileName));
        // try with full name as ui only know the file name
        if (!removed) {
            removed = getSessionFileMetadatas().remove(new FileMetadata(file.toString()));
        }
        if (removed) {
            try {
                Files.deleteIfExists(file);
                return Boolean.TRUE;
            } catch (IOException e) {
                log.error("Could not delete file {}: {}", file, e.getMessage(), e);
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean clearUploadedFiles()
            throws ArchivaRestServiceException {
        List<FileMetadata> fileMetadatas = new ArrayList<>(getSessionFileMetadatas());
        for (FileMetadata fileMetadata : fileMetadatas) {
            deleteFile(Paths.get(fileMetadata.getServerFileName()).toString());
        }
        getSessionFileMetadatas().clear();
        return Boolean.TRUE;
    }

    @Override
    public List<FileMetadata> getSessionFileMetadatas()
            throws ArchivaRestServiceException {
        return getSessionFilesList();
    }


    private boolean hasValidChars(String checkString) {
        if (checkString.contains(FS)) {
            return false;
        }
        if (checkString.contains("../")) {
            return false;
        }
        if (checkString.contains("/..")) {
            return false;
        }
        return true;
    }

    private void checkParamChars(String param, String value) throws ArchivaRestServiceException {
        if (!hasValidChars(value)) {
            ArchivaRestServiceException e = new ArchivaRestServiceException("Bad characters in " + param, null);
            e.setHttpErrorCode(422);
            e.setErrorKey("fileupload.malformed.param");
            e.setFieldName(param);
            throw e;
        }
    }

    @Override
    public Boolean save(String repositoryId, String groupId, String artifactId, String version, String packaging,
                        boolean generatePom)
            throws ArchivaRestServiceException {
        repositoryId = StringUtils.trim(repositoryId);
        groupId = StringUtils.trim(groupId);
        artifactId = StringUtils.trim(artifactId);
        version = StringUtils.trim(version);
        packaging = StringUtils.trim(packaging);

        checkParamChars("repositoryId", repositoryId);
        checkParamChars("groupId", groupId);
        checkParamChars("artifactId", artifactId);
        checkParamChars("version", version);
        checkParamChars("packaging", packaging);


        List<FileMetadata> fileMetadatas = getSessionFilesList();
        if (fileMetadatas == null || fileMetadatas.isEmpty()) {
            return Boolean.FALSE;
        }

        try {
            ManagedRepository managedRepository = managedRepositoryAdmin.getManagedRepository(repositoryId);

            if (managedRepository == null) {
                // TODO i18n ?
                throw new ArchivaRestServiceException("Cannot find managed repository with id " + repositoryId,
                        Response.Status.BAD_REQUEST.getStatusCode(), null);
            }

            if (VersionUtil.isSnapshot(version) && !managedRepository.isSnapshots()) {
                // TODO i18n ?
                throw new ArchivaRestServiceException(
                        "Managed repository with id " + repositoryId + " do not accept snapshots",
                        Response.Status.BAD_REQUEST.getStatusCode(), null);
            }
        } catch (RepositoryAdminException e) {
            throw new ArchivaRestServiceException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
        }

        // get from the session file with groupId/artifactId

        Iterable<FileMetadata> filesToAdd = Iterables.filter(fileMetadatas, new Predicate<FileMetadata>() {
            public boolean apply(FileMetadata fileMetadata) {
                return fileMetadata != null && !fileMetadata.isPomFile();
            }
        });
        Iterator<FileMetadata> iterator = filesToAdd.iterator();
        boolean pomGenerated = false;
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            log.debug("fileToAdd: {}", fileMetadata);
            saveFile(repositoryId, fileMetadata, generatePom && !pomGenerated, groupId, artifactId, version,
                    packaging);
            pomGenerated = true;
            deleteFile(fileMetadata.getServerFileName());
        }

        filesToAdd = Iterables.filter(fileMetadatas, new Predicate<FileMetadata>() {
            @Override
            public boolean apply(FileMetadata fileMetadata) {
                return fileMetadata != null && fileMetadata.isPomFile();
            }
        });

        iterator = filesToAdd.iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            log.debug("fileToAdd: {}", fileMetadata);
            savePomFile(repositoryId, fileMetadata, groupId, artifactId, version, packaging);
            deleteFile(fileMetadata.getServerFileName());
        }

        return Boolean.TRUE;
    }

    protected void savePomFile(String repositoryId, FileMetadata fileMetadata, String groupId, String artifactId,
                               String version, String packaging)
            throws ArchivaRestServiceException {

        log.debug("Saving POM");
        try {
            boolean fixChecksums =
                    !(archivaAdministration.getKnownContentConsumers().contains("create-missing-checksums"));

            org.apache.archiva.repository.ManagedRepository repoConfig = repositoryRegistry.getManagedRepository(repositoryId);

            ArtifactReference artifactReference = createArtifactRef(fileMetadata, groupId, artifactId, version);
            artifactReference.setType(packaging);

            StorageAsset pomPath = artifactUtil.getArtifactAsset(repoConfig, artifactReference);
            StorageAsset targetPath = pomPath.getParent();

            String pomFilename = pomPath.getName();
            if (StringUtils.isNotEmpty(fileMetadata.getClassifier())) {
                pomFilename = StringUtils.remove(pomFilename, "-" + fileMetadata.getClassifier());
            }
            pomFilename = FilenameUtils.removeExtension(pomFilename) + ".pom";

            copyFile(Paths.get(fileMetadata.getServerFileName()), targetPath, pomFilename, fixChecksums);
            triggerAuditEvent(repoConfig.getId(), targetPath.resolve(pomFilename).toString(), AuditEvent.UPLOAD_FILE);
            queueRepositoryTask(repoConfig.getId(), targetPath.resolve(pomFilename));
            log.debug("Finished Saving POM");
        } catch (IOException ie) {
            log.error("IOException for POM {}", ie.getMessage());
            throw new ArchivaRestServiceException("Error encountered while uploading pom file: " + ie.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ie);
        } catch (RepositoryException rep) {
            log.error("RepositoryException for POM {}", rep.getMessage());
            throw new ArchivaRestServiceException("Repository exception: " + rep.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rep);
        } catch (RepositoryAdminException e) {
            log.error("RepositoryAdminException for POM {}", e.getMessage());
            throw new ArchivaRestServiceException("RepositoryAdmin exception: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
        }
    }

    protected void saveFile(String repositoryId, FileMetadata fileMetadata, boolean generatePom, String groupId,
                            String artifactId, String version, String packaging)
            throws ArchivaRestServiceException {
        log.debug("Saving file");
        try {

            org.apache.archiva.repository.ManagedRepository repoConfig = repositoryRegistry.getManagedRepository(repositoryId);

            ArtifactReference artifactReference = createArtifactRef(fileMetadata, groupId, artifactId, version);
            artifactReference.setType(
                    StringUtils.isEmpty(fileMetadata.getPackaging()) ? packaging : fileMetadata.getPackaging());

            StorageAsset artifactPath = artifactUtil.getArtifactAsset(repoConfig, artifactReference);
            StorageAsset targetPath = artifactPath.getParent();

            log.debug("artifactPath: {} found targetPath: {}", artifactPath, targetPath);

            Date lastUpdatedTimestamp = Calendar.getInstance().getTime();
            int newBuildNumber = -1;
            String timestamp = null;

            StorageAsset versionMetadataFile = targetPath.resolve(MetadataTools.MAVEN_METADATA);
            ArchivaRepositoryMetadata versionMetadata = getMetadata(versionMetadataFile);

            if (VersionUtil.isSnapshot(version)) {
                TimeZone timezone = TimeZone.getTimeZone("UTC");
                DateFormat fmt = new SimpleDateFormat("yyyyMMdd.HHmmss");
                fmt.setTimeZone(timezone);
                timestamp = fmt.format(lastUpdatedTimestamp);
                if (versionMetadata.getSnapshotVersion() != null) {
                    newBuildNumber = versionMetadata.getSnapshotVersion().getBuildNumber() + 1;
                } else {
                    newBuildNumber = 1;
                }
            }

            if (!targetPath.exists()) {
                targetPath.create();
            }

            String filename = artifactPath.getName().toString();
            if (VersionUtil.isSnapshot(version)) {
                filename = filename.replaceAll(VersionUtil.SNAPSHOT, timestamp + "-" + newBuildNumber);
            }

            // We always fix checksums for newly uploaded files, even if the content consumer is active.
            boolean fixChecksums = true;
            // !(archivaAdministration.getKnownContentConsumers().contains("create-missing-checksums"));

            try {
                StorageAsset targetFile = targetPath.resolve(filename);
                if (targetFile.exists() && !VersionUtil.isSnapshot(version) && repoConfig.blocksRedeployments()) {
                    throw new ArchivaRestServiceException(
                            "Overwriting released artifacts in repository '" + repoConfig.getId() + "' is not allowed.",
                            Response.Status.BAD_REQUEST.getStatusCode(), null);
                } else {
                    copyFile(Paths.get(fileMetadata.getServerFileName()), targetPath, filename, fixChecksums);
                    triggerAuditEvent(repoConfig.getId(), artifactPath.toString(), AuditEvent.UPLOAD_FILE);
                    queueRepositoryTask(repoConfig.getId(), targetFile);
                }
            } catch (IOException ie) {
                log.error("IOException copying file: {}", ie.getMessage(), ie);
                throw new ArchivaRestServiceException(
                        "Overwriting released artifacts in repository '" + repoConfig.getId() + "' is not allowed.",
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ie);
            }

            if (generatePom) {
                String pomFilename = filename;
                if (StringUtils.isNotEmpty(fileMetadata.getClassifier())) {
                    pomFilename = StringUtils.remove(pomFilename, "-" + fileMetadata.getClassifier());
                }
                pomFilename = FilenameUtils.removeExtension(pomFilename) + ".pom";

                try {
                    StorageAsset generatedPomFile =
                            createPom(targetPath, pomFilename, fileMetadata, groupId, artifactId, version, packaging);
                    triggerAuditEvent(repoConfig.getId(), targetPath.resolve(pomFilename).toString(), AuditEvent.UPLOAD_FILE);
                    if (fixChecksums) {
                        fixChecksums(generatedPomFile);
                    }
                    queueRepositoryTask(repoConfig.getId(), generatedPomFile);
                } catch (IOException ie) {
                    throw new ArchivaRestServiceException(
                            "Error encountered while writing pom file: " + ie.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ie);
                }
            }

            // explicitly update only if metadata-updater consumer is not enabled!
            if (!archivaAdministration.getKnownContentConsumers().contains("metadata-updater")) {
                updateProjectMetadata(targetPath, lastUpdatedTimestamp, timestamp, newBuildNumber,
                        fixChecksums, fileMetadata, groupId, artifactId, version, packaging);

                if (VersionUtil.isSnapshot(version)) {
                    updateVersionMetadata(versionMetadata, versionMetadataFile, lastUpdatedTimestamp, timestamp,
                            newBuildNumber, fixChecksums, fileMetadata, groupId, artifactId, version,
                            packaging);
                }
            }
        } catch (RepositoryNotFoundException re) {
            log.error("RepositoryNotFoundException during save {}", re.getMessage());
            re.printStackTrace();
            throw new ArchivaRestServiceException("Target repository cannot be found: " + re.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), re);
        } catch (RepositoryException rep) {
            log.error("RepositoryException during save {}", rep.getMessage());
            throw new ArchivaRestServiceException("Repository exception: " + rep.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rep);
        } catch (RepositoryAdminException e) {
            log.error("RepositoryAdminException during save {}", e.getMessage());
            throw new ArchivaRestServiceException("RepositoryAdmin exception: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
        } catch (IOException e) {
            log.error("IOException during save {}", e.getMessage());
            throw new ArchivaRestServiceException("Repository exception " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e);
        }
    }

    private ArtifactReference createArtifactRef(FileMetadata fileMetadata, String groupId, String artifactId, String version) {
        ArtifactReference artifactReference = new ArtifactReference();
        artifactReference.setArtifactId(artifactId);
        artifactReference.setGroupId(groupId);
        artifactReference.setVersion(version);
        artifactReference.setClassifier(fileMetadata.getClassifier());
        return artifactReference;
    }

    private ArchivaRepositoryMetadata getMetadata(StorageAsset metadataFile)
            throws RepositoryMetadataException {
        ArchivaRepositoryMetadata metadata = new ArchivaRepositoryMetadata();
        if (metadataFile.exists()) {
            Repository repo = repositoryRegistry.getRepositoryOfAsset( metadataFile );
            RepositoryType type = repo == null ? RepositoryType.MAVEN : repo.getType( );
            MetadataReader metadataReader = repositoryRegistry.getMetadataReader( type );
            metadata = metadataReader.read(metadataFile);
        }
        return metadata;
    }

    private StorageAsset createPom(StorageAsset targetPath, String filename, FileMetadata fileMetadata, String groupId,
                           String artifactId, String version, String packaging)
            throws IOException {
        Model projectModel = new Model();
        projectModel.setModelVersion("4.0.0");
        projectModel.setGroupId(groupId);
        projectModel.setArtifactId(artifactId);
        projectModel.setVersion(version);
        projectModel.setPackaging(packaging);

        StorageAsset pomFile = targetPath.resolve(filename);
        MavenXpp3Writer writer = new MavenXpp3Writer();

        try (Writer w = new OutputStreamWriter(pomFile.getWriteStream(true))) {
            writer.write(w, projectModel);
        }

        return pomFile;
    }

    private void fixChecksums(StorageAsset file) {
        ChecksummedFile checksum = new ChecksummedFile(file.getFilePath());
        checksum.fixChecksums(algorithms);
    }

    private void queueRepositoryTask(String repositoryId, StorageAsset localFile) {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId(repositoryId);
        task.setResourceFile(localFile);
        task.setUpdateRelatedArtifacts(true);
        task.setScanAll(false);

        try {
            scheduler.queueTask(task);
        } catch (TaskQueueException e) {
            log.error("Unable to queue repository task to execute consumers on resource file ['{}"
                    + "'].", localFile.getName());
        }
    }

    private void copyFile(Path sourceFile, StorageAsset targetPath, String targetFilename, boolean fixChecksums)
            throws IOException {

        targetPath.resolve(targetFilename).replaceDataFromFile(sourceFile);

        if (fixChecksums) {
            fixChecksums(targetPath.resolve(targetFilename));
        }
    }

    /**
     * Update artifact level metadata. If it does not exist, create the metadata and fix checksums if necessary.
     */
    private void updateProjectMetadata(StorageAsset targetPath, Date lastUpdatedTimestamp, String timestamp, int buildNumber,
                                       boolean fixChecksums, FileMetadata fileMetadata, String groupId,
                                       String artifactId, String version, String packaging)
            throws RepositoryMetadataException {
        List<String> availableVersions = new ArrayList<>();
        String latestVersion = version;

        StorageAsset projectDir = targetPath.getParent();
        StorageAsset projectMetadataFile = projectDir.resolve(MetadataTools.MAVEN_METADATA);

        ArchivaRepositoryMetadata projectMetadata = getMetadata(projectMetadataFile);

        if (projectMetadataFile.exists()) {
            availableVersions = projectMetadata.getAvailableVersions();

            Collections.sort(availableVersions, VersionComparator.getInstance());

            if (!availableVersions.contains(version)) {
                availableVersions.add(version);
            }

            latestVersion = availableVersions.get(availableVersions.size() - 1);
        } else {
            availableVersions.add(version);

            projectMetadata.setGroupId(groupId);
            projectMetadata.setArtifactId(artifactId);
        }

        if (projectMetadata.getGroupId() == null) {
            projectMetadata.setGroupId(groupId);
        }

        if (projectMetadata.getArtifactId() == null) {
            projectMetadata.setArtifactId(artifactId);
        }

        projectMetadata.setLatestVersion(latestVersion);
        projectMetadata.setLastUpdatedTimestamp(lastUpdatedTimestamp);
        projectMetadata.setAvailableVersions(availableVersions);

        if (!VersionUtil.isSnapshot(version)) {
            projectMetadata.setReleasedVersion(latestVersion);
        }

        RepositoryMetadataWriter.write(projectMetadata, projectMetadataFile);

        if (fixChecksums) {
            fixChecksums(projectMetadataFile);
        }
    }

    /**
     * Update version level metadata for snapshot artifacts. If it does not exist, create the metadata and fix checksums
     * if necessary.
     */
    private void updateVersionMetadata(ArchivaRepositoryMetadata metadata, StorageAsset metadataFile,
                                       Date lastUpdatedTimestamp, String timestamp, int buildNumber,
                                       boolean fixChecksums, FileMetadata fileMetadata, String groupId,
                                       String artifactId, String version, String packaging)
            throws RepositoryMetadataException {
        if (!metadataFile.exists()) {
            metadata.setGroupId(groupId);
            metadata.setArtifactId(artifactId);
            metadata.setVersion(version);
        }

        if (metadata.getSnapshotVersion() == null) {
            metadata.setSnapshotVersion(new SnapshotVersion());
        }

        metadata.getSnapshotVersion().setBuildNumber(buildNumber);
        metadata.getSnapshotVersion().setTimestamp(timestamp);
        metadata.setLastUpdatedTimestamp(lastUpdatedTimestamp);

        RepositoryMetadataWriter.write(metadata, metadataFile);

        if (fixChecksums) {
            fixChecksums(metadataFile);
        }
    }


}
