package org.apache.archiva.repository.scanner;

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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;

/**
 * RepositoryScanner 
 *
 * @version $Id$
 */
public interface RepositoryScanner
{
    /**
     * The value to pass to {@link #scan(ManagedRepositoryConfiguration, long)} to have the scan
     * operate in a fresh fashion, with no check on changes based on timestamp.
     */
    public static final long FRESH_SCAN = 0;

    /**
     * <p>
     * Typical Ignorable Content patterns.
     * </p>
     * 
     * <p><strong>
     * NOTE: Do not use for normal webapp or task driven repository scanning.
     * </strong></p>
     * 
     * <p>
     * These patterns are only valid for archiva-cli and archiva-converter use.
     * </p>
     */
    public static final String[] IGNORABLE_CONTENT = {
        "bin/**",
        "reports/**",
        ".index",
        ".reports/**",
        ".maven/**",
        "**/.svn/**",
        "**/*snapshot-version",
        "*/website/**",
        "*/licences/**",
        "**/.htaccess",
        "**/*.html",
        "**/*.txt",
        "**/README*",
        "**/CHANGELOG*",
        "**/KEYS*" };

    /**
     * Scan the repository for content changes.
     * 
     * Internally, this will use the as-configured known and invalid consumer lists.
     * 
     * @param repository the repository to change.
     * @param changesSince the timestamp to use as a threshold on what is considered new or changed.
     *                     (To have all content be taken into consideration regardless of timestamp,
     *                      use the {@link #FRESH_SCAN} constant) 
     * @return the statistics for this scan.
     * @throws RepositoryScannerException if there was a fundamental problem with getting the discoverer started.
     */
    public RepositoryScanStatistics scan( ManagedRepositoryConfiguration repository, long changesSince )
        throws RepositoryScannerException;

    /**
     * Scan the repository for content changes.
     * 
     * Internally, this will use the as-configured known and invalid consumer lists.
     * 
     * @param repository the repository to change.
     * @param knownContentConsumers the list of consumers that follow the {@link KnownRepositoryContentConsumer} 
     *                              interface that should be used for this scan.
     * @param invalidContentConsumers the list of consumers that follow the {@link InvalidRepositoryContentConsumer} 
     *                                 interface that should be used for this scan.
     * @param ignoredContentPatterns list of patterns that should be ignored and not sent to any consumer.
     * @param changesSince the timestamp to use as a threshold on what is considered new or changed.
     *                     (To have all content be taken into consideration regardless of timestamp,
     *                      use the {@link #FRESH_SCAN} constant) 
     * @return the statistics for this scan.
     * @throws RepositoryScannerException if there was a fundamental problem with getting the discoverer started.
     */
    public RepositoryScanStatistics scan( ManagedRepositoryConfiguration repository,
                                             List<KnownRepositoryContentConsumer> knownContentConsumers,
                                             List<InvalidRepositoryContentConsumer> invalidContentConsumers,
                                             List<String> ignoredContentPatterns, long changesSince )
        throws RepositoryScannerException;

    Set<RepositoryScannerInstance> getInProgressScans();
}
