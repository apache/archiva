package org.apache.maven.archiva.web;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.security.user.User;

/**
 * ArchivaSecurityDefaults
 *
 * NOTE: this is targeted for removal with the forth coming rbac role templating 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ArchivaSecurityDefaults
{
    public static final String ROLE = ArchivaSecurityDefaults.class.getName();

    public static final String GUEST_USERNAME = "guest";
    
    public static final String CONFIGURATION_EDIT_OPERATION = "edit-configuration";
    
    public static final String CONFIGURATION_EDIT_PERMISSION = "Edit Configuration";
    
    public static final String INDEX_REGENERATE_OPERATION = "regenerate-index";

    public static final String INDEX_REGENERATE_PERMISSION = "Regenerate Index";

    public static final String INDEX_RUN_OPERATION = "run-indexer";

    public static final String INDEX_RUN_PERMISSION = "Run Indexer";

    public static final String REPORTS_ACCESS_OPERATION = "access-reports";

    public static final String REPORTS_ACCESS_PERMISSION = "Access Reports";

    public static final String REPORTS_GENERATE_OPERATION = "generate-reports";

    public static final String REPORTS_GENERATE_PERMISSION = "Generate Reports";

    public static final String REPOSITORY_ACCESS = "Access Repository";

    public static final String REPOSITORY_ACCESS_OPERATION = "read-repository";

    public static final String REPOSITORY_ADD_OPERATION = "add-repository";

    public static final String REPOSITORY_ADD_PERMISSION = "Add Repository";

    public static final String REPOSITORY_DELETE = "Delete Repository";
    
    public static final String REPOSITORY_DELETE_OPERATION = "delete-repository";

    public static final String REPOSITORY_EDIT = "Edit Repository";
    
    public static final String REPOSITORY_EDIT_OPERATION = "edit-repository";
    
    public static final String REPOSITORY_MANAGER = "Repository Manager";
    
    public static final String REPOSITORY_OBSERVER = "Repository Observer";

    public static final String REPOSITORY_UPLOAD = "Repository Upload";

    public static final String REPOSITORY_UPLOAD_OPERATION = "upload-repository";

    public static final String ROLES_GRANT_OPERATION = "grant-roles";

    public static final String ROLES_GRANT_PERMISSION = "Grant Roles";

    public static final String ROLES_REMOVE_OPERATION = "remove-roles";

    public static final String ROLES_REMOVE_PERMISSION = "Remove Roles";

    public static final String SYSTEM_ADMINISTRATOR = "System Administrator";

    public static final String USER_ADMINISTRATOR = "User Administrator";

    public static final String USER_EDIT_OPERATION = "edit-user";

    public static final String USERS_EDIT_ALL_OPERATION = "edit-all-users";

    public static final String USERS_EDIT_ALL_PERMISSION = "Edit All Users";
    
    public void ensureDefaultsExist();
    public User getGuestUser();
}
