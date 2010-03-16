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
 * Information about the issue management system used by the project.
 *
 * @todo considering moving this to a facet - avoid referring to it externally
 */
public class IssueManagement
{
    /**
     * A simple identifier for the type of issue management server used, eg <tt>jira</tt>, <tt>bugzilla</tt>, etc.
     */
    private String system;

    /**
     * The base URL of the issue management system.
     */
    private String url;

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getSystem()
    {
        return system;
    }

    public void setSystem( String system )
    {
        this.system = system;
    }
}
