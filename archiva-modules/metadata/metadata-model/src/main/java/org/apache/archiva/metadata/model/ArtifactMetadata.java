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

public class ArtifactMetadata
    extends FacetedMetadata
{
    private String id;
    
    private long size;

    private String version;

    private Date fileLastModified;

    private Date whenGathered;

    private String md5;

    private String sha1;

    private String namespace;

    private String project;

    private String repositoryId;

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
        if ( !fileLastModified.equals( that.fileLastModified ) )
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
        if ( repositoryId != null ? !repositoryId.equals( that.repositoryId ) : that.repositoryId != null )
        {
            return false;
        }
        if ( sha1 != null ? !sha1.equals( that.sha1 ) : that.sha1 != null )
        {
            return false;
        }
        if ( !version.equals( that.version ) )
        {
            return false;
        }
        if ( !whenGathered.equals( that.whenGathered ) )
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
            ", repositoryId='" + repositoryId + '\'' + '}';
    }
}
