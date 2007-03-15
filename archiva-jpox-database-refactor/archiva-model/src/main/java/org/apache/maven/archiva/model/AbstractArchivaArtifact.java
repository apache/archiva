package org.apache.maven.archiva.model;

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

import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.StringUtils;

import java.util.Map;

/**
 * ArchivaArtifact 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractArchivaArtifact
{
    private String classifier;

    private RepositoryContent key;

    private String type;

    public AbstractArchivaArtifact( ArchivaRepository repository, String groupId, String artifactId, String version, String classifier, String type )
    {
        this.key = new RepositoryContent( repository, groupId, artifactId, version );

        this.classifier = classifier;

        this.type = type;

        validateIdentity();
    }

    public String getClassifier()
    {
        return classifier;
    }

    public RepositoryContent getRepositoryContent()
    {
        return key;
    }

    public String getType()
    {
        return type;
    }

    public boolean hasClassifier()
    {
        return StringUtils.isNotEmpty( classifier );
    }

    public void setRepositoryContent( RepositoryContent key )
    {
        this.key = key;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if ( key.getGroupId() != null )
        {
            sb.append( key.getGroupId() );
            sb.append( ":" );
        }
        appendArtifactTypeClassifierString( sb );
        sb.append( ":" );
        if ( key.getVersion() != null )
        {
            sb.append( key.getVersion() );
        }

        return sb.toString();
    }

    private void appendArtifactTypeClassifierString( StringBuffer sb )
    {
        sb.append( key.getArtifactId() );
        sb.append( ":" );
        sb.append( getType() );
        if ( hasClassifier() )
        {
            sb.append( ":" );
            sb.append( getClassifier() );
        }
    }

    protected boolean empty( String value )
    {
        return value == null || value.trim().length() < 1;
    }

    protected void validateIdentity()
    {
        if ( empty( key.getGroupId() ) )
        {
            throw new InvalidArtifactRTException( key.getGroupId(), key.getArtifactId(), key.getVersion(), type,
                                                  "The groupId cannot be empty." );
        }

        if ( key.getArtifactId() == null )
        {
            throw new InvalidArtifactRTException( key.getGroupId(), key.getArtifactId(), key.getVersion(), type,
                                                  "The artifactId cannot be empty." );
        }

        if ( type == null )
        {
            throw new InvalidArtifactRTException( key.getGroupId(), key.getArtifactId(), key.getVersion(), type,
                                                  "The type cannot be empty." );
        }

        if ( key.getVersion() == null )
        {
            throw new InvalidArtifactRTException( key.getGroupId(), key.getArtifactId(), key.getVersion(), type,
                                                  "The version cannot be empty." );
        }
    }
}
