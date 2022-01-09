package org.apache.archiva.common;
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

/**
 * Generic interface for mapping DTOs
 *
 * @author Martin Schreier <martin_s@apache.org>
 */
public interface ModelMapper<S,T>
{
    /**
     * Converts the source instance to a new instance of the target type.
     * @param source the source instance
     * @return a new instance of the target type
     */
    T map(S source);

    /**
     * Updates the target instance based on the source instance
     * @param source the source instance
     * @param target the target instance
     */
    void update( S source, T target );


    /**
     * Converts the target instance back to the source type
     * @param target the target instance
     * @return a new instance of the source type
     */
    S reverseMap(T target);

    /**
     * Updates the source instance based on the target instance
     * @param target the target instance
     * @param source the source instance
     */
    void reverseUpdate( T target, S source);

    /**
     * Returns the class name of the source type
     * @return the source type
     */
    Class<S> getSourceType();

    /**
     * Returns the class name of the target type
     * @return the target type
     */
    Class<T> getTargetType();

    /**
     * Returns <code>true</code>, if the given type are the same or supertype of the mapping types.
     * @param sourceType
     * @param targetType
     * @param <S>
     * @param <T>
     * @return
     */
    <S, T> boolean supports( Class<S> sourceType, Class<T> targetType );

}
