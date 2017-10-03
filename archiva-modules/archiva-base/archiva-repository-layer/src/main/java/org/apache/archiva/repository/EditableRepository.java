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

import java.net.URI;
import java.util.Locale;

/**
 * This is the editable part of a repository.
 * Normally a repository should also implement this interface but it is not
 * required.
 *
 * Capabilities and features are a integral part of the implementation and not
 * provided here by the interface.
 * Feature setting methods are provided by the features itself.
 *
 */
public interface EditableRepository extends Repository
{
    /**
     * Returns the primary locale used for setting the default values for
     * name and description.
     *
     * @return The locale used for name and description when they are not set
     */
    Locale getPrimaryLocale();

    /**
     * Sets the name for the given locale
     *
     * @param locale the locale for which the name is set
     * @param name The name value in the language that matches the locale
     */
    void setName( Locale locale, String name);

    /**
     * Sets the description for the given locale
     *
     * @param locale the locale for which the description is set
     * @param description The description in the language that matches the locale.
     */
    void setDescription(Locale locale, String description);

    /**
     * Sets the location of the repository. May be a URI that is suitable for the
     * repository implementation. Not all implementations will accept the same URI schemes.
     * @param location the location URI
     * @throws UnsupportedURIException if the URI scheme is not supported by the repository type.
     */
    void setLocation(URI location) throws UnsupportedURIException;

    /**
     * Adds a failover location for the repository.
     *
     * @param location The location that should be used as failover.
     * @throws UnsupportedURIException if the URI scheme is not supported by the repository type.
     */
    void addFailoverLocation(URI location) throws UnsupportedURIException;

    /**
     * Removes a failover location from the set.
     *
     * @param location the location uri to remove
     */
    void removeFailoverLocation(URI location);

    /**
     * Clears the failover location set.
     */
    void clearFailoverLocations();

    /**
     * Sets the flag for scanning the repository. If true, the repository will be scanned.
     * You have to set the scheduling times, if you set this to true.
     *
     * @param scanned if true, the repository is scanned regulary.
     */
    void setScanned(boolean scanned);

    /**
     * Adds a scheduling time at the given index.
     * @param index the index where time should be set
     * @param scheduleDefinition the scheduling definition to add
     */
    void addSchedulingTime(int index, ScheduleDefinition scheduleDefinition);

    /**
     * Adds a scheduling time to the end of the list.
     * @param scheduleDefinition the scheduling definition to set
     */
    void addSchedulingTime(ScheduleDefinition scheduleDefinition);

    /**
     * Removes the scheduling time definition from the list.
     * @param scheduleDefinition the scheduling definition to remove.
     */
    void removeSchedulingTime(ScheduleDefinition scheduleDefinition);

    /**
     * Removes the scheduling time definition add the given index.
     *
     * @param index the index to remove
     */
    void removeSchedulingTime(int index);

    /**
     * Clears the list of scheduling definitions.
     */
    void clearSchedulingTimes();

    /**
     * Set true, if the repository has indexes stored.
     * @param hasIndex true, if the repository has indexes, otherwise false.
     */
    void setIndex(boolean hasIndex);

    /**
     * Sets the path to the index directory. May be a relative or absolute URI.
     * @param indexPath the path
     */
    void setIndexPath(URI indexPath);

    /**
     * Sets the layout string.
     * @param layout
     */
    void setLayout(String layout);

    /**
     * Adds an active release scheme. Release schemes may be combined.
     * @param scheme the scheme to add.
     */
    void addActiveReleaseScheme(ReleaseScheme scheme);

    /**
     * Removes an active release scheme from the set.
     * @param scheme the scheme to remove.
     */
    void removeActiveReleaseScheme(ReleaseScheme scheme);

    /**
     * Clears all active release schemes.
     */
    void clearActiveReleaseSchemes();


}
