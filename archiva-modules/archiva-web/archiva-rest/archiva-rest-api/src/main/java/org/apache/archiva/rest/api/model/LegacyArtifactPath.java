package org.apache.archiva.rest.api.model;
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
 * @since 1.4
 */
@XmlRootElement( name = "legacyArtifactPath" )
public class LegacyArtifactPath
    implements Serializable
{
    /**
     * The legacy path.
     */
    private String path;

    /**
     * The artifact reference, as " [groupId] :
     * [artifactId] : [version] : [classifier] : [type] ".
     */
    private String artifact;

    public LegacyArtifactPath()
    {
        // no op
    }

    public LegacyArtifactPath( String path, String artifact )
    {
        this.path = path;
        this.artifact = artifact;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public String getArtifact()
    {
        return artifact;
    }

    public void setArtifact( String artifact )
    {
        this.artifact = artifact;
    }

    public boolean match( String path )
    {
        return path.equals( this.path );
    }

    public String getGroupId()
    {
        return artifact.split( ":" )[0];
    }

    public String getArtifactId()
    {
        return artifact.split( ":" )[1];
    }

    public String getVersion()
    {
        return artifact.split( ":" )[2];
    }

    public String getClassifier()
    {
        String classifier = artifact.split( ":" )[3];
        return classifier.length() > 0 ? classifier : null;
    }

    public String getType()
    {
        return artifact.split( ":" )[4];
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "LegacyArtifactPath" );
        sb.append( "{path='" ).append( path ).append( '\'' );
        sb.append( ", artifact='" ).append( artifact ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
