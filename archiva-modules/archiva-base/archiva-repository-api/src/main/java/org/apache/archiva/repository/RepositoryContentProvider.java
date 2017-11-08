package org.apache.archiva.repository;

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

import java.util.Set;

/**
 * A repository content provider creates repository content instances for specific repository types.
 */
public interface RepositoryContentProvider
{
    /**
     * Returns true, if this content object supports the given layout otherwise, false.
     * @param layout the layout string
     * @return true, if layout is supported, otherwise false.
     */
    boolean supportsLayout(String layout);

    /**
     * Returns the repository types, this content object can be used for.
     *
     * @return all supported repository types.
     */
    Set<RepositoryType> getSupportedRepositoryTypes();


    /**
     * Returns true, if this content object supports the given repository type.
     *
     * @param type the type to check.
     * @return true, if the type is supported, otherwise false.
     */
    boolean supports(RepositoryType type);

    /**
     * Creates a new instance of RemoteRepositoryContent. The returned instance should be initialized
     * from the given repository data.
     *
     * @param repository the repository
     * @return a repository content instance
     * @throws RepositoryException if the layout is not supported, or a error occured during initialization
     */
    RemoteRepositoryContent createRemoteContent(RemoteRepository repository) throws RepositoryException;

    /**
     * Creates a new instance of ManagedRepositoryContent.
     *
     * @param repository the repository
     * @return a new instance
     * @throws RepositoryException if the layout is not supported, or a error occured during initialization
     */
    ManagedRepositoryContent createManagedContent(ManagedRepository repository) throws RepositoryException;

    /**
     * Creates a generic content object.
     *
     * @param repository the repository
     * @param clazz  the content class
     * @param <T> the generic type of the content
     * @param <V> the generic type of the repository (must correspond to the content class)
     * @return a new instance
     * @throws RepositoryException if the clazz, or layout is not supported, or something went wrong during initialization
     */
    <T extends RepositoryContent, V extends Repository> T createContent(Class<T> clazz, V repository) throws RepositoryException;
}
