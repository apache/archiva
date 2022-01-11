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
public interface MultiModelMapper<B,T,R>
{
    /**
     * Converts the source instance to a new instance of the target type.
     * @param source the source instance
     * @return a new instance of the target type
     */
    T map( B source);

    /**
     * Updates the target instance based on the source instance
     * @param source the source instance
     * @param target the target instance
     */
    void update( B source, T target );


    /**
     * Converts the target instance back to the source type
     * @param source the target instance
     * @return a new instance of the source type
     */
    B reverseMap( R source);

    /**
     * Updates the source instance based on the target instance
     * @param source the target instance
     * @param target the source instance
     */
    void reverseUpdate( R source, B target);

    /**
     * Returns the class name of the source type
     * @return the source type
     */
    Class<B> getBaseType();

    /**
     * Returns the class name of type that is the goal for the mapping.
     * @return the target type
     */
    Class<T> getDestinationType();

    /**
     * Returns the class name of the source for the reverse mapping.
     * @return
     */
    Class<R> getReverseSourceType();

    /**
     * Returns <code>true</code>, if the given type are the same or supertype of the mapping types.
     * @param baseType
     * @param destinationType
     * @param reverseSourceType
     * @param <S>
     * @param <T>
     * @return
     */
    <S, T, R> boolean supports( Class<S> baseType, Class<T> destinationType, Class<R> reverseSourceType);

    default int getHash() {
        return getHash(getBaseType( ), getDestinationType( ), getReverseSourceType( ) );
    }

    static int getHash(Class<?> baseType, Class<?> destinationType, Class<?> reverseSourceType) {
        return baseType.hashCode( ) ^ destinationType.hashCode( ) ^ reverseSourceType.hashCode( );
    }

}
