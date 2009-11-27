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

public class ProjectVersionReference
{
    private ReferenceType referenceType;

    private String projectId;

    private String namespace;

    private String projectVersion;

    public void setReferenceType( ReferenceType referenceType )
    {
        this.referenceType = referenceType;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    public void setProjectId( String projectId )
    {
        this.projectId = projectId;
    }

    public ReferenceType getReferenceType()
    {
        return referenceType;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public String getProjectVersion()
    {
        return projectVersion;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setProjectVersion( String projectVersion )
    {
        this.projectVersion = projectVersion;
    }

    public enum ReferenceType
    {
        DEPENDENCY,
        PARENT
    }
}
