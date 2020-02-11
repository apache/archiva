package org.apache.archiva.repository.content;

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

import org.apache.archiva.repository.UnsupportedRepositoryTypeException;
import org.apache.archiva.repository.storage.StorageAsset;

import java.util.Map;

/**
 *
 * Each artifact is attached to exactly one version.
 *
 * Implementations must provide proper hash and equals methods.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface Version extends Comparable<Version>
{
    /**
     * Returns the version string.
     *
     * @return the version string
     */
    String getVersion();

    /**
     * Returns the local representation of the version. For maven this is a directory.
     * It is implementation dependent, what exactly this asset points to.
     *
     * @return the local storage representation of the version
     */
    StorageAsset getAsset();

    /**
     * Each version is attached to a project.
     * @return the attached project
     */
    Project getProject();

    /**
     * Returns additional attributes.
     * @return the map of attribute key, value pairs
     */
    Map<String,String> getAttributes();

    /**
     * Returns the repository type specific implementation
     * @param clazz the specific implementation class
     * @param <T> the class or interface
     * @return the specific project implementation
     */
    <T extends Project> T adapt(Class<T> clazz) throws UnsupportedRepositoryTypeException;

    /**
     * Returns <code>true</code>, if this project supports the given adaptor class.
     * @param clazz the class to convert this project to
     * @param <T> the type
     * @return <code>true/code>, if the implementation is supported, otherwise false
     */
    <T extends Project> boolean supports(Class<T> clazz);

}
