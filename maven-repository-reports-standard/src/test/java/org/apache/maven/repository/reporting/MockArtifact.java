package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.metadata.ArtifactMetadata;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class MockArtifact
    implements Artifact
{

    public String getGroupId()
    {
        return null;
    }

    public String getArtifactId()
    {
        return null;
    }

    public String getVersion()
    {
        return null;
    }

    public void setVersion( String s )
    {
    }

    public String getScope()
    {
        return null;
    }

    public String getType()
    {
        return null;
    }

    public String getClassifier()
    {
        return null;
    }

    public boolean hasClassifier()
    {
        return false;
    }

    public File getFile()
    {
        return null;
    }

    public void setFile( File file )
    {
    }

    public String getBaseVersion()
    {
        return null;
    }

    public void setBaseVersion( String s )
    {
    }

    public String getId()
    {
        return null;
    }

    public String getDependencyConflictId()
    {
        return null;
    }

    public void addMetadata( ArtifactMetadata artifactMetadata )
    {
    }

    public Collection getMetadataList()
    {
        return null;
    }

    public void setRepository( ArtifactRepository artifactRepository )
    {
    }

    public ArtifactRepository getRepository()
    {
        return null;
    }

    public void updateVersion( String s, ArtifactRepository artifactRepository )
    {
    }

    public String getDownloadUrl()
    {
        return null;
    }

    public void setDownloadUrl( String s )
    {
    }

    public ArtifactFilter getDependencyFilter()
    {
        return null;
    }

    public void setDependencyFilter( ArtifactFilter artifactFilter )
    {
    }

    public ArtifactHandler getArtifactHandler()
    {
        return null;
    }

    public List getDependencyTrail()
    {
        return null;
    }

    public void setDependencyTrail( List list )
    {
    }

    public void setScope( String s )
    {
    }

    public VersionRange getVersionRange()
    {
        return null;
    }

    public void setVersionRange( VersionRange versionRange )
    {
    }

    public void selectVersion( String s )
    {
    }

    public void setGroupId( String s )
    {
    }

    public void setArtifactId( String s )
    {
    }

    public boolean isSnapshot()
    {
        return false;
    }

    public void setResolved( boolean b )
    {
    }

    public boolean isResolved()
    {
        return false;
    }

    public void setResolvedVersion( String s )
    {
    }

    public void setArtifactHandler( ArtifactHandler artifactHandler )
    {
    }

    public boolean isRelease()
    {
        return false;
    }

    public void setRelease( boolean b )
    {
    }

    public List getAvailableVersions()
    {
        return null;
    }

    public void setAvailableVersions( List list )
    {
    }

    public boolean isOptional()
    {
        return false;
    }

    public ArtifactVersion getSelectedVersion()
        throws OverConstrainedVersionException
    {
        return null;
    }

    public boolean isSelectedVersionKnown()
        throws OverConstrainedVersionException
    {
        return false;
    }

    public int compareTo( Object o )
    {
        return 0;
    }
}
