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
 * ManagedEjbArtifact - adds the ability to reference the ejb-client jar too. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedEjbArtifact
    extends ManagedJavaArtifact
{
    public static final String CLIENT = "client";

    public ManagedEjbArtifact( String repoId, Artifact artifact, String path )
    {
        super( repoId, artifact, path );
    }

    public String getClientPath()
    {
        return (String) super.attached.get( CLIENT );
    }

    public void setClientPath( String clientPath )
    {
        super.attached.put( CLIENT, clientPath );
    }
}
