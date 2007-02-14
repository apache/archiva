package org.apache.maven.archiva.artifact;

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

/**
 * ManagedJavaArtifact - a ManagedArtifact with optional javadoc and source 
 * reference jars.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedJavaArtifact
    extends ManagedArtifact
{
    public static final String JAVADOC = "javadoc";

    public static final String SOURCES = "sources";

    public ManagedJavaArtifact( String repoId, Artifact artifact, String path )
    {
        super( repoId, artifact, path );
    }

    public String getJavadocPath()
    {
        return (String) super.attached.get( JAVADOC );
    }

    public void setJavadocPath( String javadocPath )
    {
        super.attached.put( JAVADOC, javadocPath );
    }

    public String getSourcesPath()
    {
        return (String) super.attached.get( SOURCES );
    }

    public void setSourcesPath( String sourcesPath )
    {
        super.attached.put( SOURCES, sourcesPath );
    }
}
