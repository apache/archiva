package org.apache.maven.archiva.reporting;

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
import org.apache.maven.archiva.database.ObjectNotFoundException;

import java.util.List;

/**
 * DynamicReportSource 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface DynamicReportSource
{
    /**
     * The human readable name of this report.
     * 
     * @return the name of the report.
     */
    public String getName();

    /**
     * Get the entire list of values for this report.
     * 
     * @return the complete List of objects for this report.
     * @throws ArchivaDatabaseException if there was a fundamental issue with accessing the database.
     * @throws ObjectNotFoundException  if no records were found.
     */
    public List getData() throws ObjectNotFoundException, ArchivaDatabaseException;

    /**
     * Get the entire list of values for this report.
     * 
     * @param limits the limits on the data to fetch. (NOTE: This object is 
     * updated by the underlying implementation of this interface with
     * the current values appropriate for the limits object).
     * @return the complete List of objects for this report.
     * @throws ArchivaDatabaseException if there was a fundamental issue with accessing the database.
     * @throws ObjectNotFoundException  if no records were found.
     */
    public List getData( DataLimits limits ) throws ObjectNotFoundException, ArchivaDatabaseException;
}
