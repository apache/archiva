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
{
    private String id;
    
    private long size;

    private String version;

    private Date fileLastModified;

    private Date whenGathered;

    private String md5;

    private String sha1;

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
}
