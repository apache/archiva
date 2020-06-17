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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.content.ContentAccessException;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.content.ManagedRepositoryContentLayout;
import org.apache.archiva.repository.storage.StorageAsset;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface ManagedRepositoryContent extends RepositoryContent
{


    /**
     * Returns the path of the given item.
     *
     * @param item
     * @return
     */
    String toPath( ContentItem item );


    /**
     * <p>
     * Convenience method to get the repository id.
     * </p>
     * <p>
     * Equivalent to calling <code>.getRepository().getId()</code>
     * </p>
     *
     * @return the repository id.
     */
    String getId();

    /**
     * Delete all items that match the given selector. The type and number of deleted items
     * depend on the specific selector:
     * <ul>
     *     <li>namespace: the complete namespace is deleted (recursively if the recurse flag is set)</li>
     *     <li>project: the complete project and all contained versions are deleted</li>
     *     <li>version: the version inside the project is deleted (project is required)</li>
     *     <li>artifactId: all artifacts that match the id (project and version are required)</li>
     *     <li>artifactVersion: all artifacts that match the version (project and version are required)</li>
     *     <li></li>
     * </ul>
     *
     * @param selector the item selector that selects the artifacts to delete
     * @param consumer a consumer of the items that will be called after deletion
     * @returns the list of items that are deleted
     * @throws ContentAccessException if the deletion was not possible or only partly successful, because the access
     * to the artifacts failed
     * @throws IllegalArgumentException if the selector does not specify valid artifacts to delete
     */
    void deleteAllItems( ItemSelector selector, Consumer<ItemDeleteStatus> consumer ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Removes the specified content item and if the item is a container or directory,
     * all content stored under the given item.
     *
     * @param item the item.
     * @throws ItemNotFoundException if the item cannot be found
     * @throws ContentAccessException if the deletion was not possible or only partly successful, because the access
     *  to the artifacts failed
     */
    void deleteItem( ContentItem item ) throws ItemNotFoundException, ContentAccessException;

    /**
     * Returns a item for the given selector. The type of the returned item depends on the
     * selector.
     *
     * @param selector the item selector
     * @return the content item that matches the given selector
     * @throws ContentAccessException if an error occured while accessing the backend
     * @throws IllegalArgumentException if the selector does not select a valid content item
     */
    ContentItem getItem( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Returns a stream of items that match the given selector. It may return a stream of mixed types,
     * like namespaces, projects, versions and artifacts. It will not select a specific type.
     * The selector can specify the '*' pattern for all fields.
     * The returned elements will be provided by depth first.
     *
     * @param selector the item selector that specifies the items
     * @return the stream of content items
     * @throws ContentAccessException if the access to the underlying storage failed
     * @throws IllegalArgumentException if a illegal coordinate combination was provided
     */
    Stream<? extends ContentItem> newItemStream( ItemSelector selector, boolean parallel ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Returns the item that matches the given path. The item at the path must not exist.
     *
     * @param path the path string that points to the item
     * @return the content item if the path is a valid item path
     * @throws LayoutException if the path is not valid for the repository layout
     */
    ContentItem toItem( String path ) throws LayoutException;

    /**
     * Returns the item that matches the given asset path. The asset must not exist.
     *
     * @param assetPath the path to the artifact or directory
     * @return the item, if it is a valid path for the repository layout
     * @throws LayoutException if the path is not valid for the repository
     */
    ContentItem toItem( StorageAsset assetPath ) throws LayoutException;

    /**
     * Returns true, if the selector coordinates point to a existing item in the repository.
     *
     * @param selector the item selector
     * @return <code>true</code>, if there exists such a item, otherwise <code>false</code>
     */
    boolean hasContent( ItemSelector selector );

    /**
     * Get the repository configuration associated with this
     * repository content.
     *
     * @return the repository that is associated with this repository content.
     */
    ManagedRepository getRepository();

    /**
     * Set the repository configuration to associate with this
     * repository content.
     *
     * @param repo the repository to associate with this repository content.
     */
    void setRepository( ManagedRepository repo );

    /**
     * Returns the parent of the item.
     * @param item the current item
     * @return the parent item, or <code>null</code> if no such item exists
     */
    ContentItem getParent(ContentItem item);

    /**
     * Returns the list of children items.
     * @param item the current item
     * @return the list of children, or a empty list, if no children exist
     */
    List<? extends ContentItem> getChildren( ContentItem item);

    /**
     * Tries to apply the given characteristic to the content item. If the layout does not allow this,
     * it will throw a <code>LayoutException</code>.
     *
     * @param clazz the characteristic class to apply
     * @param item the content item
     * @param <T> The characteristic
     * @return the applied characteristic
     */
    <T extends ContentItem> T applyCharacteristic(Class<T> clazz, ContentItem item) throws LayoutException;

    /**
     * Returns the given layout from the content.
     * @param clazz The layout class
     * @param <T> the layout class
     * @return the specific layout
     * @throws LayoutException if the repository does not support this layout type
     */
    <T extends ManagedRepositoryContentLayout> T getLayout( Class<T> clazz) throws LayoutException;

    /**
     * Returns <code>true</code>, if the specific layout is supported by this content.
     * @param clazz the layout class
     * @return <code>true</code>, if the layout is supported, otherwise <code>false</code>
     */
    <T extends ManagedRepositoryContentLayout> boolean supportsLayout(Class<T> clazz);

    /**
     * Returns a list of supported layout classes
     * @return
     */
    List<Class<? extends ManagedRepositoryContentLayout>> getSupportedLayouts( );
}
