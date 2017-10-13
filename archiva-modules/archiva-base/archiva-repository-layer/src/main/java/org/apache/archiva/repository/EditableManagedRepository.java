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

/**
 * The editable part of a managed repository.
 */
public interface EditableManagedRepository extends EditableRepository, ManagedRepository
{
    /**
     * If true, the repository blocks redeployments of artifacts with the same version.
     * @param blocksRedeployment The flag for blocking redeployments.
     */
    void setBlocksRedeployment(boolean blocksRedeployment);

    /**
     * Sets the content
     * @param content
     */
    void setContent(ManagedRepositoryContent content);

    /**
     * Adds an active release scheme. Release schemes may be combined.
     * @param scheme the scheme to add.
     */
    void addActiveReleaseScheme(ReleaseScheme scheme);

    /**
     * Removes an active release scheme from the set.
     * @param scheme the scheme to remove.
     */
    void removeActiveReleaseScheme(ReleaseScheme scheme);

    /**
     * Clears all active release schemes.
     */
    void clearActiveReleaseSchemes();

}
