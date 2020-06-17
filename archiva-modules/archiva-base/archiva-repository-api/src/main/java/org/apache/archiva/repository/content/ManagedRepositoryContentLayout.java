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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.ManagedRepositoryContent;

/**
 *
 * Basic interface for content layouts.
 * A content layout provides specific content item instances for the content structure like Namespace,
 * Project, Version and their relationships.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface ManagedRepositoryContentLayout
{

    /**
     * Returns the repository content, that this layout is attached to.
     * @return the content instance
     */
    ManagedRepositoryContent getGenericContent();

    /**
     * Adapts a generic content item to a specific implementation class.
     *
     * @param clazz the target implementation
     * @param item the content item
     * @param <T> the target class
     * @return the adapted instance
     * @throws LayoutException if the conversion is not possible
     */
    <T extends ContentItem> T adaptItem( Class<T> clazz, ContentItem item ) throws LayoutException;

}
