package org.apache.maven.archiva.indexer.filecontent;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.io.File;

/**
 * Lucene record for {@link File} contents. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FileContentRecord
    implements LuceneRepositoryContentRecord
{
    private String repositoryId;

    private String filename;
    
    /**
     * Optional artifact reference for the file content.
     */
    private ArchivaArtifact artifact;

    private String contents;

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public String getContents()
    {
        return contents;
    }

    public void setContents( String contents )
    {
        this.contents = contents;
    }

    public String getPrimaryKey()
    {
        return filename;
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( filename == null ) ? 0 : filename.hashCode() );
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final FileContentRecord other = (FileContentRecord) obj;

        if ( filename == null )
        {
            if ( other.filename != null )
            {
                return false;
            }
        }
        else if ( !filename.equals( other.filename ) )
        {
            return false;
        }
        return true;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename( String filename )
    {
        this.filename = filename;
    }

    public ArchivaArtifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( ArchivaArtifact artifact )
    {
        this.artifact = artifact;
    }
}
