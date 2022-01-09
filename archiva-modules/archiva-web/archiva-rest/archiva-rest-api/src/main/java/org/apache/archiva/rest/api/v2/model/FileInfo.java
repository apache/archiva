package org.apache.archiva.rest.api.v2.model;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.archiva.repository.storage.StorageAsset;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="FileInfo",description = "Information about a file stored in the repository")
public class FileInfo implements Serializable, RestModel
{
    private static final long serialVersionUID = 900497784542880195L;
    private OffsetDateTime modified;
    private String fileName;
    private String path;

    public FileInfo( )
    {
    }

    public static FileInfo of( StorageAsset asset ) {
        FileInfo fileInfo = new FileInfo( );
        fileInfo.setFileName( asset.getName() );
        fileInfo.setPath( asset.getPath() );
        fileInfo.setModified( asset.getModificationTime( ).atOffset( ZoneOffset.UTC ) );
        return fileInfo;
    }

                               @Schema(description = "Time when the file was last modified")
    public OffsetDateTime getModified( )
    {
        return modified;
    }

    public void setModified( OffsetDateTime modified )
    {
        this.modified = modified;
    }

    @Schema(name="file_name", description = "Name of the file")
    public String getFileName( )
    {
        return fileName;
    }

    public void setFileName( String fileName )
    {
        this.fileName = fileName;
    }

    @Schema(name="path", description = "Path to the file relative to the repository directory")
    public String getPath( )
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        FileInfo fileInfo = (FileInfo) o;

        if ( modified != null ? !modified.equals( fileInfo.modified ) : fileInfo.modified != null ) return false;
        if ( fileName != null ? !fileName.equals( fileInfo.fileName ) : fileInfo.fileName != null ) return false;
        return path != null ? path.equals( fileInfo.path ) : fileInfo.path == null;
    }

    @Override
    public int hashCode( )
    {
        int result = modified != null ? modified.hashCode( ) : 0;
        result = 31 * result + ( fileName != null ? fileName.hashCode( ) : 0 );
        result = 31 * result + ( path != null ? path.hashCode( ) : 0 );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "FileInfo{" );
        sb.append( "modified=" ).append( modified );
        sb.append( ", fileName='" ).append( fileName ).append( '\'' );
        sb.append( ", path='" ).append( path ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
