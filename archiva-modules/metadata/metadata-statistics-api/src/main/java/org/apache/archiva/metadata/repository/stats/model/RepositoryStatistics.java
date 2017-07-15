package org.apache.archiva.metadata.repository.stats.model;

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

import org.apache.archiva.metadata.model.MetadataFacet;

import java.util.Date;
import java.util.Map;

/**
 *
 * Provides statistics data of metadata repositories.
 *
 * @since 2.3
 */
public interface RepositoryStatistics extends MetadataFacet
{
    String FACET_ID = "org.apache.archiva.metadata.repository.stats";

    String getRepositoryId( );

    Date getScanEndTime( );

    Date getScanStartTime( );

    long getTotalArtifactCount( );

    void setTotalArtifactCount( long totalArtifactCount );

    long getTotalArtifactFileSize( );

    void setTotalArtifactFileSize( long totalArtifactFileSize );

    long getTotalFileCount( );

    void setTotalFileCount( long totalFileCount );

    long getTotalGroupCount( );

    void setTotalGroupCount( long totalGroupCount );

    long getTotalProjectCount( );

    void setTotalProjectCount( long totalProjectCount );

    void setNewFileCount( long newFileCount );

    long getNewFileCount( );

    long getDuration( );

    /**
     * Statistics data by artifact type.
     *
     * @return A list of data keys and values
     */
    Map<String, Long> getTotalCountForType( );

    /**
     * Returns the value for the given artifact type.
     *
     * @param type The artifact type
     * @return The count value.
     */
    long getTotalCountForType( String type );

    /**
     * Sets the value for the given artifact type.
     * @param type The artifact type.
     * @param count The count value.
     */
    void setTotalCountForType( String type, long count );

    /**
     * Reads custom statistic values that are store implementation
     * specific.
     *
     * @param fieldName A unique field name.
     */
    long getCustomValue(String fieldName);

    /**
     * Saves custom statistic values that are store implementation
     * specific. The field name should be unique (e.g. prefixed by the
     * package name of the data provider).
     *
     * @param fieldName A unique field name.
     * @param count The statistic counter value
     */
    void setCustomValue(String fieldName, long count);

}
