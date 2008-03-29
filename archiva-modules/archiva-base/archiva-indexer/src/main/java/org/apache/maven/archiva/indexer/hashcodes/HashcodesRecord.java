package org.apache.maven.archiva.indexer.hashcodes;

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

/**
 * Lucene record for {@link ArchivaArtifact} hashcodes information.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class HashcodesRecord
    implements LuceneRepositoryContentRecord
{
    private String repositoryId;

    private ArchivaArtifact artifact;

    private String filename;

    public ArchivaArtifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( ArchivaArtifact artifact )
    {
        this.artifact = artifact;
    }

    public String getPrimaryKey()
    {
        StringBuffer id = new StringBuffer();
        id.append( artifact.getGroupId() ).append( ":" );
        id.append( artifact.getArtifactId() ).append( ":" );
        id.append( artifact.getVersion() );

        if ( artifact.getClassifier() != null )
        {
            id.append( ":" ).append( artifact.getClassifier() );
        }

        id.append( ":" ).append( artifact.getType() );

        return id.toString();
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( artifact == null ) ? 0 : artifact.hashCode() );
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

        final HashcodesRecord other = (HashcodesRecord) obj;

        if ( artifact == null )
        {
            if ( other.artifact != null )
            {
                return false;
            }
        }
        else if ( !artifact.equals( other.artifact ) )
        {
            return false;
        }

        return true;
    }

    public String getRepositoryId()
    {
        return this.repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename( String filename )
    {
        this.filename = filename;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "HashcodesRecord[" );
        sb.append( "artifact=" ).append( artifact );
        sb.append( ",filename=" ).append( filename );
        sb.append( "]" );
        return sb.toString();
    }
}
