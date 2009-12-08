package org.apache.archiva.scheduler.repository;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;

public class TestRepositoryStatisticsManager
    implements RepositoryStatisticsManager
{
    private Map<String, List<RepositoryStatistics>> repoStats = new HashMap<String, List<RepositoryStatistics>>();

    public RepositoryStatistics getLastStatistics( String repositoryId )
    {
        List<RepositoryStatistics> repositoryStatisticsList = getStatsList( repositoryId );
        return !repositoryStatisticsList.isEmpty()
            ? repositoryStatisticsList.get( repositoryStatisticsList.size() - 1 )
            : null;
    }

    public void addStatisticsAfterScan( String repositoryId, RepositoryStatistics repositoryStatistics )
    {
        List<RepositoryStatistics> stats = getStatsList( repositoryId );
        stats.add( repositoryStatistics );
    }

    public void deleteStatistics( String repositoryId )
    {
        repoStats.remove( repositoryId );
    }

    public List<RepositoryStatistics> getStatisticsInRange( String repositoryId, Date startDate, Date endDate )
    {
        throw new UnsupportedOperationException();
    }

    private List<RepositoryStatistics> getStatsList( String repositoryId )
    {
        List<RepositoryStatistics> stats = repoStats.get( repositoryId );
        if ( stats == null )
        {
            stats = new ArrayList<RepositoryStatistics>();
            repoStats.put( repositoryId, stats );
        }
        return stats;
    }
}
