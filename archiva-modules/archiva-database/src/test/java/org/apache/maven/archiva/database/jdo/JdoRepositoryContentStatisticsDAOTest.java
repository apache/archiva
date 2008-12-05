package org.apache.maven.archiva.database.jdo;

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

import javax.jdo.JDOHelper;

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.database.constraints.RepositoryContentStatisticsByRepositoryConstraint;
import org.apache.maven.archiva.model.RepositoryContentStatistics;

/**
 * JdoRepositoryContentStatisticsDAOTest
 * 
 * @version
 */
public class JdoRepositoryContentStatisticsDAOTest
    extends AbstractArchivaDatabaseTestCase
{
    public void testCRUD()
        throws Exception
    {
        RepositoryContentStatisticsDAO repoContentStatisticsDAO = dao.getRepositoryContentStatisticsDAO();

        // create
        RepositoryContentStatistics savedStats =
           repoContentStatisticsDAO.saveRepositoryContentStatistics( createStats( "internal", "2007/10/21 8:00:00",
           20000, 12000, 400 ) );
        assertNotNull( savedStats );

        String savedKeyId = JDOHelper.getObjectId( savedStats ).toString();
        assertEquals( "1[OID]org.apache.maven.archiva.model.RepositoryContentStatistics", savedKeyId );

        // query
        List results =
           repoContentStatisticsDAO.queryRepositoryContentStatistics( new RepositoryContentStatisticsByRepositoryConstraint(
                                                                                                                              "internal" ) );
        assertNotNull( results );
        assertEquals( 1, results.size() );

        RepositoryContentStatistics stats = (RepositoryContentStatistics) results.get( 0 );
        assertEquals( "internal", stats.getRepositoryId() );

        // delete
        repoContentStatisticsDAO.deleteRepositoryContentStatistics( stats );

        assertEquals( 0, repoContentStatisticsDAO.queryRepositoryContentStatistics(
            new RepositoryContentStatisticsByRepositoryConstraint( "internal" ) ).size() );
    }

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
}
