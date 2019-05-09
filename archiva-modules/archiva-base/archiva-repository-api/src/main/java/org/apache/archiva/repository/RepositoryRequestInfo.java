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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.features.RepositoryFeature;

/**
 * This interface is for mapping web request paths to artifacts.
 * The file system storage may differ from the paths used for web access.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface RepositoryRequestInfo
{

    /**
     * Returns the artifact reference for a given path.
     * Takes an incoming requested path (in "/" format) and gleans the layout
     * and ArtifactReference appropriate for that content.
     *
     * @param requestPath The path of the web request
     * @return The artifact reference
     * @throws LayoutException
     */
    ArtifactReference toArtifactReference( String requestPath ) throws LayoutException;

    /**
     * <p>
     * Tests the path to see if it conforms to the expectations of a metadata request.
     * </p>
     * <p>
     * NOTE: The implementation may do only a cursory check on the path's extension.  A result of true
     * from this method is not a guarantee that the support resource is in a valid format, or
     * that it even contains data.
     * </p>
     *
     * @param requestPath the path to test.
     * @return true if the requestedPath is likely a metadata request.
     */
    boolean isMetadata( String requestPath );

    /**
     * Returns true, if the given request points to a archetype catalog.
     *
     * @param requestPath
     * @return true if the requestedPath is likely an archetype catalog request.
     */
    boolean isArchetypeCatalog( String requestPath );

    /**
     * <p>
     * Tests the path to see if it conforms to the expectations of a support file request. Support files are used
     * for signing and validating the artifact files.
     * </p>
     * <p>
     * May test for certain extensions like <code>.sha1</code>, <code>.md5</code>, <code>.asc</code>, and <code>.php</code>.
     * </p>
     * <p>
     * NOTE: The implementation may do only a cursory check on the path's extension.  A result of true
     * from this method is not a guarantee that the support resource is in a valid format, or
     * that it even contains data.
     * </p>
     *
     * @param requestPath the path to test.
     * @return true if the requestedPath is likely that of a support file request.
     */
    boolean isSupportFile( String requestPath );

    /**
     * <p>
     * Tests the path to see if it conforms to the expectations of a support file request of the metadata file.
     * </p>
     * <p>
     * May test for certain extensions like <code>.sha1</code>, <code>.md5</code>, <code>.asc</code>, and <code>.php</code>.
     * </p>
     * <p>
     * NOTE: The implementation may do only a cursory check on the path's extension.  A result of true
     * from this method is not a guarantee that the support resource is in a valid format, or
     * that it even contains data.
     * </p>
     *
     * @param requestPath the path to test.
     * @return true if the requestedPath is likely that of a support file request.
     */
    boolean isMetadataSupportFile( String requestPath );

    /**
     * Returns the likely layout type for the given request.
     * Implementations may only check the path elements for this.  To make sure, the path is valid,
     * you should call {@link #toArtifactReference(String)}
     *
     * @return
     */
    String getLayout( String requestPath );

    /**
     * Adjust the requestedPath to conform to the native layout of the provided {@link org.apache.archiva.repository.ManagedRepositoryContent}.
     *
     * @param requestPath the incoming requested path.
     * @return the adjusted (to native) path.
     * @throws LayoutException if the path cannot be parsed.
     */
    String toNativePath( String requestPath)  throws LayoutException;

    /**
     * Extension method that allows to provide different features that are not supported by all
     * repository types.
     *
     * @param clazz The feature class that is requested
     * @param <T>   This is the class of the feature
     * @return The feature implementation for this repository instance, if it is supported
     * @throws UnsupportedFeatureException if the feature is not supported by this repository type
     */
    <T extends RepositoryFeature<T>> RepositoryFeature<T> getFeature( Class<T> clazz ) throws UnsupportedFeatureException;


    /**
     * Returns true, if the requested feature is supported by this repository.
     *
     * @param clazz The requested feature class
     * @param <T>   The requested feature class
     * @return True, if the feature is supported, otherwise false.
     */
    <T extends RepositoryFeature<T>> boolean supportsFeature( Class<T> clazz );


}
