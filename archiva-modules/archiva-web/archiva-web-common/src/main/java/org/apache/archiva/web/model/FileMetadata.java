package org.apache.archiva.web.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "fileMetadata" )
public class FileMetadata
    implements Serializable
{
    private String name;

    private String serverFileName;

    private long size;

    private String url;

    private String deleteUrl;

    private String deleteType;

    private String errorKey;

    private String classifier;

    private String packaging;

    private boolean pomFile;

    public FileMetadata()
    {
        // no op
    }

    public FileMetadata( String serverFileName )
    {
        this.serverFileName = serverFileName;
    }

    public FileMetadata( String name, long size, String url )
    {
        this.name = name;
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

    public String getDeleteUrl()
    {
        return deleteUrl;
    }

    public void setDeleteUrl( String deleteUrl )
    {
        this.deleteUrl = deleteUrl;
    }

    public String getDeleteType()
    {
        return deleteType;
    }

    public void setDeleteType( String deleteType )
    {
        this.deleteType = deleteType;
    }

    public String getErrorKey()
    {
        return errorKey;
    }

    public void setErrorKey( String errorKey )
    {
        this.errorKey = errorKey;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }


    public boolean isPomFile()
    {
        return pomFile;
    }

    public void setPomFile( boolean pomFile )
    {
        this.pomFile = pomFile;
    }

    public String getServerFileName()
    {
        return serverFileName;
    }

    public void setServerFileName( String serverFileName )
    {
        this.serverFileName = serverFileName;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof FileMetadata ) )
        {
            return false;
        }

        FileMetadata that = (FileMetadata) o;

        if ( !serverFileName.equals( that.serverFileName ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return serverFileName.hashCode();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "FileMetadata{" );
        sb.append( "name='" ).append( name ).append( '\'' );
        sb.append( ", serverFileName='" ).append( serverFileName ).append( '\'' );
        sb.append( ", size=" ).append( size );
        sb.append( ", url='" ).append( url ).append( '\'' );
        sb.append( ", deleteUrl='" ).append( deleteUrl ).append( '\'' );
        sb.append( ", deleteType='" ).append( deleteType ).append( '\'' );
        sb.append( ", errorKey='" ).append( errorKey ).append( '\'' );
        sb.append( ", classifier='" ).append( classifier ).append( '\'' );
        sb.append( ", packaging='" ).append( packaging ).append( '\'' );
        sb.append( ", pomFile=" ).append( pomFile );
        sb.append( '}' );
        return sb.toString();
    }
}
