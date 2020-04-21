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

import org.apache.archiva.model.ArtifactReference;

/**
 * Represents a artifact of a repository. This object contains unique coordinates of the
 * artifact. A artifact has exactly one file representation in the repository.
 * The artifact instance does not tell, if the file exists or is readable. It just
 * keeps the coordinates and some meta information of the artifact.
 * <p>
 * Artifact implementations should be immutable. The implementation must not always represent the current state of the
 * corresponding storage asset (file). It is just a view of the attributes for a given point in time.
 * <p>
 * Implementations must provide proper hash and equals methods.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface Artifact extends ContentItem
{

    /**
     * The artifact identifier. The ID is unique in a given namespace of a given repository.
     * But there may exist artifacts with the same ID but different types, classifiers or extensions.
     * <p>
     * Never returns <code>null</code> or a empty string.
     *
     * @return the identifier of the artifact. Never returns <code>null</code> or empty string
     */
    String getId( );

    /**
     * The version string of the artifact. The version string is exactly the version that is attached
     * to the artifact.
     * The version string may be different to the version string returned by the attached {@link Version} object.
     * E.g for maven artifacts the artifact version may be 1.3-20070725.210059-1 and the attached {@link Version} object
     * has version 1.3-SNAPSHOT.
     *
     * @return the artifact version string
     * @see #getVersion()
     */
    String getArtifactVersion( );

    /**
     * Returns the attached version this artifact is part of.
     *
     * @return the version object
     */
    Version getVersion( );

    /**
     * Returns the type of the artifact. The type is some hint about the usage of the artifact.
     * Implementations may always return a empty string, if it is not used.
     *
     * @return the type of the artifact. Returns never <code>null</code>, but may be empty string
     */
    String getType( );

    /**
     * A classifier that distinguishes artifacts.
     * Implementations may always return a empty string, if it is not used.
     *
     * @return the classifier of the artifact. Returns never <code>null</code>, but may be empty string
     */
    String getClassifier( );

    /**
     * Short cut for the file name. Should always return the same value as the artifact name.
     *
     * @return the name of the file
     */
    default String getFileName( )
    {
        return getAsset( ).getName( );
    }

    /**
     * Returns the extension of the file. This method should always return the extension string after the last
     * '.'-character.
     *
     * @return the file name extension
     */
    default String getExtension( )
    {
        final String name = getAsset( ).getName( );
        final int idx = name.lastIndexOf( '.' )+1;
        if ( idx > 0 )
        {
            return name.substring( idx );
        }
        else
        {
            return "";
        }
    }

    /**
     * This may be different from extension and gives the remainder that is used to build the file path from
     * the artifact coordinates (namespace, id, version, classifier, type)
     *
     * @return the file name remainder
     */
    String getRemainder( );

    /**
     * Should return the mime type of the artifact.
     *
     * @return the mime type of the artifact.
     */
    String getContentType( );

    /**
     * Returns the type of the artifact
     * @return
     */
    ArtifactType getArtifactType();

    /**
     * Returns a unique key
     * @return
     */
    String toKey();



}
