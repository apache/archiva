package org.apache.archiva.metadata.repository.cassandra.model;

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

import org.apache.archiva.metadata.repository.cassandra.CassandraUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.apache.archiva.metadata.repository.cassandra.model.ColumnNames.*;

/**
 * Cassandra storage model for {@link org.apache.archiva.metadata.model.ArtifactMetadata}
 *
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class ArtifactMetadataModel
    implements Serializable
{

    public final static String[] COLUMNS = new String[] { ID.toString(), REPOSITORY_NAME.toString(),
        NAMESPACE_ID.toString(), PROJECT.toString(), PROJECT_VERSION.toString(), VERSION.toString(),
        FILE_LAST_MODIFIED.toString(), SIZE.toString(), MD5.toString(), SHA1.toString(), WHEN_GATHERED.toString() };

    private String id;

    private String repositoryId;

    private String namespace;

    private String project;

    private String projectVersion;

    private String version;

    private long fileLastModified;

    private long size;

    private String md5;

    private String sha1;

    private long whenGathered;

    private Map<String, String> checksums = new HashMap<>();

    public ArtifactMetadataModel()
    {
        // no op
    }

    public ArtifactMetadataModel( String id, String repositoryId, String namespace, String project,
                                  String projectVersion, String version, Date fileLastModified, long size, String md5,
                                  String sha1, Date whenGathered )
    {
        this.id = id;
        this.repositoryId = repositoryId;
        this.namespace = namespace;
        this.project = project;
        this.projectVersion = projectVersion;
        this.version = version;
        this.fileLastModified = ( fileLastModified != null ? fileLastModified.getTime() : 0 );
        this.size = size;
        this.md5 = md5;
        this.sha1 = sha1;
        this.whenGathered = whenGathered != null ? whenGathered.getTime() : new Date().getTime();
    }


    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    public String getProject()
    {
        return project;
    }

    public void setProject( String project )
    {
        this.project = project;
    }

    public String getProjectVersion()
    {
        return projectVersion;
    }

    public void setProjectVersion( String projectVersion )
    {
        this.projectVersion = projectVersion;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public long getFileLastModified()
    {
        return fileLastModified;
    }

    public void setFileLastModified( long fileLastModified )
    {
        this.fileLastModified = fileLastModified;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize( long size )
    {
        this.size = size;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public String getSha1()
    {
        return sha1;
    }

    public void setSha1( String sha1 )
    {
        this.sha1 = sha1;
    }

    public Date getWhenGathered()
    {
        return new Date( whenGathered );
    }

    public void setWhenGathered( long whenGathered )
    {
        this.whenGathered = whenGathered;
    }

    public void setChecksum(String type, String value) {
        this.checksums.put(type, value);
    }

    public String getChecksum(String type) {
        return this.checksums.get(type);
    }

    public void setChecksums(Map<String,String> checksums) {
        this.checksums = checksums;
    }

    public Map<String,String> getChecksums() {
        return this.checksums;
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "ArtifactMetadataModel{" );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( ", repositoryId='" ).append( repositoryId ).append( '\'' );
        sb.append( ", namespace='" ).append( namespace ).append( '\'' );
        sb.append( ", project='" ).append( project ).append( '\'' );
        sb.append( ", projectVersion='" ).append( projectVersion ).append( '\'' );
        sb.append( ", version='" ).append( version ).append( '\'' );
        sb.append( ", fileLastModified=" ).append( fileLastModified );
        sb.append( ", size=" ).append( size );
        sb.append( ", md5='" ).append( md5 ).append( '\'' );
        sb.append( ", sha1='" ).append( sha1 ).append( '\'' );
        sb.append( ", whenGathered=" ).append( whenGathered );
        sb.append( '}' );
        return sb.toString();
    }

    public static class KeyBuilder
    {

        private String project;

        private String id;

        private String namespaceId;

        private String repositoryId;

        private String projectVersion;

        public KeyBuilder()
        {

        }

        public KeyBuilder withId( String id )
        {
            this.id = id;
            return this;
        }


        public KeyBuilder withNamespace( Namespace namespace )
        {
            this.namespaceId = namespace.getName();
            this.repositoryId = namespace.getRepository().getName();
            return this;
        }

        public KeyBuilder withNamespace( String namespaceId )
        {
            this.namespaceId = namespaceId;
            return this;
        }

        public KeyBuilder withProject( String project )
        {
            this.project = project;
            return this;
        }

        public KeyBuilder withProjectVersion( String projectVersion )
        {
            this.projectVersion = projectVersion;
            return this;
        }

        public KeyBuilder withRepositoryId( String repositoryId )
        {
            this.repositoryId = repositoryId;
            return this;
        }

        public String build()
        {
            //repositoryId + namespaceId + project + projectVersion + id
            // FIXME add some controls

            String str =
                CassandraUtils.generateKey( this.repositoryId, this.namespaceId, this.project, this.projectVersion,
                                            this.id );

            //return Long.toString( str.hashCode() );
            return str;
        }
    }

}
