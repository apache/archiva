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

import java.util.HashMap;
import java.util.Map;

/**
 * ManagedArtifact 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedArtifact
{
    private String repositoryId;

    private Artifact artifact;

    private String path;

    protected Map attached;

    public ManagedArtifact( String repoId, Artifact artifact, String path )
    {
        super();
        this.repositoryId = repoId;
        this.artifact = artifact;
        this.path = path;
        this.attached = new HashMap();
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public String getPath()
    {
        return path;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public Map getAttached()
    {
        return attached;
    }

    public void setAttached( Map attached )
    {
        this.attached = attached;
    }
}
