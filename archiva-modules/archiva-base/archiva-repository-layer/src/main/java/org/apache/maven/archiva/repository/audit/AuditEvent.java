package org.apache.maven.archiva.repository.audit;

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
 * AuditEvent 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AuditEvent
{
    public static final String CREATE_DIR = "Created Directory";

    public static final String CREATE_FILE = "Created File";

    public static final String REMOVE_DIR = "Removed Directory";

    public static final String REMOVE_FILE = "Removed File";

    public static final String MODIFY_FILE = "Modify File";
    
    private String repositoryId;

    private String userId;
    
    private String remoteIP;

    private String resource;

    private String action;

    public AuditEvent()
    {
        /* do nothing */
    }

    public AuditEvent( String repoId, String user, String resource, String action )
    {
        this.repositoryId = repoId;
        this.userId = user;
        this.resource = resource;
        this.action = action;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    public String getResource()
    {
        return resource;
    }

    public void setResource( String resource )
    {
        this.resource = resource;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction( String action )
    {
        this.action = action;
    }

    public String getRemoteIP()
    {
        return remoteIP;
    }

    public void setRemoteIP( String remoteIP )
    {
        this.remoteIP = remoteIP;
    }
}
