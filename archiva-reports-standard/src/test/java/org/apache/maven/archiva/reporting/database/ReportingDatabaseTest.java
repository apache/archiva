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

import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;

/**
 * Test for {@link ReportingDatabase}.
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class ReportingDatabaseTest
    extends AbstractRepositoryReportsTestCase
{
    private ReportingDatabase database;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        database = (ReportingDatabase) lookup( ReportingDatabase.ROLE );
    }

    protected void tearDown()
        throws Exception
    {
        release( database );
        super.tearDown();
    }

    public void testLookup()
    {
        assertNotNull( "database should not be null.", database );
        assertNotNull( "database.artifactDatabase should not be null.", database.getArtifactDatabase() );
        assertNotNull( "database.metadataDatabase should not be null.", database.getMetadataDatabase() );
    }
}
