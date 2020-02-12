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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.UnsupportedConversionException;

import java.util.Map;

public interface ContentItem
{
    /**
     * Returns the repository type specific implementation
     * @param clazz the specific implementation class
     * @param <T> the class or interface
     * @return the specific project implementation
     */
    <T extends Project> T adapt( Class<T> clazz ) throws UnsupportedConversionException;

    /**
     * Returns <code>true</code>, if this project supports the given adaptor class.
     * @param clazz the class to convert this project to
     * @param <T> the type
     * @return <code>true/code>, if the implementation is supported, otherwise false
     */
    <T extends Project> boolean supports( Class<T> clazz );

    /**
     * Additional attributes
     * @return the additional attributes
     */
    Map<String, String> getAttributes( );

    /**
     * Returns the attribute value for the given key.
     *
     * @param key the attribute key
     * @return the value, if the key exists, otherwise <code>null</code>
     *
     */
    String getAttribute( String key );
}
