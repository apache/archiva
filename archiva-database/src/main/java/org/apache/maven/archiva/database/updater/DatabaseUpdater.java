package org.apache.maven.archiva.database.updater;

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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.model.ArchivaArtifact;

/**
 * The database update component. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface DatabaseUpdater
{
    /**
     * Execute the {@link #updateAllUnprocessed()} and {@link #updateAllProcessed()}
     * tasks in one go.
     * 
     * @throws ArchivaDatabaseException
     */
    public void update()
        throws ArchivaDatabaseException;

    /**
     * Update all unprocessed content.
     * 
     * @throws ArchivaDatabaseException if there was a fatal error with the database.
     */
    public void updateAllUnprocessed()
        throws ArchivaDatabaseException;

    /**
     * Update specific unprocessed content.
     * 
     * @throws ArchivaDatabaseException if there was a fatal error with the database.
     */
    public void updateUnprocessed( ArchivaArtifact artifact )
        throws ArchivaDatabaseException;

    /**
     * Update all previously processed content.
     * 
     * This is done to allow archiva to remove content from the database that 
     * may have been removed from the filesystem too.
     * 
     * @throws ArchivaDatabaseException if there was a fatal error with the database.
     */
    public void updateAllProcessed()
        throws ArchivaDatabaseException;

    /**
     * Update specific processed content.
     * 
     * Example: This is done to allow a specific artifact to be removed from the
     * database if it no longer exists on the filesystem.
     * 
     * @throws ArchivaDatabaseException if there was a fatal error with the database.
     */
    public void updateProcessed( ArchivaArtifact artifact )
        throws ArchivaDatabaseException;
}
