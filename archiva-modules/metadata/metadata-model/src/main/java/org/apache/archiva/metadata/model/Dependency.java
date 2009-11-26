package org.apache.archiva.metadata.model;

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

/**
 * TODO: review what is appropriate for the base here - rest should be in a maven dependency facet
 */
public class Dependency
{
    private String classifier;

    private boolean optional;

    private String scope;

    private String systemPath;

    private String type;

    private String artifactId;

    private String groupId;

    private String version;

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setOptional( boolean optional )
    {
        this.optional = optional;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String getScope()
    {
        return scope;
    }

    public void setSystemPath( String systemPath )
    {
        this.systemPath = systemPath;
    }

    public String getSystemPath()
    {
        return systemPath;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getGroupId()
    {
        return groupId;
    }
}
