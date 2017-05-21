package org.apache.archiva.security;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;

import java.util.List;

/**
 * UserRepositories 
 *
 *
 */
public interface UserRepositories
{
    /**
     * Get the list of observable repository ids for the user specified.
     * 
     * @param principal the principle to obtain the observable repository ids from.
     * @return the list of observable repository ids.
     * @throws PrincipalNotFoundException
     * @throws AccessDeniedException
     * @throws ArchivaSecurityException
     */
    List<String> getObservableRepositoryIds( String principal )
        throws PrincipalNotFoundException, AccessDeniedException, ArchivaSecurityException;
    
    /**
     * Get the list of writable repository ids for the user specified.
     * 
     * @param principal the principle to obtain the observable repository ids from.
     * @return the list of observable repository ids.
     * @throws PrincipalNotFoundException
     * @throws AccessDeniedException
     * @throws ArchivaSecurityException
     */
    List<String> getManagableRepositoryIds( String principal )
        throws PrincipalNotFoundException, AccessDeniedException, ArchivaSecurityException;
    
    /**
     * Create any missing repository roles for the provided repository id.
     * 
     * @param repoId the repository id to work off of.
     * @throws ArchivaSecurityException if there was a problem creating the repository roles.
     */
    void createMissingRepositoryRoles( String repoId )
        throws ArchivaSecurityException;
    
    /**
     * Check if user is authorized to upload artifacts in the repository.
     * 
     * @param principal
     * @param repoId
     * @return
     * @throws PrincipalNotFoundException
     * @throws ArchivaSecurityException
     */
    boolean isAuthorizedToUploadArtifacts( String principal, String repoId)
        throws PrincipalNotFoundException, ArchivaSecurityException;
     
    /**
     * Check if user is authorized to delete artifacts in the repository.
     * 
     * @param principal
     * @param repoId
     * @return
     * @throws ArchivaSecurityException 
     * @throws AccessDeniedException 
     */
    boolean isAuthorizedToDeleteArtifacts( String principal, String repoId )
        throws AccessDeniedException, ArchivaSecurityException;

    /**
     *
     * @param principal
     * @since 1.4-M3
     * @return
     * @throws ArchivaSecurityException
     * @throws AccessDeniedException
     * @throws PrincipalNotFoundException
     */
    List<ManagedRepository> getAccessibleRepositories( String principal )
            throws ArchivaSecurityException, AccessDeniedException, PrincipalNotFoundException;

    /**
     *
     * Returns a list of repositories for which the user has the manager role.
     *
     * @param principal
     * @since 2.2.2
     * @return
     * @throws ArchivaSecurityException
     * @throws AccessDeniedException
     * @throws PrincipalNotFoundException
     */
    List<ManagedRepository> getManagableRepositories( String principal )
            throws ArchivaSecurityException, AccessDeniedException, PrincipalNotFoundException;
    
}
