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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * RepositoryManager
 */
public interface RepositoryManager
{
    /**
     * Checks if the RepositoryManager instance can handle the data represented by the ResourceContext
     *
     * If the RepositoryManager can handle the ResourceContext it should return 
     * the same ResourceContext or a mutated version which is then passed to other methods on object.
     *
     * Should return null if unable to handle the ResourceContext
     *
     * @param context
     * @return context
     */
    ResourceContext handles(ResourceContext context);

    /**
     * Returns a list of Status objects that represent the current status
     * of the resources and collections represented by the ResourceContext
     *
     * If the ResourceContext is a collection then stat should return
     * a list of Status objects for all children where the first element in
     * the list is the status of the collection itself
     *
     * If the ResourceContext is a resource (file) then the returned list should
     * only return a list with a single Status representing the ResourceContext
     *
     * @param context
     * @return statusList
     */
    List<Status> stat(ResourceContext context);

    /**
     * Reads the data represented by the ResourceContext to the OutputStream
     * @param context
     * @param os
     * @return success
     */
    boolean read(ResourceContext context, OutputStream os);

    /**
     * Writes the data represented by the ResourceContext from the InputStream
     * @param context
     * @param is
     * @return success
     */
    boolean write(ResourceContext context, InputStream is);

    /**
     * Checks if the repositoryId exists as part of this repository manager
     * @param repositoryId
     * @return exists
     */
    boolean exists(String repositoryId);
}
