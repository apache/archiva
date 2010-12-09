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
import org.apache.maven.archiva.model.ArchivaArtifactModel;

import java.util.List;

/**
 * Obtain a set of unique ArtifactIds for the specified groupId.
 *
 * @version $Id$
 */
public class UniqueArtifactIdConstraint
    extends AbstractSimpleConstraint
    implements Constraint
{
    private StringBuffer sql = new StringBuffer();

    private StringBuffer countSql = new StringBuffer();

    private Class<?> resultClass;
    
    /**
     * Obtain a set of unique ArtifactIds for the specified groupId.
     * 
     * @param groupId the groupId to search for artifactIds within.
     */
    public UniqueArtifactIdConstraint( List<String> selectedRepositoryIds, String groupId )
    {
        appendSelect( sql, false );
        sql.append( " WHERE " );
        SqlBuilder.appendWhereSelectedRepositories( sql, "repositoryId", selectedRepositoryIds );
        sql.append( " && " );
        appendWhereSelectedGroupId( sql );
        appendGroupBy( sql );

        countSql.append( "SELECT count(artifactId) FROM " ).append( ArchivaArtifactModel.class.getName() );
        countSql.append( " WHERE " );
        SqlBuilder.appendWhereSelectedRepositories( countSql, "repositoryId", selectedRepositoryIds );
        countSql.append( " && " );
        appendWhereSelectedGroupId( countSql );
        appendGroupBy( countSql );

        super.params = new Object[] { groupId };
    }

    /**
     * Obtain a set of unique ArtifactIds for the specified groupId.
     * 
     * @param groupId the groupId to search for artifactIds within.
     */
    public UniqueArtifactIdConstraint( String groupId )
    {
        appendSelect( sql, false );
        sql.append( " WHERE " );
        appendWhereSelectedGroupId( sql );
        appendGroupBy( sql );

        countSql.append( "SELECT count(artifactId) FROM " ).append( ArchivaArtifactModel.class.getName() );
        countSql.append( " WHERE " );
        appendWhereSelectedGroupId( countSql );
        appendGroupBy( countSql );

        super.params = new Object[] { groupId };
    }
    
    /**
     * Obtain a set of unique artifactIds with respect to their groups from the specified repository.
     * 
     * @param repoId
     * @param isUnique
     */
    public UniqueArtifactIdConstraint( String repoId, boolean isUnique )
    {
        appendSelect( sql, isUnique );
        sql.append( " WHERE repositoryId == \"" + repoId + "\"" );

        if( isUnique )
        {
            countSql.append( "SELECT count(this) FROM " ).append( ArchivaArtifactModel.class.getName() );
            countSql.append( " WHERE repositoryId == \"" ).append( repoId ).append( "\"" );
            countSql.append( " GROUP BY groupId, artifactId" );
        }
        else
        {
            countSql.append( "SELECT count(artifactId) FROM " ).append( ArchivaArtifactModel.class.getName() );
            countSql.append( " WHERE repositoryId == \"" ).append( repoId ).append( "\"" );
        }

        resultClass = Object[].class;
    }

    @SuppressWarnings("unchecked")
    public Class getResultClass()
    {
        if( resultClass != null )
        {
            return resultClass;
        }
        
        return String.class;
    }

    public String getSelectSql()
    {
        return sql.toString();
    }

    @Override
    public String getCountSql()
    {
        return countSql.toString();
    }

    private void appendGroupBy( StringBuffer buf )
    {
        buf.append( " GROUP BY artifactId ORDER BY artifactId ASCENDING" );
    }

    private void appendSelect( StringBuffer buf, boolean isUnique )
    {
        if( isUnique )
        {
            buf.append( "SELECT DISTINCT groupId, artifactId FROM " ).append( ArchivaArtifactModel.class.getName() );
        }
        else
        {
            buf.append( "SELECT artifactId FROM " ).append( ArchivaArtifactModel.class.getName() );
        }
    }

    private void appendWhereSelectedGroupId( StringBuffer buf )
    {
        buf.append( " groupId == selectedGroupId PARAMETERS String selectedGroupId" );
    }

}
