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

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * The item selector is used to specify coordinates for retrieving ContentItem elements.
 */
public interface ItemSelector
{

    /**
     * Selects the namespace to search for. You can use the {@link #recurse()} flag
     * to decide, if only the given namespace or the namespace and all sub namespaces (if they exist) should be
     * queried. If empty, the root namespace is searched.
     * @return the namespace to search
     */
    String getNamespace( );

    /**
     * Selects the project id to search for. If empty all projects are searched.
     * @return the project id
     */
    String getProjectId( );

    /**
     * Selects the version to search for. If empty all versions are searched.
     * @return the version
     */
    String getVersion( );

    /**
     * Selects a specific artifact version. This may be different from the version, e.g.
     * for SNAPSHOT versions. If empty, the artifact version will be ignored.
     * @return the artifact version or empty string
     */
    String getArtifactVersion( );

    /**
     * Returns the artifact id to search for. If empty, all artifacts are returned.
     * @return the artifact id or a empty string
     */
    String getArtifactId( );

    /**
     * Returns the type to search for. If empty, the type is ignored.
     * @return the type or a empty string.
     */
    String getType( );

    /**
     * Returns the classifier string used for querying, or empty string if no classifier.
     * If it returns a '*' than all classifiers should be selected.
     * @return the classifier string
     */
    String getClassifier( );

    /**
     * Returns the attribute to search for or <code>null</code>, if the
     * attribute key should not be used for search.
     * @param key the attribute key
     * @return
     */
    String getAttribute( String key );

    /**
     * The extension of the file/asset.
     * @return
     */
    String getExtension( );

    /**
     * The map of attributes to search for
     * @return
     */
    Map<String, String> getAttributes( );

    /**
     * Returns <code>true</code>, if the query should recurse into all sub directories for
     * retrieving artifacts.
     */
    boolean recurse();

    /**
     * <code>true</code>, if all files/assets should be returned that match the given selector,
     * or <code>false</code>, if only the main assets should be returned.
     * Related assets are e.g. hash files or signature files.
     * @return <code>true</code>, if all assets should be found otherwise <code>false</code>
     */
    boolean includeRelatedArtifacts();

    default boolean hasNamespace( )
    {
        return !StringUtils.isEmpty( getNamespace( ) );
    }

    default boolean hasProjectId( )
    {
        return !StringUtils.isEmpty( getProjectId( ) );
    }

    default boolean hasVersion( )
    {
        return !StringUtils.isEmpty( getVersion( ) );
    }

    default boolean hasArtifactId( )
    {
        return !StringUtils.isEmpty( getArtifactId( ) );
    }

    default boolean hasArtifactVersion( )
    {
        return !StringUtils.isEmpty( getArtifactVersion( ) );
    }

    default boolean hasType( )
    {
        return !StringUtils.isEmpty( getType( ) );
    }

    default boolean hasClassifier( )
    {
        return !StringUtils.isEmpty( getClassifier( ) );
    }

    boolean hasAttributes( );

    boolean hasExtension( );
}
