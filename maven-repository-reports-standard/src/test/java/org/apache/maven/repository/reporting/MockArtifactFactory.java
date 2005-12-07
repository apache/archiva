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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * 
 */
public class MockArtifactFactory
    implements ArtifactFactory
{
    public Artifact createArtifact( String s, String s1, String s2, String s3, String s4 )
    {
        return null;
    }

    public Artifact createArtifactWithClassifier( String s, String s1, String s2, String s3, String s4 )
    {
        return null;
    }

    public Artifact createDependencyArtifact( String s, String s1, VersionRange versionRange, String s2, String s3,
                                              String s4 )
    {
        return null;
    }

    public Artifact createDependencyArtifact( String s, String s1, VersionRange versionRange, String s2, String s3,
                                              String s4, String s5 )
    {
        return null;
    }

    public Artifact createDependencyArtifact( String s, String s1, VersionRange versionRange, String s2, String s3,
                                              String s4, String s5, boolean b )
    {
        return null;
    }

    public Artifact createBuildArtifact( String s, String s1, String s2, String s3 )
    {
        return null;
    }

    public Artifact createProjectArtifact( String s, String s1, String s2 )
    {
        return null;
    }

    public Artifact createParentArtifact( String s, String s1, String s2 )
    {
        return null;
    }

    public Artifact createPluginArtifact( String s, String s1, VersionRange versionRange )
    {
        return null;
    }

    public Artifact createProjectArtifact( String s, String s1, String s2, String s3 )
    {
        return null;
    }

    public Artifact createExtensionArtifact( String s, String s1, VersionRange versionRange )
    {
        return null;
    }
}
