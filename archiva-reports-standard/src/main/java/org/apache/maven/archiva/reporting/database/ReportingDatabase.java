package org.apache.maven.archiva.reporting.database;

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

import java.util.Iterator;

/**
 * The Main Reporting Database.
 * 
 * @todo i18n, including message formatting and parameterisation
 * @plexus.component role="org.apache.maven.archiva.reporting.database.ReportingDatabase"
 *                   role-hint="default"
 */
public class ReportingDatabase
{
    public static final String ROLE = ReportingDatabase.class.getName();

    /**
     * @plexus.requirement role-hint="default"
     */
    private ArtifactResultsDatabase artifactDatabase;

    /**
     * @plexus.requirement role-hint="default"
     */
    private MetadataResultsDatabase metadataDatabase;

    public Iterator getArtifactIterator()
    {
        return artifactDatabase.getIterator();
    }

    public Iterator getMetadataIterator()
    {
        return metadataDatabase.getIterator();
    }

    public void clear()
    {
    }

    /**
     * <p>
     * Get the number of failures in the database.
     * </p>
     * 
     * <p>
     * <b>WARNING:</b> This is a very resource intensive request. Use sparingly.
     * </p>
     * 
     * @return the number of failures in the database.
     */
    public int getNumFailures()
    {
        int count = 0;
        count += artifactDatabase.getNumFailures();
        count += metadataDatabase.getNumFailures();
        return count;
    }

    /**
     * <p>
     * Get the number of notices in the database.
     * </p>
     * 
     * <p>
     * <b>WARNING:</b> This is a very resource intensive request. Use sparingly.
     * </p>
     * 
     * @return the number of notices in the database.
     */
    public int getNumNotices()
    {
        int count = 0;
        count += artifactDatabase.getNumNotices();
        count += metadataDatabase.getNumNotices();
        return count;
    }

    /**
     * <p>
     * Get the number of warnings in the database.
     * </p>
     * 
     * <p>
     * <b>WARNING:</b> This is a very resource intensive request. Use sparingly.
     * </p>
     * 
     * @return the number of warnings in the database.
     */
    public int getNumWarnings()
    {
        int count = 0;
        count += artifactDatabase.getNumWarnings();
        count += metadataDatabase.getNumWarnings();
        return count;
    }

    public ArtifactResultsDatabase getArtifactDatabase()
    {
        return artifactDatabase;
    }

    public MetadataResultsDatabase getMetadataDatabase()
    {
        return metadataDatabase;
    }
}
