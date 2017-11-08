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
 * Describe the capabilities a repository implementation supports.
 */
public interface RepositoryCapabilities {

    /**
     * Returns true, if this repository has a mechanism for indexes
     * @return true, if this repository is indexable, otherwise false.
     */
    default boolean isIndexable() {
        return true;
    }

    /**
     * Returns true, if this repository type is storing its artifacts on the filesystem.
     * @return true, if this is a file based repository, otherwise false.
     */
    default boolean isFileBased() {
        return true;
    }

    /**
     * Returns true, if this repository allows to block redeployments to prevent overriding
     * released artifacts
     * @return true, if this repo can block redeployments, otherwise false.
     */
    default boolean canBlockRedeployments() {
        return true;
    }

    /**
     * Returns true, if the artifacts can be scanned for metadata retrieval or maintenance tasks
     * @return true, if this repository can be scanned regularily, otherwise false.
     */
    default boolean isScannable() {
        return true;
    }

    /**
     * Returns true, if this repository can use failover repository urls
     * @return true, if there is a failover mechanism for repository access, otherwise false.
     */
    default boolean allowsFailover() {
        return false;
    }

    /**
     * Returns the release schemes this repository type can handle
     */
    Set<ReleaseScheme> supportedReleaseSchemes();

    /**
     * Returns the layouts this repository type can provide
     * @return The list of layouts supported by this repository.
     */
    Set<String> supportedLayouts();

    /**
     * Returns additional capabilities, that this repository type implements.
     * @return A list of custom capabilities.
     */
    Set<String> customCapabilities();

    /**
     * Returns the supported features this repository type supports. This method returns
     * string that corresponds to fully qualified class names.
     * We use string representation to allow implementations provide their own feature
     * implementations if necessary and to avoid class errors.
     *
     * @return The list of supported features as string values.
     */
    Set<String> supportedFeatures();


}
