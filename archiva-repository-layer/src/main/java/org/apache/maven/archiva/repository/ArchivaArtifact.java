package org.apache.maven.archiva.repository;

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

import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.RepositoryContent;
import org.codehaus.plexus.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ArchivaArtifact - Mutable artifact object.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaArtifact
{
    private static final String SNAPSHOT_VERSION = "SNAPSHOT";

    private static final Pattern VERSION_FILE_PATTERN = Pattern.compile( "^(.*)-([0-9]{8}\\.[0-9]{6})-([0-9]+)$" );

    private ArchivaArtifactModel model;
    
    private String baseVersion;
    
    private boolean snapshot = false;
    
    public ArchivaArtifact( ArchivaRepository repository, String groupId, String artifactId, String version,
                            String classifier, String type )
    {
        if ( empty( groupId ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty groupId." );
        }

        if ( empty( artifactId ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty artifactId." );
        }

        if ( empty( version ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty version." );
        }

        if ( empty( type ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty type." );
        }

        model = new ArchivaArtifactModel();

        model.setContentKey( new RepositoryContent( repository.getModel(), groupId, artifactId, version ) );
        model.setClassifier( StringUtils.defaultString( classifier ) );
        model.setType( type );
        
        // Determine Snapshot Base Version.
        Matcher m = VERSION_FILE_PATTERN.matcher( version );
        if ( m.matches() )
        {
            this.baseVersion =  m.group( 1 ) + "-" + SNAPSHOT_VERSION ;
            snapshot = true;
        }
        else
        {
            this.baseVersion = version;
            snapshot = version.endsWith( SNAPSHOT_VERSION );
        }
    }

    public String getGroupId()
    {
        return model.getContentKey().getGroupId();
    }

    public String getArtifactId()
    {
        return model.getContentKey().getArtifactId();
    }

    public String getVersion()
    {
        return model.getContentKey().getVersion();
    }

    public String getBaseVersion()
    {
        return baseVersion;
    }
    
    public boolean isSnapshot()
    {
        return snapshot;
    }
    
    public String getClassifier()
    {
        return model.getClassifier();
    }

    public String getType()
    {
        return model.getType();
    }

    public boolean hasClassifier()
    {
        return StringUtils.isNotEmpty( model.getClassifier() );
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if ( model.getContentKey().getGroupId() != null )
        {
            sb.append( model.getContentKey().getGroupId() );
            sb.append( ":" );
        }
        appendArtifactTypeClassifierString( sb );
        sb.append( ":" );
        if ( model.getContentKey().getVersion() != null )
        {
            sb.append( model.getContentKey().getVersion() );
        }

        return sb.toString();
    }

    private void appendArtifactTypeClassifierString( StringBuffer sb )
    {
        sb.append( model.getContentKey().getArtifactId() );
        sb.append( ":" );
        sb.append( getType() );
        if ( hasClassifier() )
        {
            sb.append( ":" );
            sb.append( getClassifier() );
        }
    }

    private boolean empty( String value )
    {
        return value == null || value.trim().length() < 1;
    }

}
