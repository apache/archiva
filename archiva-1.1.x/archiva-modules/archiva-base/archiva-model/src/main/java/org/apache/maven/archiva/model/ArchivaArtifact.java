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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;

/**
 * ArchivaArtifact - Mutable artifact object.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaArtifact
{
    private ArchivaArtifactModel model;

    private ArchivaArtifactPlatformDetails platformDetails;

    private String baseVersion;

    public ArchivaArtifact( String groupId, String artifactId, String version,
                            String classifier, String type )
    {
        if ( empty( groupId ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty groupId ["
                + Keys.toKey( groupId, artifactId, version, classifier, type ) + "]" );
        }

        if ( empty( artifactId ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty artifactId ["
                + Keys.toKey( groupId, artifactId, version, classifier, type ) + "]" );
        }

        if ( empty( version ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty version ["
                + Keys.toKey( groupId, artifactId, version, classifier, type ) + "]" );
        }

        if ( empty( type ) )
        {
            throw new IllegalArgumentException( "Unable to create ArchivaArtifact with empty type ["
                + Keys.toKey( groupId, artifactId, version, classifier, type ) + "]" );
        }

        model = new ArchivaArtifactModel();

        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setClassifier( StringUtils.defaultString( classifier ) );
        model.setType( type );
        model.setSnapshot( VersionUtil.isSnapshot( version ) );
        
        this.baseVersion = VersionUtil.getBaseVersion( version );
    }

    public ArchivaArtifact( ArchivaArtifactModel artifactModel )
    {
        this.model = artifactModel;
        model.setSnapshot( VersionUtil.isSnapshot( model.getVersion() ) );
        this.baseVersion = VersionUtil.getBaseVersion( model.getVersion() );
    }
    
    public ArchivaArtifact( ArtifactReference ref )
    {
        this( ref.getGroupId(), ref.getArtifactId(), ref.getVersion(), ref.getClassifier(), ref.getType() );
    }

    public ArchivaArtifactModel getModel()
    {
        return model;
    }

    public String getGroupId()
    {
        return model.getGroupId();
    }

    public String getArtifactId()
    {
        return model.getArtifactId();
    }

    public String getVersion()
    {
        return model.getVersion();
    }

    public String getBaseVersion()
    {
        return baseVersion;
    }

    public boolean isSnapshot()
    {
        return model.isSnapshot();
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

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        if ( model != null )
        {
            result = PRIME * result + ( ( model.getGroupId() == null ) ? 0 : model.getGroupId().hashCode() );
            result = PRIME * result + ( ( model.getArtifactId() == null ) ? 0 : model.getArtifactId().hashCode() );
            result = PRIME * result + ( ( model.getVersion() == null ) ? 0 : model.getVersion().hashCode() );
            result = PRIME * result + ( ( model.getClassifier() == null ) ? 0 : model.getClassifier().hashCode() );
            result = PRIME * result + ( ( model.getType() == null ) ? 0 : model.getType().hashCode() );
        }
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

        final ArchivaArtifact other = (ArchivaArtifact) obj;

        if ( model == null )
        {
            if ( other.model != null )
            {
                return false;
            }
        }
        if ( !model.equals( other.model ) )
        {
            return false;
        }

        return true;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if ( model.getGroupId() != null )
        {
            sb.append( model.getGroupId() );
            sb.append( ":" );
        }
        appendArtifactTypeClassifierString( sb );
        sb.append( ":" );
        if ( model.getVersion() != null )
        {
            sb.append( model.getVersion() );
        }

        return sb.toString();
    }

    private void appendArtifactTypeClassifierString( StringBuffer sb )
    {
        sb.append( model.getArtifactId() );
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
        return ( value == null ) || ( value.trim().length() < 1 );
    }

    public ArchivaArtifactPlatformDetails getPlatformDetails()
    {
        return platformDetails;
    }

    public void setPlatformDetails( ArchivaArtifactPlatformDetails platformDetails )
    {
        this.platformDetails = platformDetails;
    }
}
