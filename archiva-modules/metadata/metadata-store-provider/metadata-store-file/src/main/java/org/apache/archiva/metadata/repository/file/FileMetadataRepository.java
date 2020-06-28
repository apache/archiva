package org.apache.archiva.metadata.repository.file;

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

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.QueryParameter;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.AbstractMetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataService;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File implementation of the metadata repository. It uses property files in a separate directory tree.
 * The implementation has no fulltext index. So fulltext queries are not supported.
 *
 * Some retrieval methods may not be very efficient.
 */
public class FileMetadataRepository
        extends AbstractMetadataRepository implements MetadataRepository {

    private final ArchivaConfiguration configuration;

    private Logger log = LoggerFactory.getLogger(FileMetadataRepository.class);

    private static final String PROJECT_METADATA_KEY = "project-metadata";

    private static final String PROJECT_VERSION_METADATA_KEY = "version-metadata";

    private static final String NAMESPACE_METADATA_KEY = "namespace-metadata";

    private static final String METADATA_KEY = "metadata";

    private Map<String, Path> baseDirectory = new HashMap<>();

    public FileMetadataRepository(MetadataService metadataService,
                                  ArchivaConfiguration configuration) {
        super(metadataService);
        this.configuration = configuration;
    }

    private Path getBaseDirectory(String repoId)
            throws IOException {
        if (!baseDirectory.containsKey(repoId)) {
            Path baseDir;
            ManagedRepositoryConfiguration managedRepositoryConfiguration =
                    configuration.getConfiguration().getManagedRepositoriesAsMap().get(repoId);
            if (managedRepositoryConfiguration == null) {
                baseDir = Files.createTempDirectory(repoId);
            } else {
                baseDir = Paths.get(managedRepositoryConfiguration.getLocation());
            }
            baseDirectory.put(repoId, baseDir.resolve(".archiva"));
        }
        return baseDirectory.get(repoId);
    }

    private Path getDirectory(String repoId)
            throws IOException {
        return getBaseDirectory(repoId).resolve("content");
    }

    @Override
    public void updateProject(RepositorySession session, String repoId, ProjectMetadata project) {
        updateProject(session, repoId, project.getNamespace(), project.getId());
    }

    private void updateProject(RepositorySession session, String repoId, String namespace, String id) {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        updateNamespace(session, repoId, namespace);

        try {
            Path namespaceDirectory = getDirectory(repoId).resolve(namespace);
            Properties properties = new Properties();
            properties.setProperty("namespace", namespace);
            properties.setProperty("id", id);
            writeProperties(properties, namespaceDirectory.resolve(id), PROJECT_METADATA_KEY);
        } catch (IOException e) {
            log.error("Could not update project {}, {}, {}: {}", repoId, namespace, id, e.getMessage(), e);
        }
    }

    @Override
    public void updateProjectVersion(RepositorySession session, String repoId, String namespace, String projectId,
                                     ProjectVersionMetadata versionMetadata) {

        try {
            updateProject(session, repoId, namespace, projectId);

            Path directory =
                    getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + versionMetadata.getId());

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);
            // remove properties that are not references or artifacts
            for (Object key : new ArrayList<>(properties.keySet())) {
                String name = (String) key;
                if (!name.contains(":") && !name.equals("facetIds")) {
                    properties.remove(name);
                }

                // clear the facet contents so old properties are no longer written
                clearMetadataFacetProperties(versionMetadata.getFacetList(), properties, "");
            }
            properties.setProperty("id", versionMetadata.getId());
            setProperty(properties, "name", versionMetadata.getName());
            setProperty(properties, "description", versionMetadata.getDescription());
            setProperty(properties, "url", versionMetadata.getUrl());
            setProperty(properties, "incomplete", String.valueOf(versionMetadata.isIncomplete()));
            if (versionMetadata.getScm() != null) {
                setProperty(properties, "scm.connection", versionMetadata.getScm().getConnection());
                setProperty(properties, "scm.developerConnection", versionMetadata.getScm().getDeveloperConnection());
                setProperty(properties, "scm.url", versionMetadata.getScm().getUrl());
            }
            if (versionMetadata.getCiManagement() != null) {
                setProperty(properties, "ci.system", versionMetadata.getCiManagement().getSystem());
                setProperty(properties, "ci.url", versionMetadata.getCiManagement().getUrl());
            }
            if (versionMetadata.getIssueManagement() != null) {
                setProperty(properties, "issue.system", versionMetadata.getIssueManagement().getSystem());
                setProperty(properties, "issue.url", versionMetadata.getIssueManagement().getUrl());
            }
            if (versionMetadata.getOrganization() != null) {
                setProperty(properties, "org.name", versionMetadata.getOrganization().getName());
                setProperty(properties, "org.url", versionMetadata.getOrganization().getUrl());
            }
            int i = 0;
            for (License license : versionMetadata.getLicenses()) {
                setProperty(properties, "license." + i + ".name", license.getName());
                setProperty(properties, "license." + i + ".url", license.getUrl());
                i++;
            }
            i = 0;
            for (MailingList mailingList : versionMetadata.getMailingLists()) {
                setProperty(properties, "mailingList." + i + ".archive", mailingList.getMainArchiveUrl());
                setProperty(properties, "mailingList." + i + ".name", mailingList.getName());
                setProperty(properties, "mailingList." + i + ".post", mailingList.getPostAddress());
                setProperty(properties, "mailingList." + i + ".unsubscribe", mailingList.getUnsubscribeAddress());
                setProperty(properties, "mailingList." + i + ".subscribe", mailingList.getSubscribeAddress());
                setProperty(properties, "mailingList." + i + ".otherArchives",
                        join(mailingList.getOtherArchives()));
                i++;
            }
            i = 0;
            ProjectVersionReference reference = new ProjectVersionReference();
            reference.setNamespace(namespace);
            reference.setProjectId(projectId);
            reference.setProjectVersion(versionMetadata.getId());
            reference.setReferenceType(ProjectVersionReference.ReferenceType.DEPENDENCY);
            for (Dependency dependency : versionMetadata.getDependencies()) {
                setProperty(properties, "dependency." + i + ".classifier", dependency.getClassifier());
                setProperty(properties, "dependency." + i + ".scope", dependency.getScope());
                setProperty(properties, "dependency." + i + ".systemPath", dependency.getSystemPath());
                setProperty(properties, "dependency." + i + ".artifactId", dependency.getArtifactId());
                setProperty(properties, "dependency." + i + ".groupId", dependency.getNamespace());
                setProperty(properties, "dependency." + i + ".version", dependency.getVersion());
                setProperty(properties, "dependency." + i + ".type", dependency.getType());
                setProperty(properties, "dependency." + i + ".optional", String.valueOf(dependency.isOptional()));

                updateProjectReference(repoId, dependency.getNamespace(), dependency.getArtifactId(),
                        dependency.getVersion(), reference);

                i++;
            }
            Set<String> facetIds = new LinkedHashSet<>(versionMetadata.getFacetIds());
            facetIds.addAll(Arrays.asList(properties.getProperty("facetIds", "").split(",")));
            properties.setProperty("facetIds", join(facetIds));

            updateProjectVersionFacets(versionMetadata, properties);

            writeProperties(properties, directory, PROJECT_VERSION_METADATA_KEY);
        } catch (IOException e) {
            log.error("Could not update project version {}, {}, {}: {}", repoId, namespace, versionMetadata.getId(), e.getMessage(), e);
        }
    }

    private void updateProjectVersionFacets(ProjectVersionMetadata versionMetadata, Properties properties) {
        for (MetadataFacet facet : versionMetadata.getFacetList()) {
            for (Map.Entry<String, String> entry : facet.toProperties().entrySet()) {
                properties.setProperty(facet.getFacetId() + ":" + entry.getKey(), entry.getValue());
            }
        }
    }

    private static void clearMetadataFacetProperties(Collection<MetadataFacet> facetList, Properties properties,
                                                     String prefix) {
        List<Object> propsToRemove = new ArrayList<>();
        for (MetadataFacet facet : facetList) {
            for (Object key : new ArrayList<>(properties.keySet())) {
                String keyString = (String) key;
                if (keyString.startsWith(prefix + facet.getFacetId() + ":")) {
                    propsToRemove.add(key);
                }
            }
        }

        for (Object key : propsToRemove) {
            properties.remove(key);
        }
    }

    private void updateProjectReference(String repoId, String namespace, String projectId, String projectVersion,
                                        ProjectVersionReference reference) {
        try {
            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + projectVersion);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);
            int i = Integer.parseInt(properties.getProperty("ref:lastReferenceNum", "-1")) + 1;
            setProperty(properties, "ref:lastReferenceNum", Integer.toString(i));
            setProperty(properties, "ref:reference." + i + ".namespace", reference.getNamespace());
            setProperty(properties, "ref:reference." + i + ".projectId", reference.getProjectId());
            setProperty(properties, "ref:reference." + i + ".projectVersion", reference.getProjectVersion());
            setProperty(properties, "ref:reference." + i + ".referenceType", reference.getReferenceType().toString());

            writeProperties(properties, directory, PROJECT_VERSION_METADATA_KEY);
        } catch (IOException e) {
            log.error("Could not update project reference {}, {}, {}, {}: {}", repoId, namespace, projectId, projectVersion, e.getMessage(), e);
        }
    }

    @Override
    public void updateNamespace(RepositorySession session, String repoId, String namespace) {
        try {
            Path namespaceDirectory = getDirectory(repoId).resolve(namespace);
            Properties properties = new Properties();
            properties.setProperty("namespace", namespace);
            writeProperties(properties, namespaceDirectory, NAMESPACE_METADATA_KEY);

        } catch (IOException e) {
            log.error("Could not update namespace of {}, {}: {}", repoId, namespace, e.getMessage(), e);
        }
    }

    @Override
    public List<String> getMetadataFacets(RepositorySession session, String repoId, String facetId)
            throws MetadataRepositoryException {
        try {
            Path directory = getMetadataDirectory(repoId, facetId);
            if (!(Files.exists(directory) && Files.isDirectory(directory))) {
                return Collections.emptyList();
            }
            List<String> facets;
            final String searchFile = METADATA_KEY + ".properties";
            try (Stream<Path> fs = Files.walk(directory, FileVisitOption.FOLLOW_LINKS)) {
                facets = fs.filter(Files::isDirectory).filter(path -> Files.exists(path.resolve(searchFile)))
                        .map(path -> directory.relativize(path).toString()).collect(Collectors.toList());
            }
            return facets;
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public <T extends MetadataFacet> Stream<T> getMetadataFacetStream(RepositorySession session, String repositoryId, Class<T> facetClazz, QueryParameter queryParameter) throws MetadataRepositoryException {
        final MetadataFacetFactory<T> metadataFacetFactory = getFacetFactory(facetClazz);
        if (metadataFacetFactory == null) {
            return null;
        }
        final String facetId = metadataFacetFactory.getFacetId();
        final String searchFile = METADATA_KEY + ".properties";
        try {
            Path directory = getMetadataDirectory(repositoryId, facetId);
            return Files.walk(directory, FileVisitOption.FOLLOW_LINKS).filter(Files::isDirectory)
                    .filter(path -> Files.exists(path.resolve(searchFile)))
                    .map(path -> directory.relativize(path).toString())
                    .sorted()
                    .skip(queryParameter.getOffset())
                    .limit(queryParameter.getLimit())
                    .map(name -> getMetadataFacet(session, repositoryId, facetClazz, name));
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public boolean hasMetadataFacet(RepositorySession session, String repositoryId, String facetId)
            throws MetadataRepositoryException {

        try {
            Path directory = getMetadataDirectory(repositoryId, facetId);
            if (!(Files.exists(directory) && Files.isDirectory(directory))) {
                return false;
            }
            final String searchFile = METADATA_KEY + ".properties";
            try (Stream<Path> fs = Files.walk(directory, FileVisitOption.FOLLOW_LINKS)) {
                return fs.filter(Files::isDirectory).anyMatch(path -> Files.exists(path.resolve(searchFile)));
            }
        } catch (IOException e) {
            log.error("Could not retrieve facet metatadata {}, {}: {}", repositoryId, facetId, e.getMessage(), e);
            throw new MetadataRepositoryException(e.getMessage(), e);
        }

    }


    @Override
    public <T extends MetadataFacet> T getMetadataFacet(RepositorySession session, String repositoryId, Class<T> facetClazz, String name) {
        final MetadataFacetFactory<T> metadataFacetFactory = getFacetFactory(facetClazz);
        if (metadataFacetFactory == null) {
            return null;
        }
        final String facetId = metadataFacetFactory.getFacetId();

        Properties properties;
        try {
            properties =
                    readProperties(getMetadataDirectory(repositoryId, facetId).resolve(name), METADATA_KEY);
        } catch (NoSuchFileException | FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            log.error("Could not read properties from {}, {}: {}", repositoryId, facetId, e.getMessage(), e);
            return null;
        }
        T metadataFacet = null;
        if (metadataFacetFactory != null) {
            metadataFacet = metadataFacetFactory.createMetadataFacet(repositoryId, name);
            Map<String, String> map = new HashMap<>();
            for (Object key : new ArrayList<>(properties.keySet())) {
                String property = (String) key;
                map.put(property, properties.getProperty(property));
            }
            metadataFacet.fromProperties(map);
        }
        return metadataFacet;
    }


    @Override
    public void addMetadataFacet(RepositorySession session, String repositoryId, MetadataFacet metadataFacet) {
        Properties properties = new Properties();
        properties.putAll(metadataFacet.toProperties());

        try {
            Path directory =
                    getMetadataDirectory(repositoryId, metadataFacet.getFacetId()).resolve(metadataFacet.getName());
            writeProperties(properties, directory, METADATA_KEY);
        } catch (IOException e) {
            // TODO!
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void removeMetadataFacets(RepositorySession session, String repositoryId, String facetId)
            throws MetadataRepositoryException {
        try {
            Path dir = getMetadataDirectory(repositoryId, facetId);
            org.apache.archiva.common.utils.FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public void removeMetadataFacet(RepositorySession session, String repoId, String facetId, String name)
            throws MetadataRepositoryException {
        try {
            Path dir = getMetadataDirectory(repoId, facetId).resolve(name);
            org.apache.archiva.common.utils.FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange(RepositorySession session, String repoId, ZonedDateTime startTime, ZonedDateTime endTime)
            throws MetadataRepositoryException {
        try {
            List<ArtifactMetadata> artifacts = new ArrayList<>();
            for (String ns : getRootNamespaces(session, repoId)) {
                getArtifactsByDateRange(session, artifacts, repoId, ns, startTime, endTime);
            }
            artifacts.sort(new ArtifactComparator());
            return artifacts;
        } catch (MetadataResolutionException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }


    /**
     * Result is sorted by date,
     *
     * @param session        The repository session
     * @param repositoryId   The repository id
     * @param startTime      The start time, can be <code>null</code>
     * @param endTime        The end time, can be <code>null</code>
     * @param queryParameter Additional parameters for the query that affect ordering and number of returned results.
     * @return
     * @throws MetadataRepositoryException
     */
    @Override
    public Stream<ArtifactMetadata> getArtifactByDateRangeStream( RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime, QueryParameter queryParameter) throws MetadataRepositoryException {
        try {
            List<ArtifactMetadata> artifacts = new ArrayList<>();
            for (String ns : getRootNamespaces(session, repositoryId)) {
                getArtifactsByDateRange(session, artifacts, repositoryId, ns, startTime, endTime);
            }
            Comparator<ArtifactMetadata> comp = getArtifactMetadataComparator(queryParameter, "whenGathered");
            return artifacts.stream().sorted(comp).skip(queryParameter.getOffset()).limit(queryParameter.getLimit());

        } catch (MetadataResolutionException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }


    private void getArtifactsByDateRange(RepositorySession session, List<ArtifactMetadata> artifacts, String repoId, String ns, ZonedDateTime startTime,
                                         ZonedDateTime endTime)
            throws MetadataRepositoryException {
        try {
            for (String namespace : this.getChildNamespaces(session, repoId, ns)) {
                getArtifactsByDateRange(session, artifacts, repoId, ns + "." + namespace, startTime, endTime);
            }

            for (String project : getProjects(session, repoId, ns)) {
                for (String version : getProjectVersions(session, repoId, ns, project)) {
                    for (ArtifactMetadata artifact : getArtifacts(session, repoId, ns, project, version)) {
                        if (startTime == null || startTime.isBefore(ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())))) {
                            if (endTime == null || endTime.isAfter(ZonedDateTime.from(artifact.getWhenGathered().toInstant().atZone(ZoneId.systemDefault())))) {
                                artifacts.add(artifact);
                            }
                        }
                    }
                }
            }
        } catch (MetadataResolutionException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }


    @Override
    public List<ArtifactMetadata> getArtifacts( RepositorySession session, String repoId, String namespace, String projectId,
                                                String projectVersion)
            throws MetadataResolutionException {
        try {
            Map<String, ArtifactMetadata> artifacts = new HashMap<>();

            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + projectVersion);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);

            for (Map.Entry entry : properties.entrySet()) {
                String name = (String) entry.getKey();
                StringTokenizer tok = new StringTokenizer(name, ":");
                if (tok.hasMoreTokens() && "artifact".equals(tok.nextToken())) {
                    String field = tok.nextToken();
                    String id = tok.nextToken();

                    ArtifactMetadata artifact = artifacts.get(id);
                    if (artifact == null) {
                        artifact = new ArtifactMetadata();
                        artifact.setRepositoryId(repoId);
                        artifact.setNamespace(namespace);
                        artifact.setProject(projectId);
                        artifact.setProjectVersion(projectVersion);
                        artifact.setVersion(projectVersion);
                        artifact.setId(id);
                        artifacts.put(id, artifact);
                    }

                    String value = (String) entry.getValue();
                    if ("updated".equals(field)) {
                        artifact.setFileLastModified(Long.parseLong(value));
                    } else if ("size".equals(field)) {
                        artifact.setSize(Long.valueOf(value));
                    } else if ("whenGathered".equals(field)) {
                        artifact.setWhenGathered(ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(value)), ZoneId.of("GMT")));
                    } else if ("version".equals(field)) {
                        artifact.setVersion(value);
                    } else if (field.startsWith("checksum")) {
                        String algorithmStr = StringUtils.removeStart( name, "artifact:checksum:"+id+":");
                        artifact.setChecksum( ChecksumAlgorithm.valueOf( algorithmStr ), value );
                    } else if ("facetIds".equals(field)) {
                        if (value.length() > 0) {
                            String propertyPrefix = "artifact:facet:" + id + ":";
                            for (String facetId : value.split(",")) {
                                MetadataFacetFactory factory = getFacetFactory(facetId);
                                if (factory == null) {
                                    log.error("Attempted to load unknown artifact metadata facet: {}", facetId);
                                } else {
                                    MetadataFacet facet = factory.createMetadataFacet();
                                    String prefix = propertyPrefix + facet.getFacetId();
                                    Map<String, String> map = new HashMap<>();
                                    for (Object key : new ArrayList<>(properties.keySet())) {
                                        String property = (String) key;
                                        if (property.startsWith(prefix)) {
                                            map.put(property.substring(prefix.length() + 1),
                                                    properties.getProperty(property));
                                        }
                                    }
                                    facet.fromProperties(map);
                                    artifact.addFacet(facet);
                                }
                            }
                        }

                        updateArtifactFacets(artifact, properties);
                    }
                }
            }
            return new ArrayList<>(artifacts.values());
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }


    @Override
    public void close() {
        // nothing additional to close
    }


    private void updateArtifactFacets(ArtifactMetadata artifact, Properties properties) {
        String propertyPrefix = "artifact:facet:" + artifact.getId() + ":";
        for (MetadataFacet facet : artifact.getFacetList()) {
            for (Map.Entry<String, String> e : facet.toProperties().entrySet()) {
                String key = propertyPrefix + facet.getFacetId() + ":" + e.getKey();
                properties.setProperty(key, e.getValue());
            }
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByChecksum(RepositorySession session, String repositoryId, String checksum)
            throws MetadataRepositoryException {
        try {
            // TODO: this is quite slow - if we are to persist with this repository implementation we should build an index
            //  of this information (eg. in Lucene, as before)
            // alternatively, we could build a referential tree in the content repository, however it would need some levels
            // of depth to avoid being too broad to be useful (eg. /repository/checksums/a/ab/abcdef1234567)

            return getArtifactStream( session, repositoryId ).filter(
                a -> a.hasChecksum( checksum )
            ).collect( Collectors.toList() );
        } catch (MetadataResolutionException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public void removeNamespace(RepositorySession session, String repositoryId, String project)
            throws MetadataRepositoryException {
        try {
            Path namespaceDirectory = getDirectory(repositoryId).resolve(project);
            org.apache.archiva.common.utils.FileUtils.deleteDirectory(namespaceDirectory);
            //Properties properties = new Properties();
            //properties.setProperty( "namespace", namespace );
            //writeProperties( properties, namespaceDirectory, NAMESPACE_METADATA_KEY );

        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public void removeTimestampedArtifact( RepositorySession session, ArtifactMetadata artifactMetadata, String baseVersion)
            throws MetadataRepositoryException {

        try {
            Path directory = getDirectory(artifactMetadata.getRepositoryId()).resolve(
                    artifactMetadata.getNamespace() + "/" + artifactMetadata.getProject() + "/"
                            + baseVersion);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);

            String id = artifactMetadata.getId();

            properties.remove("artifact:updated:" + id);
            properties.remove("artifact:whenGathered:" + id);
            properties.remove("artifact:size:" + id);
            artifactMetadata.getChecksums().entrySet().stream().forEach( entry ->
                properties.remove( "artifact:checksum:"+id+":"+entry.getKey().name() ));
            properties.remove("artifact:version:" + id);
            properties.remove("artifact:facetIds:" + id);

            String prefix = "artifact:facet:" + id + ":";
            for (Object key : new ArrayList<>(properties.keySet())) {
                String property = (String) key;
                if (property.startsWith(prefix)) {
                    properties.remove(property);
                }
            }

            writeProperties(properties, directory, PROJECT_VERSION_METADATA_KEY);
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }

    }

    @Override
    public void removeArtifact(RepositorySession session, String repoId, String namespace, String project, String version, String id)
            throws MetadataRepositoryException {
        try {
            Path directory = getDirectory(repoId).resolve(namespace + "/" + project + "/" + version);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);

            properties.remove("artifact:updated:" + id);
            properties.remove("artifact:whenGathered:" + id);
            properties.remove("artifact:size:" + id);
            properties.remove("artifact:version:" + id);
            properties.remove("artifact:facetIds:" + id);

            String facetPrefix = "artifact:facet:" + id + ":";
            String checksumPrefix = "artifact:checksum:"+id+":";
            for (String property  : properties.stringPropertyNames()) {
                if (property.startsWith( checksumPrefix )) {
                    properties.remove( property );
                } else if (property.startsWith(facetPrefix)) {
                    properties.remove(property);
                }
            }

            org.apache.archiva.common.utils.FileUtils.deleteDirectory(directory);
            //writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    /**
     * FIXME implements this !!!!
     *
     * @param session
     * @param repositoryId
     * @param namespace
     * @param project
     * @param projectVersion
     * @param metadataFacet  will remove artifacts which have this {@link MetadataFacet} using equals
     * @throws MetadataRepositoryException
     */
    @Override
    public void removeFacetFromArtifact( RepositorySession session, String repositoryId, String namespace, String project, String projectVersion,
                                         MetadataFacet metadataFacet)
            throws MetadataRepositoryException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void removeRepository(RepositorySession session, String repoId)
            throws MetadataRepositoryException {
        try {
            Path dir = getDirectory(repoId);
            org.apache.archiva.common.utils.FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }


    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionFacet( RepositorySession session, String key, String value, String repositoryId)
            throws MetadataRepositoryException {
        throw new UnsupportedOperationException("not yet implemented in File backend");
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByAttribute( RepositorySession session, String key, String value, String repositoryId)
            throws MetadataRepositoryException {
        throw new UnsupportedOperationException("not yet implemented in File backend");
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionAttribute( RepositorySession session, String key, String value, String repositoryId)
            throws MetadataRepositoryException {
        throw new UnsupportedOperationException("getArtifactsByProjectVersionAttribute not yet implemented in File backend");
    }

    private Path getMetadataDirectory(String repoId, String facetId)
            throws IOException {
        return getBaseDirectory(repoId).resolve("facets/" + facetId);
    }

    private String join(Collection<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            StringBuilder s = new StringBuilder();
            for (String id : ids) {
                s.append(id);
                s.append(",");
            }
            return s.substring(0, s.length() - 1);
        }
        return "";
    }

    private void setProperty(Properties properties, String name, String value) {
        if (value != null) {
            properties.setProperty(name, value);
        }
    }

    @Override
    public void updateArtifact(RepositorySession session, String repoId, String namespace, String projectId, String projectVersion,
                               ArtifactMetadata artifact) {
        try {
            ProjectVersionMetadata metadata = new ProjectVersionMetadata();
            metadata.setId(projectVersion);
            updateProjectVersion(session, repoId, namespace, projectId, metadata);

            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + projectVersion);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);

            clearMetadataFacetProperties(artifact.getFacetList(), properties,
                    "artifact:facet:" + artifact.getId() + ":");

            String id = artifact.getId();
            properties.setProperty("artifact:updated:" + id,
                    Long.toString(artifact.getFileLastModified().toInstant().toEpochMilli()));
            properties.setProperty("artifact:whenGathered:" + id,
                    Long.toString(artifact.getWhenGathered().toInstant().toEpochMilli()));
            properties.setProperty("artifact:size:" + id, Long.toString(artifact.getSize()));
            artifact.getChecksums().entrySet().stream().forEach( entry ->
                properties.setProperty( "artifact:checksum:"+id+":"+entry.getKey().name(), entry.getValue() ));
            properties.setProperty("artifact:version:" + id, artifact.getVersion());

            Set<String> facetIds = new LinkedHashSet<>(artifact.getFacetIds());
            String property = "artifact:facetIds:" + id;
            facetIds.addAll(Arrays.asList(properties.getProperty(property, "").split(",")));
            properties.setProperty(property, join(facetIds));

            updateArtifactFacets(artifact, properties);

            writeProperties(properties, directory, PROJECT_VERSION_METADATA_KEY);
        } catch (IOException e) {
            // TODO
            log.error(e.getMessage(), e);
        }
    }

    private Properties readOrCreateProperties(Path directory, String propertiesKey) {
        try {
            return readProperties(directory, propertiesKey);
        } catch (FileNotFoundException | NoSuchFileException e) {
            // ignore and return new properties
        } catch (IOException e) {
            // TODO
            log.error(e.getMessage(), e);
        }
        return new Properties();
    }

    private Properties readProperties(Path directory, String propertiesKey)
            throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(directory.resolve(propertiesKey + ".properties"))) {

            properties.load(in);
        }
        return properties;
    }

    @Override
    public ProjectMetadata getProject(RepositorySession session, String repoId, String namespace, String projectId)
            throws MetadataResolutionException {
        try {
            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId);

            Properties properties = readOrCreateProperties(directory, PROJECT_METADATA_KEY);

            ProjectMetadata project = null;

            String id = properties.getProperty("id");
            if (id != null) {
                project = new ProjectMetadata();
                project.setNamespace(properties.getProperty("namespace"));
                project.setId(id);
            }

            return project;
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public ProjectVersionMetadata getProjectVersion(RepositorySession session, String repoId, String namespace, String projectId,
                                                    String projectVersion)
            throws MetadataResolutionException {
        try {
            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + projectVersion);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);
            String id = properties.getProperty("id");
            ProjectVersionMetadata versionMetadata = null;
            if (id != null) {
                versionMetadata = new ProjectVersionMetadata();
                versionMetadata.setId(id);
                versionMetadata.setName(properties.getProperty("name"));
                versionMetadata.setDescription(properties.getProperty("description"));
                versionMetadata.setUrl(properties.getProperty("url"));
                versionMetadata.setIncomplete(Boolean.valueOf(properties.getProperty("incomplete", "false")));

                String scmConnection = properties.getProperty("scm.connection");
                String scmDeveloperConnection = properties.getProperty("scm.developerConnection");
                String scmUrl = properties.getProperty("scm.url");
                if (scmConnection != null || scmDeveloperConnection != null || scmUrl != null) {
                    Scm scm = new Scm();
                    scm.setConnection(scmConnection);
                    scm.setDeveloperConnection(scmDeveloperConnection);
                    scm.setUrl(scmUrl);
                    versionMetadata.setScm(scm);
                }

                String ciSystem = properties.getProperty("ci.system");
                String ciUrl = properties.getProperty("ci.url");
                if (ciSystem != null || ciUrl != null) {
                    CiManagement ci = new CiManagement();
                    ci.setSystem(ciSystem);
                    ci.setUrl(ciUrl);
                    versionMetadata.setCiManagement(ci);
                }

                String issueSystem = properties.getProperty("issue.system");
                String issueUrl = properties.getProperty("issue.url");
                if (issueSystem != null || issueUrl != null) {
                    IssueManagement issueManagement = new IssueManagement();
                    issueManagement.setSystem(issueSystem);
                    issueManagement.setUrl(issueUrl);
                    versionMetadata.setIssueManagement(issueManagement);
                }

                String orgName = properties.getProperty("org.name");
                String orgUrl = properties.getProperty("org.url");
                if (orgName != null || orgUrl != null) {
                    Organization org = new Organization();
                    org.setName(orgName);
                    org.setUrl(orgUrl);
                    versionMetadata.setOrganization(org);
                }

                boolean done = false;
                int i = 0;
                while (!done) {
                    String licenseName = properties.getProperty("license." + i + ".name");
                    String licenseUrl = properties.getProperty("license." + i + ".url");
                    if (licenseName != null || licenseUrl != null) {
                        License license = new License();
                        license.setName(licenseName);
                        license.setUrl(licenseUrl);
                        versionMetadata.addLicense(license);
                    } else {
                        done = true;
                    }
                    i++;
                }

                done = false;
                i = 0;
                while (!done) {
                    String mailingListName = properties.getProperty("mailingList." + i + ".name");
                    if (mailingListName != null) {
                        MailingList mailingList = new MailingList();
                        mailingList.setName(mailingListName);
                        mailingList.setMainArchiveUrl(properties.getProperty("mailingList." + i + ".archive"));
                        String p = properties.getProperty("mailingList." + i + ".otherArchives");
                        if (p != null && p.length() > 0) {
                            mailingList.setOtherArchives(Arrays.asList(p.split(",")));
                        } else {
                            mailingList.setOtherArchives(Collections.emptyList());
                        }
                        mailingList.setPostAddress(properties.getProperty("mailingList." + i + ".post"));
                        mailingList.setSubscribeAddress(properties.getProperty("mailingList." + i + ".subscribe"));
                        mailingList.setUnsubscribeAddress(
                                properties.getProperty("mailingList." + i + ".unsubscribe"));
                        versionMetadata.addMailingList(mailingList);
                    } else {
                        done = true;
                    }
                    i++;
                }

                done = false;
                i = 0;
                while (!done) {
                    String dependencyArtifactId = properties.getProperty("dependency." + i + ".artifactId");
                    if (dependencyArtifactId != null) {
                        Dependency dependency = new Dependency();
                        dependency.setArtifactId(dependencyArtifactId);
                        dependency.setNamespace(properties.getProperty("dependency." + i + ".groupId"));
                        dependency.setClassifier(properties.getProperty("dependency." + i + ".classifier"));
                        dependency.setOptional(
                                Boolean.valueOf(properties.getProperty("dependency." + i + ".optional")));
                        dependency.setScope(properties.getProperty("dependency." + i + ".scope"));
                        dependency.setSystemPath(properties.getProperty("dependency." + i + ".systemPath"));
                        dependency.setType(properties.getProperty("dependency." + i + ".type"));
                        dependency.setVersion(properties.getProperty("dependency." + i + ".version"));
                        dependency.setOptional(
                                Boolean.valueOf(properties.getProperty("dependency." + i + ".optional")));
                        versionMetadata.addDependency(dependency);
                    } else {
                        done = true;
                    }
                    i++;
                }

                String facetIds = properties.getProperty("facetIds", "");
                if (facetIds.length() > 0) {
                    for (String facetId : facetIds.split(",")) {
                        MetadataFacetFactory factory = getFacetFactory(facetId);
                        if (factory == null) {
                            log.error("Attempted to load unknown project version metadata facet: {}", facetId);
                        } else {
                            MetadataFacet facet = factory.createMetadataFacet();
                            Map<String, String> map = new HashMap<>();
                            for (Object key : new ArrayList<>(properties.keySet())) {
                                String property = (String) key;
                                if (property.startsWith(facet.getFacetId())) {
                                    map.put(property.substring(facet.getFacetId().length() + 1),
                                            properties.getProperty(property));
                                }
                            }
                            facet.fromProperties(map);
                            versionMetadata.addFacet(facet);
                        }
                    }
                }

                updateProjectVersionFacets(versionMetadata, properties);
            }
            return versionMetadata;
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getArtifactVersions( RepositorySession session, String repoId, String namespace, String projectId,
                                             String projectVersion)
            throws MetadataResolutionException {
        try {
            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + projectVersion);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);

            Set<String> versions = new HashSet<>();
            for (Map.Entry entry : properties.entrySet()) {
                String name = (String) entry.getKey();
                if (name.startsWith("artifact:version:")) {
                    versions.add((String) entry.getValue());
                }
            }
            return new ArrayList<>( versions );
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public List<ProjectVersionReference> getProjectReferences( RepositorySession session, String repoId, String namespace, String projectId,
                                                               String projectVersion)
            throws MetadataResolutionException {
        try {
            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + projectVersion);

            Properties properties = readOrCreateProperties(directory, PROJECT_VERSION_METADATA_KEY);
            int numberOfRefs = Integer.parseInt(properties.getProperty("ref:lastReferenceNum", "-1")) + 1;

            List<ProjectVersionReference> references = new ArrayList<>();
            for (int i = 0; i < numberOfRefs; i++) {
                ProjectVersionReference reference = new ProjectVersionReference();
                reference.setProjectId(properties.getProperty("ref:reference." + i + ".projectId"));
                reference.setNamespace(properties.getProperty("ref:reference." + i + ".namespace"));
                reference.setProjectVersion(properties.getProperty("ref:reference." + i + ".projectVersion"));
                reference.setReferenceType(ProjectVersionReference.ReferenceType.valueOf(
                        properties.getProperty("ref:reference." + i + ".referenceType")));
                references.add(reference);
            }
            return references;
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getRootNamespaces( RepositorySession session, String repoId)
            throws MetadataResolutionException {
        return this.getChildNamespaces(session, repoId, null);
    }

    private Stream<String> getAllNamespacesStream(RepositorySession session, String repoId) {
        Path directory = null;
        try
        {
            directory = getDirectory(repoId);
        }
        catch ( IOException e )
        {
            return Stream.empty( );
        }
        if (!(Files.exists(directory) && Files.isDirectory(directory))) {
            return Stream.empty( );
        }
        final String searchFile = NAMESPACE_METADATA_KEY + ".properties";
        try
        {
            return  Files.list(directory).filter(Files::isDirectory).filter(path ->
                    Files.exists(path.resolve(searchFile))
                ).map(path -> path.getFileName().toString());
        }
        catch ( IOException e )
        {
            return Stream.empty( );
        }
    }

    @Override
    public List<String> getChildNamespaces( RepositorySession session, String repoId, String baseNamespace)
            throws MetadataResolutionException {
        try {
            List<String> allNamespaces;
            Path directory = getDirectory(repoId);
            if (!(Files.exists(directory) && Files.isDirectory(directory))) {
                return Collections.emptyList();
            }
            final String searchFile = NAMESPACE_METADATA_KEY + ".properties";
            try (Stream<Path> fs = Files.list(directory)) {
                allNamespaces = fs.filter(Files::isDirectory).filter(path ->
                        Files.exists(path.resolve(searchFile))
                ).map(path -> path.getFileName().toString()).collect(Collectors.toList());
            }

            Set<String> namespaces = new LinkedHashSet<>();
            int fromIndex = baseNamespace != null ? baseNamespace.length() + 1 : 0;
            for (String namespace : allNamespaces) {
                if (baseNamespace == null || namespace.startsWith(baseNamespace + ".")) {
                    int i = namespace.indexOf('.', fromIndex);
                    if (i >= 0) {
                        namespaces.add(namespace.substring(fromIndex, i));
                    } else {
                        namespaces.add(namespace.substring(fromIndex));
                    }
                }
            }
            return new ArrayList<>(namespaces);
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getProjects( RepositorySession session, String repoId, String namespace)
            throws MetadataResolutionException {
        try {
            List<String> projects;
            Path directory = getDirectory(repoId).resolve(namespace);
            if (!(Files.exists(directory) && Files.isDirectory(directory))) {
                return Collections.emptyList();
            }
            final String searchFile = PROJECT_METADATA_KEY + ".properties";
            try (Stream<Path> fs = Files.list(directory)) {
                projects = fs.filter(Files::isDirectory).filter(path ->
                        Files.exists(path.resolve(searchFile))
                ).map(path -> path.getFileName().toString()).collect(Collectors.toList());
            }

            return projects;
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getProjectVersions( RepositorySession session, String repoId, String namespace, String projectId)
            throws MetadataResolutionException {
        try {
            List<String> projectVersions;
            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId);
            if (!(Files.exists(directory) && Files.isDirectory(directory))) {
                return Collections.emptyList();
            }
            final String searchFile = PROJECT_VERSION_METADATA_KEY + ".properties";
            try (Stream<Path> fs = Files.list(directory)) {
                projectVersions = fs.filter(Files::isDirectory).filter(path ->
                        Files.exists(path.resolve(searchFile))
                ).map(path -> path.getFileName().toString()).collect(Collectors.toList());
            }
            return projectVersions;
        } catch (IOException e) {
            throw new MetadataResolutionException(e.getMessage(), e);
        }
    }

    @Override
    public void removeProject(RepositorySession session, String repositoryId, String namespace, String projectId)
            throws MetadataRepositoryException {
        try {
            Path directory = getDirectory(repositoryId).resolve(namespace + "/" + projectId);
            org.apache.archiva.common.utils.FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public void removeProjectVersion(RepositorySession session, String repoId, String namespace, String projectId, String projectVersion)
            throws MetadataRepositoryException {
        try {
            Path directory = getDirectory(repoId).resolve(namespace + "/" + projectId + "/" + projectVersion);
            org.apache.archiva.common.utils.FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }

    }

    private void writeProperties(Properties properties, Path directory, String propertiesKey)
            throws IOException {
        Files.createDirectories(directory);
        try (OutputStream os = Files.newOutputStream(directory.resolve(propertiesKey + ".properties"))) {
            properties.store(os, null);
        }
    }

    private static class ArtifactComparator
            implements Comparator<ArtifactMetadata> {
        @Override
        public int compare(ArtifactMetadata artifact1, ArtifactMetadata artifact2) {
            if (artifact1.getWhenGathered() == artifact2.getWhenGathered()) {
                return 0;
            }
            if (artifact1.getWhenGathered() == null) {
                return 1;
            }
            if (artifact2.getWhenGathered() == null) {
                return -1;
            }
            return artifact1.getWhenGathered().compareTo(artifact2.getWhenGathered());
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifacts(RepositorySession session, String repoId)
            throws MetadataRepositoryException {
        try {
            List<ArtifactMetadata> artifacts = new ArrayList<>();
            for (String ns : getRootNamespaces(session, repoId)) {
                getArtifacts(session, artifacts, repoId, ns);
            }
            return artifacts;
        } catch (MetadataResolutionException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    private class ArtifactCoordinates {
        final String namespace;
        final String project;
        final String version;

        public ArtifactCoordinates(String namespace, String project, String version) {
            this.namespace = namespace;
            this.project = project;
            this.version = version;
        }

        public String getNamespace( )
        {
            return namespace;
        }

        public String getProject( )
        {
            return project;
        }

        public String getVersion( )
        {
            return version;
        }
    }

    @Override
    public Stream<ArtifactMetadata> getArtifactStream(  final RepositorySession session,  final String repositoryId,
                                                        QueryParameter queryParameter ) throws MetadataResolutionException
    {

        return getAllNamespacesStream( session, repositoryId ).filter( Objects::nonNull ).flatMap( ns ->
            {
                try
                {
                    return getProjects( session, repositoryId, ns ).stream( ).map( proj ->
                        new ArtifactCoordinates( ns, proj, null ) );
                }
                catch ( MetadataResolutionException e )
                {
                    return null;
                }
            }
        ).filter( Objects::nonNull ).flatMap( artifactCoordinates ->
            {
                try
                {
                    return getProjectVersions( session, repositoryId, artifactCoordinates.getNamespace( ), artifactCoordinates.getProject( ) )
                        .stream( ).map(version -> new ArtifactCoordinates( artifactCoordinates.getNamespace(), artifactCoordinates.getProject(), version ));
                }
                catch ( MetadataResolutionException e )
                {
                    return null;
                }
            }
        ).filter( Objects::nonNull ).flatMap( ac ->
            {
                try
                {
                    return getArtifactStream( session, repositoryId, ac.getNamespace(), ac.getProject(), ac.getVersion() );
                }
                catch ( MetadataResolutionException e )
                {
                    return null;
                }
            }
            ).filter( Objects::nonNull );
    }

    @Override
    public Stream<ArtifactMetadata> getArtifactStream( final RepositorySession session, final String repoId,
                                                       final String namespace, final String projectId,
                                                       final String projectVersion ) throws MetadataResolutionException
    {
        return getArtifacts( session, repoId, namespace, projectId, projectVersion ).stream( );
    }

    private void getArtifacts(RepositorySession session, List<ArtifactMetadata> artifacts, String repoId, String ns)
            throws MetadataResolutionException {
        for (String namespace : this.getChildNamespaces(session, repoId, ns)) {
            getArtifacts(session, artifacts, repoId, ns + "." + namespace);
        }

        for (String project : getProjects(session, repoId, ns)) {
            for (String version : getProjectVersions(session, repoId, ns, project)) {
                artifacts.addAll(getArtifacts(session, repoId, ns, project, version));
            }
        }
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts(RepositorySession session, String repositoryId, String text, boolean exact) {
        throw new UnsupportedOperationException("searchArtifacts not yet implemented in File backend");
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts(RepositorySession session, String repositoryId, String key, String text, boolean exact) {
        throw new UnsupportedOperationException("searchArtifacts not yet implemented in File backend");
    }
}
