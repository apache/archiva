package org.apache.archiva.rest.api.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement( name = "dependency" )
public class Dependency
    implements Serializable
{
    private String groupId;

    private String artifactId;

    private String version;

    private String classifier;

    private String type;

    private String scope;

    @Override
    public String toString()
    {
        return "Dependency{" + "groupId='" + groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", version='"
            + version + '\'' + ", classifier='" + classifier + '\'' + ", type='" + type + '\'' + ", scope='" + scope
            + '\'' + '}';
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

        Dependency that = (Dependency) o;

        if ( !artifactId.equals( that.artifactId ) )
        {
            return false;
        }
        if ( classifier != null ? !classifier.equals( that.classifier ) : that.classifier != null )
        {
            return false;
        }
        if ( !groupId.equals( that.groupId ) )
        {
            return false;
        }
        if ( scope != null ? !scope.equals( that.scope ) : that.scope != null )
        {
            return false;
        }
        if ( type != null ? !type.equals( that.type ) : that.type != null )
        {
            return false;
        }
        if ( !version.equals( that.version ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + ( classifier != null ? classifier.hashCode() : 0 );
        result = 31 * result + ( type != null ? type.hashCode() : 0 );
        result = 31 * result + ( scope != null ? scope.hashCode() : 0 );
        return result;
    }

    public Dependency( String groupId, String artifactId, String version, String classifier, String type, String scope )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
        this.scope = scope;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }
}
