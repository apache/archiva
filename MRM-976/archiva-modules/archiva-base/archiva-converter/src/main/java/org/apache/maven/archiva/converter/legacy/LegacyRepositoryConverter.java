package org.apache.maven.archiva.converter.legacy;

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

import org.apache.maven.archiva.converter.RepositoryConversionException;

import java.io.File;
import java.util.List;

/**
 * Convert an entire repository.
 * 
 * @author Jason van Zyl
 */
public interface LegacyRepositoryConverter
{
    String ROLE = LegacyRepositoryConverter.class.getName();

    /**
     * Convert a legacy repository to a modern repository. This means a Maven 1.x repository
     * using v3 POMs to a Maven 2.x repository using v4.0.0 POMs.
     *
     * @param legacyRepositoryDirectory the directory of the legacy repository. 
     * @param destinationRepositoryDirectory the directory of the modern repository.
     * @param fileExclusionPatterns the list of patterns to exclude from the conversion.
     * @throws RepositoryConversionException 
     */
    void convertLegacyRepository( File legacyRepositoryDirectory, File destinationRepositoryDirectory,
                                  List fileExclusionPatterns )
        throws RepositoryConversionException;
}
