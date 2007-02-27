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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * ArchivaArtifact 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.model.ArchivaArtifact"
 */
public class ArchivaArtifact
    implements Artifact
{
    private ArtifactHandler artifactHandler;

    private String artifactId;

    private Map attached;

    private List availableVersions;

    /**
     * The resolved version for the artifact after conflict resolution, that has not been transformed.
     *
     * @todo should be final
     */
    private String baseVersion;

    private String classifier;

    private ArtifactFilter dependencyFilter;

    private List dependencyTrail;

    private String downloadUrl;

    private File file;

    private String groupId;

    private Map metadataMap;

    private boolean optional;

    private boolean release;

    private ArtifactRepository repository;

    private boolean resolved;

    private String scope;

    private String type;

    private String version;

    private VersionRange versionRange;

    public ArchivaArtifact( String groupId, String artifactId, VersionRange versionRange, String scope, String type,
                            String classifier, ArtifactHandler artifactHandler )
    {
        this( groupId, artifactId, versionRange, scope, type, classifier, artifactHandler, false );
    }

    public ArchivaArtifact( String groupId, String artifactId, VersionRange versionRange, String scope, String type,
                            String classifier, ArtifactHandler artifactHandler, boolean optional )
    {
        this.groupId = groupId;

        this.artifactId = artifactId;

        this.versionRange = versionRange;

        selectVersionFromNewRangeIfAvailable();

        this.artifactHandler = artifactHandler;

        this.scope = scope;

        this.type = type;

        if ( classifier == null )
        {
            classifier = artifactHandler.getClassifier();
        }

        this.classifier = classifier;

        this.optional = optional;

        validateIdentity();
    }

    public void addAttached( ArchivaArtifact attachedArtifact )
    {
        attached.put( attachedArtifact.getClassifier(), attachedArtifact );
        // Naughty, Attached shouldn't have it's own attached artifacts!
        attachedArtifact.clearAttached();
    }

    public void addMetadata( ArtifactMetadata metadata )
    {
        if ( metadataMap == null )
        {
            metadataMap = new HashMap();
        }

        ArtifactMetadata m = (ArtifactMetadata) metadataMap.get( metadata.getKey() );
        if ( m != null )
        {
            m.merge( metadata );
        }
        else
        {
            metadataMap.put( metadata.getKey(), metadata );
        }
    }

    public void clearAttached()
    {
        attached.clear();
    }

    public int compareTo( Object o )
    {
        Artifact a = (Artifact) o;

        int result = groupId.compareTo( a.getGroupId() );
        if ( result == 0 )
        {
            result = artifactId.compareTo( a.getArtifactId() );
            if ( result == 0 )
            {
                result = type.compareTo( a.getType() );
                if ( result == 0 )
                {
                    if ( classifier == null )
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = 1;
                        }
                    }
                    else
                    {
                        if ( a.getClassifier() != null )
                        {
                            result = classifier.compareTo( a.getClassifier() );
                        }
                        else
                        {
                            result = -1;
                        }
                    }
                    if ( result == 0 )
                    {
                        // We don't consider the version range in the comparison, just the resolved version
                        result = version.compareTo( a.getVersion() );
                    }
                }
            }
        }
        return result;
    }

    public boolean equals( Object o )
    {
        if ( o == this )
        {
            return true;
        }

        if ( !( o instanceof Artifact ) )
        {
            return false;
        }

        Artifact a = (Artifact) o;

        if ( !a.getGroupId().equals( groupId ) )
        {
            return false;
        }
        else if ( !a.getArtifactId().equals( artifactId ) )
        {
            return false;
        }
        else if ( !a.getVersion().equals( version ) )
        {
            return false;
        }
        else if ( !a.getType().equals( type ) )
        {
            return false;
        }
        else if ( a.getClassifier() == null ? classifier != null : !a.getClassifier().equals( classifier ) )
        {
            return false;
        }

        // We don't consider the version range in the comparison, just the resolved version

        return true;
    }

    public ArtifactHandler getArtifactHandler()
    {
        return artifactHandler;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public Map getAttached()
    {
        return attached;
    }

    public List getAvailableVersions()
    {
        return availableVersions;
    }

    public String getBaseVersion()
    {
        if ( baseVersion == null )
        {
            baseVersion = version;

            if ( version == null )
            {
                throw new NullPointerException( "version was null for " + groupId + ":" + artifactId );
            }
        }
        return baseVersion;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getDependencyConflictId()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( getGroupId() );
        sb.append( ":" );
        appendArtifactTypeClassifierString( sb );
        return sb.toString();
    }

    public ArtifactFilter getDependencyFilter()
    {
        return dependencyFilter;
    }

    public List getDependencyTrail()
    {
        return dependencyTrail;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public String getDownloadUrl()
    {
        return downloadUrl;
    }

    public File getFile()
    {
        return file;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getId()
    {
        return getDependencyConflictId() + ":" + getBaseVersion();
    }

    public Collection getMetadataList()
    {
        return metadataMap == null ? Collections.EMPTY_LIST : metadataMap.values();
    }

    // ----------------------------------------------------------------------
    // Object overrides
    // ----------------------------------------------------------------------

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    public String getScope()
    {
        return scope;
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return versionRange.getSelectedVersion( this );
    }

    public String getType()
    {
        return type;
    }

    public String getVersion()
    {
        return version;
    }

    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    public boolean hasClassifier()
    {
        return StringUtils.isNotEmpty( classifier );
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + groupId.hashCode();
        result = 37 * result + artifactId.hashCode();
        result = 37 * result + type.hashCode();
        if ( version != null )
        {
            result = 37 * result + version.hashCode();
        }
        result = 37 * result + ( classifier != null ? classifier.hashCode() : 0 );
        return result;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public boolean isRelease()
    {
        return release;
    }

    public boolean isResolved()
    {
        return resolved;
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return versionRange.isSelectedVersionKnown( this );
    }

    public boolean isSnapshot()
    {
        if ( version != null || baseVersion != null )
        {
            Matcher m = VERSION_FILE_PATTERN.matcher( getBaseVersion() );
            if ( m.matches() )
            {
                setBaseVersion( m.group( 1 ) + "-" + SNAPSHOT_VERSION );
                return true;
            }
            else
            {
                return getBaseVersion().endsWith( SNAPSHOT_VERSION ) || getBaseVersion().equals( LATEST_VERSION );
            }
        }
        else
        {
            return false;
        }
    }

    public void selectVersion( String version )
    {
        this.version = version;
        this.baseVersion = version;
    }

    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
        this.artifactHandler = artifactHandler;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setAttached( Map attached )
    {
        this.attached = attached;
    }

    public void setAvailableVersions( List availableVersions )
    {
        this.availableVersions = availableVersions;
    }

    public void setBaseVersion( String baseVersion )
    {
        this.baseVersion = baseVersion;
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
        this.dependencyFilter = artifactFilter;
    }

    public void setDependencyTrail( List dependencyTrail )
    {
        this.dependencyTrail = dependencyTrail;
    }

    public void setDownloadUrl( String downloadUrl )
    {
        this.downloadUrl = downloadUrl;
    }

    public void setFile( File file )
    {
        this.file = file;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }

    public void setRelease( boolean release )
    {
        this.release = release;
    }

    public void setRepository( ArtifactRepository repository )
    {
        this.repository = repository;
    }

    public void setResolved( boolean resolved )
    {
        this.resolved = resolved;
    }

    public void setResolvedVersion( String version )
    {
        this.version = version;
        // retain baseVersion
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public void setVersion( String version )
    {
        this.version = version;
        this.baseVersion = version;
        this.versionRange = null;
    }

    public void setVersionRange( VersionRange versionRange )
    {
        this.versionRange = versionRange;

        selectVersionFromNewRangeIfAvailable();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        if ( getGroupId() != null )
        {
            sb.append( getGroupId() );
            sb.append( ":" );
        }
        appendArtifactTypeClassifierString( sb );
        sb.append( ":" );
        if ( version != null || baseVersion != null )
        {
            sb.append( getBaseVersion() );
        }
        else
        {
            sb.append( versionRange.toString() );
        }
        if ( scope != null )
        {
            sb.append( ":" );
            sb.append( scope );
        }
        return sb.toString();
    }

    public void updateVersion( String version, ArtifactRepository localRepository )
    {
        setResolvedVersion( version );
        setFile( new File( localRepository.getBasedir(), localRepository.pathOf( this ) ) );
    }
    
    private void appendArtifactTypeClassifierString( StringBuffer sb )
    {
        sb.append( getArtifactId() );
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

    private void selectVersionFromNewRangeIfAvailable()
    {
        if ( versionRange != null && versionRange.getRecommendedVersion() != null )
        {
            selectVersion( versionRange.getRecommendedVersion().toString() );
        }
        else
        {
            this.version = null;
            this.baseVersion = null;
        }
    }

    private void validateIdentity()
    {
        if ( empty( groupId ) )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, getVersion(), type,
                                                  "The groupId cannot be empty." );
        }

        if ( artifactId == null )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, getVersion(), type,
                                                  "The artifactId cannot be empty." );
        }

        if ( type == null )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, getVersion(), type, "The type cannot be empty." );
        }

        if ( version == null && versionRange == null )
        {
            throw new InvalidArtifactRTException( groupId, artifactId, getVersion(), type,
                                                  "The version cannot be empty." );
        }
    }
}
