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

import org.apache.maven.archiva.common.utils.VersionUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * ArchivaArtifact - Mutable artifact object.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaArtifact
{
    private ArchivaArtifactModel model;
    
    private ArchivaArtifactPlatformDetails platformDetails;

    private String baseVersion;

    private boolean snapshot = false;

    public ArchivaArtifact( String groupId, String artifactId, String version, String classifier, String type )
    {
        this( null, groupId, artifactId, version, classifier, type );
    }

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

        if ( repository == null )
        {
            model.setContentKey( new RepositoryContent( groupId, artifactId, version ) );
        }
        else
        {
            model.setContentKey( new RepositoryContent( repository.getModel(), groupId, artifactId, version ) );
        }
        model.setClassifier( StringUtils.defaultString( classifier ) );
        model.setType( type );

        this.snapshot = VersionUtil.isSnapshot( version );
        this.baseVersion = VersionUtil.getBaseVersion( version );
    }

    public ArchivaArtifactModel getModel()
    {
        return model;
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

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        if ( model != null )
        {
            RepositoryContent key = model.getContentKey();
            if ( key != null )
            {
                result = PRIME * result + ( ( key.getGroupId() == null ) ? 0 : key.getGroupId().hashCode() );
                result = PRIME * result + ( ( key.getArtifactId() == null ) ? 0 : key.getArtifactId().hashCode() );
                result = PRIME * result + ( ( key.getVersion() == null ) ? 0 : key.getVersion().hashCode() );
                result = PRIME * result + ( ( model.getClassifier() == null ) ? 0 : model.getClassifier().hashCode() );
                result = PRIME * result + ( ( model.getType() == null ) ? 0 : model.getType().hashCode() );
            }
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
        else
        {
            RepositoryContent key = model.getContentKey();
            RepositoryContent otherkey = other.model.getContentKey();

            if ( key == null )
            {
                if ( otherkey != null )
                {
                    return false;
                }
            }
            else
            {
                if ( !equals( key.getGroupId(), otherkey.getGroupId() )
                                || !equals( key.getArtifactId(), otherkey.getArtifactId() )
                                || !equals( key.getVersion(), otherkey.getVersion() )
                                || !equals( model.getClassifier(), other.model.getClassifier() )
                                || !equals( model.getType(), other.model.getType() ) )
                {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean equals( String left, String right )
    {
        if ( left == null )
        {
            if ( right != null )
            {
                return false;
            }
        }
        else if ( !left.equals( right ) )
        {
            return false;
        }
        return true;
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
