package org.apache.maven.archiva.database.constraints;

import java.util.Date;

import org.apache.maven.archiva.database.Constraint;

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

/**
 * RepositoryContentStatisticsByRepositoryConstraint
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class RepositoryContentStatisticsByRepositoryConstraint
    extends AbstractDeclarativeConstraint
{
    private String whereClause;

    public RepositoryContentStatisticsByRepositoryConstraint( String repoId )
    {               
        whereClause = "repositoryId == repoId";
        declParams = new String[] { "String repoId" };
        params = new Object[] { repoId };
    }
    
    public RepositoryContentStatisticsByRepositoryConstraint( String repoId, Date startDate, Date endDate )
    {    
        declImports = new String[] { "import java.util.Date" };
        whereClause = "repositoryId == repoId && whenGathered >= startDate && whenGathered <= endDate";
        declParams = new String[] { "String repoId", "Date startDate", "Date endDate" };
        params = new Object[] { repoId, startDate, endDate };
        
        sortDirection = Constraint.DESCENDING;
    }

    public String getSortColumn()
    {
        return "whenGathered";
    }
    
    public String getWhereCondition()
    {
        return whereClause;
    }
}
