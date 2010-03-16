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

import java.util.Date;

/**
 * Metadata stored in the content repository for a particular artifact. Information that is shared between different
 * artifacts of a given project version can be found in the
 * {@link org.apache.archiva.metadata.model.ProjectVersionMetadata} class. The metadata is faceted to store information
 * about particular types of artifacts, for example Maven 2.x artifact specific information.
 * For more information, see the
 * <a href="{@docRoot}/../metadata-content-model.html" target="_top">Metadata Content Model</a>.
 */
public class ArtifactMetadata
    extends FacetedMetadata
{
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
     * @see org.apache.archiva.metadata.model.ProjectMetadata#namespace
     */
    private String namespace;

    /**
     * The identifier of the project within the repository and namespace.
     *
     * @see org.apache.archiva.metadata.model.ProjectMetadata#id
     */
    private String project;

    /**
     * The version of the project. This may be more generalised than @{link #version}.
     *
     * @see org.apache.archiva.metadata.model.ProjectVersionMetadata#id
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
    private Date fileLastModified;

    /**
     * The file size of the artifact, if known.
     */
    private long size;

    /**
     * The MD5 checksum of the artifact, if calculated.
     */
    private String md5;

    /**
     * The SHA-1 checksum of the artifact, if calculated.
     */
    private String sha1;

    /**
     * When the artifact was found in the repository storage and added to the metadata content repository.
     */
    private Date whenGathered;

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize( long size )
    {
        this.size = size;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getProjectVersion()
    {
        return projectVersion;
    }

    public void setProjectVersion( String projectVersion )
    {
        this.projectVersion = projectVersion;
    }

    public void setFileLastModified( long fileLastModified )
    {
        this.fileLastModified = new Date( fileLastModified );
    }

    public void setWhenGathered( Date whenGathered )
    {
        this.whenGathered = whenGathered;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public void setSha1( String sha1 )
    {
        this.sha1 = sha1;
    }

    public Date getWhenGathered()
    {
        return whenGathered;
    }

    public String getMd5()
    {
        return md5;
    }

    public String getSha1()
    {
        return sha1;
    }

    public Date getFileLastModified()
    {

        return fileLastModified;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    public void setProject( String project )
    {
        this.project = project;
    }

    public String getProject()
    {
        return project;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ArtifactMetadata that = (ArtifactMetadata) o;

        if ( size != that.size )
        {
            return false;
        }
        if ( fileLastModified != null
            ? !fileLastModified.equals( that.fileLastModified )
            : that.fileLastModified != null )
        {
            return false;
        }
        if ( !id.equals( that.id ) )
        {
            return false;
        }
        if ( md5 != null ? !md5.equals( that.md5 ) : that.md5 != null )
        {
            return false;
        }
        if ( namespace != null ? !namespace.equals( that.namespace ) : that.namespace != null )
        {
            return false;
        }
        if ( project != null ? !project.equals( that.project ) : that.project != null )
        {
            return false;
        }
        if ( projectVersion != null ? !projectVersion.equals( that.projectVersion ) : that.projectVersion != null )
        {
            return false;
        }
        if ( !repositoryId.equals( that.repositoryId ) )
        {
            return false;
        }
        if ( sha1 != null ? !sha1.equals( that.sha1 ) : that.sha1 != null )
        {
            return false;
        }
        if ( version != null ? !version.equals( that.version ) : that.version != null )
        {
            return false;
        }
        if ( whenGathered != null ? !whenGathered.equals( that.whenGathered ) : that.whenGathered != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "ArtifactMetadata{" + "id='" + id + '\'' + ", size=" + size + ", version='" + version + '\'' +
            ", fileLastModified=" + fileLastModified + ", whenGathered=" + whenGathered + ", md5='" + md5 + '\'' +
            ", sha1='" + sha1 + '\'' + ", namespace='" + namespace + '\'' + ", project='" + project + '\'' +
            ", projectVersion='" + projectVersion + '\'' + ", repositoryId='" + repositoryId + '\'' + '}';
    }
}
