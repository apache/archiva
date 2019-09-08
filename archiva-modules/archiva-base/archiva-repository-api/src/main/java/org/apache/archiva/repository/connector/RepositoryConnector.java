package org.apache.archiva.repository.connector;

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

import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;

import java.util.List;

/**
 *
 * A RepositoryConnector maps a managed repository to a remote repository.
 *
 *
 */
public interface RepositoryConnector
{
    /**
     * Returns the local repository that is connected to the remote.
     * @return The local managed repository.
     */
    ManagedRepository getSourceRepository();

    /**
     * Returns the remote repository that is connected to the local.
     * @return The remote repository.
     */
    RemoteRepository getTargetRepository();

    /**
     * Returns a list of paths that are not fetched from the remote repository.
     * @return A list of paths.
     */
    List<String> getBlacklist();

    /**
     * Returns a list of paths that are fetched from the remote repository, even if a
     * parent path is in the blacklist.
     *
     * @return The list of paths.
     */
    List<String> getWhitelist();

    /**
     * Returns true, if this connector is enabled, otherwise false.
     * @return True, if enabled.
     */
    boolean isEnabled();

    /**
     * Enables this connector, if it was disabled before, otherwise does nothing.
     */
    void enable();

    /**
     * Disables this connector, if it was enabled before, otherwise does nothing.
     */
    void disable();

}
