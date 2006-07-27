package org.apache.maven.repository.indexing.record;

import java.util.Date;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The a record with the fields in the minimal index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MinimalArtifactIndexRecord
    implements RepositoryIndexRecord
{
    /**
     * The classes in the archive for the artifact, if it is a JAR.
     */
    private String classes;

    /**
     * The MD5 checksum of the artifact file.
     */
    private String md5Checksum;

    /**
     * The filename of the artifact file (no path).
     */
    private String filename;

    /**
     * The timestamp that the artifact file was last modified.
     */
    private long lastModified;

    /**
     * The size of the artifact file in bytes.
     */
    private long size;

    public void setClasses( String classes )
    {
        this.classes = classes;
    }

    public void setMd5Checksum( String md5Checksum )
    {
        this.md5Checksum = md5Checksum;
    }

    public void setFilename( String filename )
    {
        this.filename = filename;
    }

    public void setLastModified( long lastModified )
    {
        this.lastModified = lastModified;
    }

    public void setSize( long size )
    {
        this.size = size;
    }

    public String getClasses()
    {
        return classes;
    }

    public String getMd5Checksum()
    {
        return md5Checksum;
    }

    public String getFilename()
    {
        return filename;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public long getSize()
    {
        return size;
    }

    /**
     * @noinspection RedundantIfStatement
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        MinimalArtifactIndexRecord that = (MinimalArtifactIndexRecord) obj;

        if ( lastModified != that.lastModified )
        {
            return false;
        }
        if ( size != that.size )
        {
            return false;
        }
        if ( classes != null ? !classes.equals( that.classes ) : that.classes != null )
        {
            return false;
        }
        if ( !filename.equals( that.filename ) )
        {
            return false;
        }
        if ( md5Checksum != null ? !md5Checksum.equals( that.md5Checksum ) : that.md5Checksum != null )
        {
            return false;
        }

        return true;
    }

    /**
     * @noinspection UnnecessaryParentheses
     */
    public int hashCode()
    {
        int result = classes != null ? classes.hashCode() : 0;
        result = 31 * result + ( md5Checksum != null ? md5Checksum.hashCode() : 0 );
        result = 31 * result + filename.hashCode();
        result = 31 * result + (int) ( lastModified ^ ( lastModified >>> 32 ) );
        result = 31 * result + (int) ( size ^ ( size >>> 32 ) );
        return result;
    }

    public String toString()
    {
        return "Filename: " + filename + "; checksum: " + md5Checksum + "; size: " + size + "; lastModified: " +
            new Date( lastModified ) + "; classes: " + classes;
    }

    public String getPrimaryKey()
    {
        return filename;
    }
}
