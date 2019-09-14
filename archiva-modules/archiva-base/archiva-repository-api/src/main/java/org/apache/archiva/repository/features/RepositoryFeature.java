package org.apache.archiva.repository.features;

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


/**
 *
 * The repository feature holds information about specific features. The may not be available by all repository implementations.
 * Features should be simple objects for storing additional data, the should not implement too much functionality.
 * Additional functionality the uses the information in the feature objects should be implemented in the specific repository
 * provider and repository implementations, or in the repository registry if it is generic.
 *
 * But features may throw events, if it's data is changed.
 *
 *
 * This interface is to get access to a concrete feature by accessing the generic interface.
 *
 * @param <T> the concrete feature implementation.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public interface RepositoryFeature<T extends RepositoryFeature<T>> {

    /**
     * Unique Identifier of this feature. Each feature implementation has its own unique identifier.
     *
     * @return the identifier string which should be unique for the implementation class.
     */
    default String getId() {
        return this.getClass().getName();
    }

    /**
     * Tells, if this instance is a feature of the given identifier.
     *
     * @param featureId the feature identifier string to check
     * @return true, if this instance is a instance with the feature id, otherwise <code>false</code>
     */
    default boolean isFeature(String featureId) {
        return this.getClass().getName().equals(featureId);
    }

    /**
     * Tells, if the this instance is a feature of the given feature class.
     *
     * @param clazz The class to check against.
     * @param <K> the concrete feature implementation.
     * @return
     */
    default <K extends RepositoryFeature<K>> boolean isFeature(Class<K> clazz) {
        return this.getClass().equals(clazz);
    }

    /**
     * Returns the concrete feature instance.
     * @return the feature instance.
     */
    T get();
}
