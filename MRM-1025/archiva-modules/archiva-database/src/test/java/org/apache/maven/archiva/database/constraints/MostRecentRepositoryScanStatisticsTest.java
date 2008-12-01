package org.apache.maven.archiva.database.constraints;

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

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.model.RepositoryContentStatistics;

import java.util.List;

/**
 * MostRecentRepositoryScanStatisticsTest 
 *
 * @version $Id$
 */
public class MostRecentRepositoryScanStatisticsTest
    extends AbstractArchivaDatabaseTestCase
{
    private RepositoryContentStatistics createStats( String repoId, String timestamp, long duration, long totalfiles,
                                                     long newfiles )
        throws Exception
    {
        RepositoryContentStatistics stats = new RepositoryContentStatistics();
        stats.setRepositoryId( repoId );
        stats.setDuration( duration );
        stats.setNewFileCount( newfiles );
        stats.setTotalFileCount( totalfiles );
        stats.setWhenGathered( toDate( timestamp ) );

        return stats;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        dao.save( createStats( "internal", "2007/02/21 10:00:00", 20000, 12000, 400 ) );
        dao.save( createStats( "internal", "2007/02/20 10:00:00", 20000, 11800, 0 ) );
        dao.save( createStats( "internal", "2007/02/19 10:00:00", 20000, 11800, 100 ) );
        dao.save( createStats( "internal", "2007/02/18 10:00:00", 20000, 11700, 320 ) );
    }

    public void testNotProcessedYet()
        throws Exception
    {
        List results = dao.query( new MostRecentRepositoryScanStatistics( "central" ) );
        assertNotNull( "Not Processed Yet", results );
        assertTrue( "Not Processed Yet", results.isEmpty() );
    }

    public void testStats()
        throws Exception
    {
        List results = dao.query( new MostRecentRepositoryScanStatistics( "internal" ) );
        assertNotNull( "Stats: results (not null)", results );
        assertEquals( "Stats: results.size", 1, results.size() );

        Object o = results.get( 0 );
        assertTrue( "Stats: result[0] instanceof RepositoryScanStatistics", o instanceof RepositoryContentStatistics );
        RepositoryContentStatistics stats = (RepositoryContentStatistics) o;
        assertEquals( "Stats: id", "internal", stats.getRepositoryId() );
        assertEquals( "Stats: when gathered", "2007/02/21 10:00:00", fromDate( stats.getWhenGathered() ) );
        assertEquals( "Stats: duration", 20000, stats.getDuration() );
        assertEquals( "Stats: total file count", 12000, stats.getTotalFileCount() );
        assertEquals( "Stats: new file count", 400, stats.getNewFileCount() );
    }

}
