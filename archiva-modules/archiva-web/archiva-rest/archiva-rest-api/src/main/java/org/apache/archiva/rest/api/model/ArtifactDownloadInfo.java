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

import org.apache.archiva.metadata.model.ArtifactMetadata;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "artifactDownloadInfo" )
public class ArtifactDownloadInfo
    implements Serializable
{
    private String type;

    private String namespace;

    private String project;

    private String size;

    private String id;

    private String repositoryId;

    private String version;

    private String path;

    private String classifier;

    public ArtifactDownloadInfo( ArtifactMetadata artifact, String path, String type, String classifier )
    {
        this.repositoryId = artifact.getRepositoryId();
        this.path = path.substring( 0, path.lastIndexOf( "/" ) + 1 ) + artifact.getId();

        this.type = type;
        this.classifier = classifier;

        this.namespace = artifact.getNamespace();
        this.project = artifact.getProject();

        // TODO: find a reusable formatter for this
        double s = artifact.getSize();
        String symbol = "b";
        if ( s > 1024 )
        {
            symbol = "K";
            s /= 1024;

            if ( s > 1024 )
            {
                symbol = "M";
                s /= 1024;

                if ( s > 1024 )
                {
                    symbol = "G";
                    s /= 1024;
                }
            }
        }

        DecimalFormat df = new DecimalFormat( "#,###.##", new DecimalFormatSymbols( Locale.US ) );
        this.size = df.format( s ) + " " + symbol;
        this.id = artifact.getId();
        this.version = artifact.getVersion();
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
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

    public String getSize()
    {
        return size;
    }

    public void setSize( String size )
    {
        this.size = size;
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

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ArtifactDownloadInfo" );
        sb.append( "{type='" ).append( type ).append( '\'' );
        sb.append( ", namespace='" ).append( namespace ).append( '\'' );
        sb.append( ", project='" ).append( project ).append( '\'' );
        sb.append( ", size='" ).append( size ).append( '\'' );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( ", repositoryId='" ).append( repositoryId ).append( '\'' );
        sb.append( ", version='" ).append( version ).append( '\'' );
        sb.append( ", path='" ).append( path ).append( '\'' );
        sb.append( ", classifier='" ).append( classifier ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
