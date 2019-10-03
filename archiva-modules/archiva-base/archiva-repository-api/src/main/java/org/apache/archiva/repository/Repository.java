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

import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.event.EventSource;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.features.RepositoryFeature;
import org.apache.archiva.repository.storage.StorageAsset;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 *
 * Base interface for repositories.
 *
 * Created by Martin Stockhammer on 21.09.17.
 */
public interface Repository extends EventSource, RepositoryStorage {

    /**
     * Return the identifier of the repository. Repository identifier should be unique at least
     * for the same type.
     * @return The identifier.
     */
    String getId();

    /**
     * This is the display name of the repository. This string is presented in the user interface.
     *
     * @return The display name of the repository
     */
    String getName();

    /**
     * Returns the name in the given locale.
     * @param locale
     * @return
     */
    String getName(Locale locale);

    /**
     * Returns a description of this repository.
     * @return The description.
     */
    String getDescription();

    /**
     * Returns the description for the given locale.
     * @param locale
     * @return
     */
    String getDescription(Locale locale);

    /**
     * This identifies the type of repository. Archiva does only support certain types of repositories.
     *
     * @return A unique identifier for the repository type.
     */
    RepositoryType getType();


    /**
     * Returns the location of this repository. For local repositories this might be a file URI, for
     * remote repositories a http URL or some very repository specific schemes.
     * Each repository has only one unique location.
     *
     * @return The repository location.
     */
    URI getLocation();


    /**
     * Returns a storage representation to the local data stored for this repository.
     * The repository implementation may not store the real artifacts in this path. The directory structure
     * is completely implementation dependant.
     *
     */
    StorageAsset getLocalPath();


    /**
     * A repository may allow additional locations that can be used, if the primary location is not available.
     * @return
     */
    Set<URI> getFailoverLocations();

    /**
     * True, if this repository is scanned regularly.
     */
    boolean isScanned();

    /**
     * Returns the definition, when the repository jobs are executed.
     * This must return a valid a cron string.
     *
     * @See http://www.quartz-scheduler.org/api/2.2.1/org/quartz/CronExpression.html
     *
     * @return
     */
    String getSchedulingDefinition();

    /**
     * Returns true, if this repository has a index available
     * @return
     */
    boolean hasIndex();

    /**
     * Returns a layout definition. The returned string may be implementation specific and is not
     * standardized.
     *
     * @return
     */
    String getLayout();


    /**
     * Returns the capabilities of the repository implementation.
     * @return
     */
    RepositoryCapabilities getCapabilities();


    /**
     * Extension method that allows to provide different features that are not supported by all
     * repository types.
     *
     * @param clazz The feature class that is requested
     * @param <T> This is the class of the feature
     * @return The feature implementation for this repository instance, if it is supported
     * @throws UnsupportedFeatureException if the feature is not supported by this repository type
     */
    <T extends RepositoryFeature<T>> RepositoryFeature<T> getFeature(Class<T> clazz) throws UnsupportedFeatureException;


    /**
     * Returns true, if the requested feature is supported by this repository.
     *
     * @param clazz The requested feature class
     * @param <T> The requested feature class
     * @return True, if the feature is supported, otherwise false.
     */
    <T extends RepositoryFeature<T>> boolean supportsFeature(Class<T> clazz);


    /**
     * Returns a indexing context.
     * @return
     * @throws UnsupportedOperationException
     */
    ArchivaIndexingContext getIndexingContext();

    /**
     * Closes all resources that are opened by this repository.
     */
    void close();

    /**
     * Returns the current status of this repository.
     *
     * @return <code>true</code>, if repository has not been closed, otherwise <code>false</code>
     */
    boolean isOpen();


}
