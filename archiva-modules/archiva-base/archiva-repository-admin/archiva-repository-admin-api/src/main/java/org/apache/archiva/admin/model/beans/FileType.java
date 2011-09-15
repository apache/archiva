package org.apache.archiva.admin.model.beans;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "fileType" )
public class FileType
    implements Serializable
{
    /**
     * Field id.
     */
    private String id;

    /**
     * Field patterns.
     */
    private List<String> patterns;

    public FileType()
    {
        // no op
    }

    public FileType( String id, List<String> patterns )
    {
        this.id = id;
        this.patterns = patterns;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public List<String> getPatterns()
    {
        if ( patterns == null )
        {
            this.patterns = new ArrayList<String>();
        }
        return patterns;
    }

    public void setPatterns( List<String> patterns )
    {
        this.patterns = patterns;
    }

    public void addPattern( String pattern )
    {
        getPatterns().add( pattern );
    }

    public void removePattern( String pattern )
    {
        getPatterns().remove( pattern );
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

        FileType fileType = (FileType) o;

        if ( id != null ? !id.equals( fileType.id ) : fileType.id != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return id != null ? 37 + id.hashCode() : 0;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "FileType" );
        sb.append( "{id='" ).append( id ).append( '\'' );
        sb.append( ", patterns=" ).append( patterns );
        sb.append( '}' );
        return sb.toString();
    }
}
