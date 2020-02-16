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

/**
 * The project is the container for several versions each with different artifacts.
 *
 * <pre>
 * project +--> version 1 + ->  artifact 1
 *         |              |
 *         |              + ->  artifact 2
 *         |
 *         +--> version 2 ----> artifact 3
 * </pre>
 * <p>
 * Implementations must provide proper hash and equals methods.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface Project extends ContentItem
{

    /**
     * The namespace of the project
     *
     * @return the namespace
     */
    Namespace getNamespace( );

    /**
     * The id of the project
     *
     * @return the project id
     */
    String getId( );


}