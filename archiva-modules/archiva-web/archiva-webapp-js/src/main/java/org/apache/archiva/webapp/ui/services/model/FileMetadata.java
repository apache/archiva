package org.apache.archiva.webapp.ui.services.model;
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "fileMetadata" )
public class FileMetadata
{
    private String name;

    private long size;

    private String url;

    private String deleteUrl;

    private String deleteType;

    public FileMetadata()
    {
        // no op
    }

    public FileMetadata( String filename, long size, String url )
    {
        this.name = filename;
        this.size = size;
        this.url = url;
        this.deleteUrl = url;
        this.deleteType = "DELETE";
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize( long size )
    {
        this.size = size;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    @XmlElement( name = "delete_url" )
    public String getDeleteUrl()
    {
        return deleteUrl;
    }


    public void setDeleteUrl( String deleteUrl )
    {
        this.deleteUrl = deleteUrl;
    }

    @XmlElement( name = "delete_type" )
    public String getDeleteType()
    {
        return deleteType;
    }

    public void setDeleteType( String deleteType )
    {
        this.deleteType = deleteType;
    }
}
