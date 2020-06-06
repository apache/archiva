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

import java.util.List;

/**
 * Each artifact is attached to exactly one version.
 * <p>
 * Implementations must provide proper hash and equals methods.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface Version extends ContentItem
{

    /**
     * Returns the version string.
     *
     * @return the version string
     */
    String getId( );

    /**
     * Returns the version segments. E.g. for 1.3.4 it will return ["1","3"."4"]
     *
     * @return
     */
    List<String> getVersionSegments( );

    /**
     * Returns the namespace this version is attached to
     * @return the namespace instance
     */
    Namespace getNamespace();

    /**
     * Returns the project this version is attached to.
     *
     * @return the project instance. Will never return <code>null</code>
     */
    Project getProject( );

}
