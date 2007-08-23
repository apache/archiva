package org.apache.maven.archiva.web.action.admin.repositories;

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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.RepositoryContentStatistics;

/**
 * AdminRepositoryConfiguration
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @todo! split from remote repo (which shouldn't need stats, use native class)
 */
public class AdminRepositoryConfiguration
    extends ManagedRepositoryConfiguration
{
    private RepositoryContentStatistics stats;

    public AdminRepositoryConfiguration()
    {
    }

    /**
     * Copy Constructor.
     */
    public AdminRepositoryConfiguration( ManagedRepositoryConfiguration repoconfig )
    {
        this.setId( repoconfig.getId() );
        this.setName( repoconfig.getName() );
        this.setLocation( repoconfig.getLocation() );
        this.setLayout( repoconfig.getLayout() );
        this.setIndexed( repoconfig.isIndexed() );
        this.setReleases( repoconfig.isReleases() );
        this.setSnapshots( repoconfig.isSnapshots() );

        this.setIndexDir( repoconfig.getIndexDir() );
        this.setRefreshCronExpression( repoconfig.getRefreshCronExpression() );

        this.setDaysOlder( repoconfig.getDaysOlder() );
        this.setRetentionCount( repoconfig.getRetentionCount() );
        this.setDeleteReleasedSnapshots( repoconfig.isDeleteReleasedSnapshots() );
    }

    public RepositoryContentStatistics getStats()
    {
        return stats;
    }

    public void setStats( RepositoryContentStatistics stats )
    {
        this.stats = stats;
    }
}
