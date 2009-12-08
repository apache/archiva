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
 * @version $Id$
 */
public class RepositoryProblemReport
    extends RepositoryProblem
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
        setRepositoryId( repositoryProblem.getRepositoryId() );
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

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        RepositoryProblemReport that = (RepositoryProblemReport) o;

        if ( artifactURL != null ? !artifactURL.equals( that.artifactURL ) : that.artifactURL != null )
        {
            return false;
        }
        if ( groupURL != null ? !groupURL.equals( that.groupURL ) : that.groupURL != null )
        {
            return false;
        }
        if ( versionURL != null ? !versionURL.equals( that.versionURL ) : that.versionURL != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = groupURL != null ? groupURL.hashCode() : 0;
        result = 31 * result + ( artifactURL != null ? artifactURL.hashCode() : 0 );
        result = 31 * result + ( versionURL != null ? versionURL.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "RepositoryProblemReport{" + "groupURL='" + groupURL + '\'' + ", artifactURL='" + artifactURL + '\'' +
            ", versionURL='" + versionURL + '\'' + '}';
    }
}
