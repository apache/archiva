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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ArchivaAuditLogsConstraint
 */
public class ArchivaAuditLogsConstraint
    extends RangeConstraint
{
    private String whereClause;

    /**
     * Complete custom query!
     * 
     * @param desiredArtifact
     * @param desiredRepositoryId
     * @param desiredEvent
     * @param startDate
     * @param endDate
     */
    private void createWhereClause( String desiredArtifact, String desiredRepositoryId, String desiredEvent,
                                    Date startDate, Date endDate )
    {
        whereClause = "eventDate >= desiredStartDate && eventDate <= desiredEndDate";

        declImports = new String[] { "import java.util.Date" };

        List<String> declParamsList = new ArrayList<String>();
        declParamsList.add( "Date desiredStartDate" );
        declParamsList.add( "Date desiredEndDate" );

        List<Object> paramsList = new ArrayList<Object>();
        paramsList.add( startDate );
        paramsList.add( endDate );

        if ( desiredArtifact != null && !"".equals( desiredArtifact ) )
        {
            whereClause = whereClause + " && artifact.like(desiredArtifact)";
            declParamsList.add( "String desiredArtifact" );
            paramsList.add( desiredArtifact + "%" );
        }

        if ( desiredRepositoryId != null && !"".equals( desiredRepositoryId ) )
        {
            whereClause = whereClause + " && repositoryId == desiredRepositoryId";
            declParamsList.add( "String desiredRepositoryId" );
            paramsList.add( desiredRepositoryId );
        }

        if ( desiredEvent != null && !"".equals( desiredEvent ) )
        {
            whereClause = whereClause + " && event ==  desiredEvent";
            declParamsList.add( "String desiredEvent" );
            paramsList.add( desiredEvent );
        }

        int size = declParamsList.size();
        int i = 0;
        declParams = new String[size];       
        
        while( i < size )
        {
            declParams[i] = declParamsList.get( i );
            i++;
        }        
        
        params = paramsList.toArray();
    }

    public ArchivaAuditLogsConstraint( int[] range, String desiredArtifact, String desiredRepositoryId,
                                       String desiredEvent, Date startDate, Date endDate )
    {
        super( range );
        createWhereClause( desiredArtifact, desiredRepositoryId, desiredEvent, startDate, endDate );
    }
    
    public ArchivaAuditLogsConstraint( String desiredArtifact, String desiredRepositoryId,
                                       String desiredEvent, Date startDate, Date endDate )
    {
        super();
        createWhereClause( desiredArtifact, desiredRepositoryId, desiredEvent, startDate, endDate );
    }


    public String getSortColumn()
    {
        return "eventDate";
    }

    public String getWhereCondition()
    {
        return whereClause;
    }
}
