package org.apache.archiva.metadata.model;

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
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metadata stored in the content repository for a particular artifact. Information that is shared between different
 * artifacts of a given project version can be found in the
 * {@link org.apache.archiva.metadata.model.ProjectVersionMetadata} class. The metadata is faceted to store information
 * about particular types of artifacts, for example Maven 2.x artifact specific information.
 * For more information, see the
 * <a href="{@docRoot}/../metadata-content-model.html" target="_top">Metadata Content Model</a>.
 */
@XmlRootElement(name = "artifactMetadata")
public class ArtifactMetadata
        extends FacetedMetadata {


    /**
     * The artifact ID uniquely identifies an artifact within a given namespace, project and project version. For
     * example, <tt>archiva-1.4-20100201.345612-2.jar</tt>
     */
    private String id;

    /**
     * The repository that the artifact is stored in within the content repository.
     */
    private String repositoryId;

    /**
     * The namespace of the project within the repository.
     *
     * @see ProjectMetadata#getNamespace()
     */
    private String namespace;

    /**
     * The identifier of the project within the repository and namespace.
     *
     * @see ProjectMetadata#getId()
     */
    private String project;

    /**
     * The version of the project. This may be more generalised than @{link #version}.
     *
     * @see ProjectVersionMetadata#getId()
     */
    private String projectVersion;

    /**
     * The artifact version, if different from the project version. Note that the metadata does not do any calculation
     * of this based on the project version - the calling code must be sure to set and check it appropriately if
     * <tt>null</tt>.
     */
    private String version;

    /**
     * The last modified date of the artifact file, if known.
     */
    private ZonedDateTime fileLastModified;

    /**
     * The file size of the artifact, if known.
     */
    private long size;

    /**
     * The list of checksums.
     */
    private Map<ChecksumAlgorithm, String> checksums = new DualHashBidiMap<>( );

    private String toStringValue = "";
    private int lastHash = 0;

    /**
     * When the artifact was found in the repository storage and added to the metadata content repository.
     */
    private ZonedDateTime whenGathered;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public void setFileLastModified(long fileLastModified) {
        this.fileLastModified = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fileLastModified), ModelInfo.STORAGE_TZ);
    }

    public void setWhenGathered(ZonedDateTime whenGathered) {
        // We set the resolution to milliseconds, because it's the resolution that all current backends support
        this.whenGathered = whenGathered.withZoneSameInstant(ModelInfo.STORAGE_TZ).truncatedTo(ChronoUnit.MILLIS);
    }

    public void setMd5(String md5) {
        this.checksums.put(ChecksumAlgorithm.MD5, md5);
    }

    public void setSha1(String sha1) {
        this.checksums.put(ChecksumAlgorithm.SHA1, sha1);
    }

    public ZonedDateTime getWhenGathered() {
        return whenGathered;
    }

    public String getChecksum(ChecksumAlgorithm checksumAlgorithm) {
        return checksums.get(checksumAlgorithm);
    }

    public void setChecksum(ChecksumAlgorithm algorithm, String checksumValue) {
        this.checksums.put(algorithm, checksumValue);
    }

    public Set<ChecksumAlgorithm> getChecksumTypes() {
        return checksums.keySet();
    }

    public Map<ChecksumAlgorithm,String> getChecksums() {
        return this.checksums;
    }

    public boolean hasChecksum(String checksum) {
        return this.checksums.containsValue( checksum );
    }

    public void setChecksums(Map<ChecksumAlgorithm,String> checksums) {
        this.checksums = checksums;
    }

    public String getMd5() {
        return checksums.get(ChecksumAlgorithm.MD5);
    }

    public String getSha1() {
        return checksums.get(ChecksumAlgorithm.SHA1);
    }

    public ZonedDateTime getFileLastModified() {

        return fileLastModified;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getProject() {
        return project;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtifactMetadata that = (ArtifactMetadata) o;

        if (size != that.size) {
            return false;
        }
        // Time equality by instant that means the point in time must match, but not the time zone
        if (fileLastModified != null
                ? !fileLastModified.toInstant().equals(that.fileLastModified.toInstant())
                : that.fileLastModified != null) {
            return false;
        }
        if (!id.equals(that.id)) {
            return false;
        }
        for ( Map.Entry<ChecksumAlgorithm, String> entry : this.checksums.entrySet()) {
            String thatChecksum = that.checksums.get(entry.getKey());
            if (entry.getValue()!=null ? !entry.getValue().equals(thatChecksum) : thatChecksum!=null) {
                return false;
            }
        }
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) {
            return false;
        }
        if (project != null ? !project.equals(that.project) : that.project != null) {
            return false;
        }
        if (projectVersion != null ? !projectVersion.equals(that.projectVersion) : that.projectVersion != null) {
            return false;
        }
        /**
         * We cannot compare in different repositories, if this is in here
         if ( !repositoryId.equals( that.repositoryId ) )
         {
         return false;
         }
         **/
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }
        if (whenGathered != null ? !whenGathered.toInstant().equals(that.whenGathered.toInstant()) : that.whenGathered != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (repositoryId != null ? repositoryId.hashCode() : 0);
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (project != null ? project.hashCode() : 0);
        result = 31 * result + (projectVersion != null ? projectVersion.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (fileLastModified != null ? fileLastModified.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        for (String checksum : checksums.values()) {
            result = 31 * result + (checksum != null ? checksum.hashCode() : 0);
        }
        result = 31 * result + (whenGathered != null ? whenGathered.hashCode() : 0);
        return result;
    }

    /**
     * Doing some hashing to avoid the expensive string concatenation.
     */
    @Override
    public String toString() {
        final int hashCode=hashCode();
        if (hashCode!=lastHash) {
            toStringValue = "ArtifactMetadata{" + "id='" + id + '\'' + ", size=" + size + ", version='" + version + '\'' +
                    ", fileLastModified=" + fileLastModified + ", whenGathered=" + whenGathered +
                    ", namespace='" + namespace + '\'' + ", project='" + project + '\'' +
                    ", projectVersion='" + projectVersion + '\'' + ", repositoryId='" + repositoryId + '\'' +
                    ", checksums=" + checksums.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(",")) +
                    '}';
            lastHash=hashCode;
        }
        return toStringValue;
    }
}
