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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.model.RepositoryContentStatistics;

/**
 * JdoRepositoryContentStatisticsDAO 
 *
 * @version
 * 
 * @plexus.component role-hint="jdo"
 */
public class JdoRepositoryContentStatisticsDAO
    implements RepositoryContentStatisticsDAO
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private JdoAccess jdo;
    
    public void deleteRepositoryContentStatistics( RepositoryContentStatistics stats )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( stats );
    }

    public List queryRepositoryContentStatistics( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        List results = jdo.queryObjects( RepositoryContentStatistics.class, constraint );
        
        return results;
    }

    public RepositoryContentStatistics saveRepositoryContentStatistics( RepositoryContentStatistics stats )
    {
        RepositoryContentStatistics savedStats = (RepositoryContentStatistics) jdo.saveObject( stats );
        
        return savedStats;
    }

}
