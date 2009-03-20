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
 * @version $Id$
 */
public class AuditEvent
{
    public static final String CREATE_DIR = "Created Directory";

    public static final String CREATE_FILE = "Created File";

    public static final String REMOVE_DIR = "Removed Directory";

    public static final String REMOVE_FILE = "Removed File";

    public static final String MODIFY_FILE = "Modified File";

    public static final String MOVE_FILE = "Moved File";

    public static final String MOVE_DIRECTORY = "Moved Directory";

    public static final String COPY_DIRECTORY = "Copied Directory";

    public static final String COPY_FILE = "Copied File";

    public static final String UPLOAD_FILE = "Uploaded File";

    public static final String ADD_LEGACY_PATH = "Added Legacy Artifact Path";

    public static final String REMOVE_LEGACY_PATH = "Removed Legacy Artifact Path";

    public static final String PURGE_ARTIFACT = "Purged Artifact";

    public static final String PURGE_FILE = "Purged Support File";

    public static final String REMOVE_SCANNED = "Removed in Filesystem";

    // configuration events

    public static final String ADD_MANAGED_REPO = "Added Managed Repository";

    public static final String MODIFY_MANAGED_REPO = "Updated Managed Repository";

    public static final String DELETE_MANAGED_REPO = "Deleted Managed Repository";

    public static final String ADD_REMOTE_REPO = "Added Remote Repository";

    public static final String MODIFY_REMOTE_REPO = "Updated Remote Repository";

    public static final String DELETE_REMOTE_REPO = "Deleted Remote Repository";

    public static final String ADD_REPO_GROUP = "Added Repository Group";

    public static final String DELETE_REPO_GROUP = "Deleted Repository Group";

    public static final String ADD_REPO_TO_GROUP = "Added Repository to Group";

    public static final String DELETE_REPO_FROM_GROUP = "Deleted Repository from Group";

    public static final String ENABLE_REPO_CONSUMER = "Enabled Content Consumer";

    public static final String DISABLE_REPO_CONSUMER = "Disabled Content Consumer";

    public static final String ENABLE_DB_CONSUMER = "Enabled Database Consumer";

    public static final String DISABLE_DB_CONSUMER = "Disabled Database Consumer";

    public static final String ADD_PATTERN = "Added File Type Pattern";

    public static final String REMOVE_PATTERN = "Removed File Type Pattern";

    public static final String DB_SCHEDULE = "Modified Scanning Schedule";

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

    public AuditEvent( String user, String resource, String action )
    {
        this( null, user, resource, action );
    }

    public AuditEvent( String principal, String action2 )
    {
        this( null, principal, action2 );
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
