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

import org.apache.maven.archiva.database.Constraint;

/**
 * RepositoryProblemByRepositoryIdConstraint
 */
public class RepositoryProblemByRepositoryIdConstraint
    extends RangeConstraint
    implements Constraint
{
    private String whereClause;

    private void createWhereClause( String desiredRepositoryId )
    {
        whereClause = "repositoryId == desiredRepositoryId";
        declParams = new String[]{"String desiredRepositoryId"};
        params = new Object[]{desiredRepositoryId};
    }

    public RepositoryProblemByRepositoryIdConstraint( String desiredRepositoryId )
    {
        super();
        createWhereClause( desiredRepositoryId );
    }

    public RepositoryProblemByRepositoryIdConstraint( int[] range, String desiredRepositoryId )
    {
        super( range );
        createWhereClause( desiredRepositoryId );
    }

    public String getSortColumn()
    {
        return "groupId";
    }

    public String getWhereCondition()
    {
        return whereClause;
    }
}
