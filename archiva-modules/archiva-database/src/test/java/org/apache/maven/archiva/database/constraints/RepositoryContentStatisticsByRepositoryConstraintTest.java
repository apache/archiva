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

import java.util.List;

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.model.RepositoryContentStatistics;

/**
 * RepositoryContentStatisticsByRepositoryConstraintTest
 * 
 * @version
 */
public class RepositoryContentStatisticsByRepositoryConstraintTest
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

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        dao.getRepositoryContentStatisticsDAO().saveRepositoryContentStatistics(
                        createStats( "internal", "2007/10/21 8:00:00", 20000, 12000, 400 ) );
        dao.getRepositoryContentStatisticsDAO().saveRepositoryContentStatistics(
                        createStats( "internal", "2007/10/20 8:00:00", 20000, 11800, 0 ) );
        dao.getRepositoryContentStatisticsDAO().saveRepositoryContentStatistics(
                        createStats( "internal", "2007/10/19 8:00:00", 20000, 11800, 100 ) );
        dao.getRepositoryContentStatisticsDAO().saveRepositoryContentStatistics(
                        createStats( "internal", "2007/10/18 8:00:00", 20000, 11700, 320 ) );
    }

    public void testStats()
        throws Exception
    {
        Constraint constraint = new RepositoryContentStatisticsByRepositoryConstraint( "internal" );
        List<RepositoryContentStatistics> results = dao.getRepositoryContentStatisticsDAO().queryRepositoryContentStatistics( constraint );
        assertNotNull( "Stats: results (not null)", results );
        assertEquals( "Stats: results.size", 4, results.size() );

        assertEquals( "internal", ( (RepositoryContentStatistics) results.get( 0 ) ).getRepositoryId() );
        assertEquals( "internal", ( (RepositoryContentStatistics) results.get( 1 ) ).getRepositoryId() );
        assertEquals( "internal", ( (RepositoryContentStatistics) results.get( 2 ) ).getRepositoryId() );
        assertEquals( "internal", ( (RepositoryContentStatistics) results.get( 3 ) ).getRepositoryId() );
    }
    
    public void testStatsWithDateRange()
        throws Exception
    {
        Constraint constraint =
            new RepositoryContentStatisticsByRepositoryConstraint( "internal", toDate( "2007/10/18 8:00:00" ),
                                                                   toDate( "2007/10/20 8:00:00" ) );
        List<RepositoryContentStatistics> results = dao.getRepositoryContentStatisticsDAO().queryRepositoryContentStatistics( constraint );
        assertNotNull( "Stats: results (not null)", results );
        assertEquals( "Stats: results.size", 3, results.size() );

        assertEquals( "internal", ( (RepositoryContentStatistics) results.get( 0 ) ).getRepositoryId() );
        assertEquals( toDate( "2007/10/20 8:00:00" ),
                      ( (RepositoryContentStatistics) results.get( 0 ) ).getWhenGathered() );

        assertEquals( "internal", ( (RepositoryContentStatistics) results.get( 1 ) ).getRepositoryId() );
        assertEquals( toDate( "2007/10/19 8:00:00" ),
                      ( (RepositoryContentStatistics) results.get( 1 ) ).getWhenGathered() );

        assertEquals( "internal", ( (RepositoryContentStatistics) results.get( 2 ) ).getRepositoryId() );
        assertEquals( toDate( "2007/10/18 8:00:00" ),
                      ( (RepositoryContentStatistics) results.get( 2 ) ).getWhenGathered() );
    }
}
