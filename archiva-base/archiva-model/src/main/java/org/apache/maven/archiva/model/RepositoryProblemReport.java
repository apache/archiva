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

/**
 * RepositoryProblemReport
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryProblemReport extends RepositoryProblem
{
	private static final long serialVersionUID = 4990893576717148324L;

	protected String groupURL;

    protected String artifactURL;

    protected String versionURL;

    public RepositoryProblemReport( RepositoryProblem repositoryProblem )
    {
        setGroupId( repositoryProblem.getGroupId() );
        setArtifactId( repositoryProblem.getArtifactId() );
        setVersion( repositoryProblem.getVersion() );
        setMessage( repositoryProblem.getMessage() );
        setOrigin( repositoryProblem.getOrigin() );
        setPath( repositoryProblem.getPath() );
        setType( repositoryProblem.getType() );
    }

    public void setGroupURL( String groupURL )
    {
        this.groupURL = groupURL;
    }

    public String getGroupURL()
    {
        return groupURL; 
    }

    public void setArtifactURL( String artifactURL )
    {
        this.artifactURL = artifactURL;
    }

    public String getArtifactURL()
    {
        return artifactURL; 
    }

    public void setVersionURL( String versionURL )
    {
        this.versionURL = versionURL;
    }

    public String getVersionURL()
    {
        return versionURL; 
    }
}
