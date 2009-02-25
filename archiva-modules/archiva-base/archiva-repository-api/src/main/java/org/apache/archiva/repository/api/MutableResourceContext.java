package org.apache.archiva.repository.api;

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
 * Wraps a ResourceContext instance and allows its members to be overloaded
 * @author jdumay
 */
public class MutableResourceContext implements ResourceContext
{
    private final ResourceContext context;

    private String logicalPath;

    private String repositoryId;

    private String principal;

    public MutableResourceContext(ResourceContext context)
    {
        this.context = context;
    }

    public String getLogicalPath()
    {
        if (logicalPath != null)
        {
            return logicalPath;
        }
        return context.getLogicalPath();
    }

    public String getRepositoryId()
    {
        if (repositoryId != null)
        {
            return repositoryId;
        }
        return context.getRepositoryId();
    }

    public String getPrincipal()
    {
        if (principal != null)
        {
            return principal;
        }
        return context.getPrincipal();
    }

    /**
     * Sets the principal overriding the internal principal value
     * @param principal
     */
    public void setPrincipal(String principal)
    {
        this.principal = principal;
    }

    /**
     * Sets the logicalPath overriding the internal logical path value
     * @param logicalPath
     */
    public void setLogicalPath(String logicalPath)
    {
        this.logicalPath = logicalPath;
    }

    /**
     * Sets the repositoryId overriding the internal logical path value
     * @param repositoryId
     */
    public void setRepositoryId(String repositoryId)
    {
        this.repositoryId = repositoryId;
    }
}
