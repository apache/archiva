package org.apache.maven.archiva.discoverer.consumers;

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

import org.apache.maven.archiva.discoverer.PathUtil;
import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * MockArtifactConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.discoverer.DiscovererConsumers"
 *     role-hint="mock-artifact"
 *     instantiation-strategy="per-lookup"
 */
public class MockArtifactConsumer
    extends GenericArtifactConsumer
{
    private Map artifactMap = new HashMap();

    private Map failureMap = new HashMap();

    public void processArtifact( Artifact artifact, File file )
    {
        String relpath = PathUtil.getRelative( repository.getBasedir(), file );
        artifactMap.put( relpath, artifact );
    }

    public void processArtifactBuildFailure( File path, String message )
    {
        String relpath = PathUtil.getRelative( repository.getBasedir(), path );
        failureMap.put( relpath, message );
    }

    public Map getArtifactMap()
    {
        return artifactMap;
    }

    public Map getFailureMap()
    {
        return failureMap;
    }
}