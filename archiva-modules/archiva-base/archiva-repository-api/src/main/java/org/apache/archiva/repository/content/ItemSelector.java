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

    String getProjectId( );

    String getNamespace( );

    String getVersion( );

    String getArtifactVersion( );

    String getArtifactId( );

    String getType( );

    String getClassifier( );

    String getAttribute( String key );

    String getExtension( String extension );

    Map<String, String> getAttributes( );

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
