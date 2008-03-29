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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.model.ArchivaArtifactModel;

import java.util.List;

/**
 * Obtain the list of version's for specific GroupId and ArtifactId.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class UniqueVersionConstraint
    extends AbstractSimpleConstraint
    implements Constraint
{
    private StringBuffer sql = new StringBuffer();

    /**
     * Obtain the list of version's for specific GroupId and ArtifactId.
     * 
     * @param selectedRepositoryIds the selected repository ids.
     * @param groupId the selected groupId.
     * @param artifactId the selected artifactId.
     */
    public UniqueVersionConstraint( List<String> selectedRepositoryIds, String groupId, String artifactId )
    {
        if ( StringUtils.isBlank( groupId ) )
        {
            throw new IllegalArgumentException( "A blank groupId is not allowed." );
        }

        if ( StringUtils.isBlank( artifactId ) )
        {
            throw new IllegalArgumentException( "A blank artifactId is not allowed." );
        }

        appendSelect( sql );
        sql.append( " WHERE " );
        SqlBuilder.appendWhereSelectedRepositories( sql, "repositoryId", selectedRepositoryIds );
        sql.append( " && " );
        appendWhereSelectedGroupIdArtifactId( sql );
        appendGroupBy( sql );

        super.params = new Object[] { groupId, artifactId };
    }

    /**
     * Obtain the list of version's for specific GroupId and ArtifactId.
     * 
     * @param groupId the selected groupId.
     * @param artifactId the selected artifactId.
     */
    public UniqueVersionConstraint( String groupId, String artifactId )
    {
        if ( StringUtils.isBlank( groupId ) )
        {
            throw new IllegalArgumentException( "A blank groupId is not allowed." );
        }

        if ( StringUtils.isBlank( artifactId ) )
        {
            throw new IllegalArgumentException( "A blank artifactId is not allowed." );
        }

        appendSelect( sql );
        sql.append( " WHERE " );
        appendWhereSelectedGroupIdArtifactId( sql );
        appendGroupBy( sql );

        super.params = new Object[] { groupId, artifactId };
    }

    @SuppressWarnings("unchecked")
    public Class getResultClass()
    {
        return String.class;
    }

    public String getSelectSql()
    {
        return sql.toString();
    }

    private void appendGroupBy( StringBuffer buf )
    {
        buf.append( " GROUP BY version ORDER BY version ASCENDING" );
    }

    private void appendSelect( StringBuffer buf )
    {
        buf.append( "SELECT version FROM " ).append( ArchivaArtifactModel.class.getName() );
    }

    private void appendWhereSelectedGroupIdArtifactId( StringBuffer buf )
    {
        buf.append( " groupId == selectedGroupId && artifactId == selectedArtifactId" );
        buf.append( " PARAMETERS String selectedGroupId, String selectedArtifactId" );
    }
}
