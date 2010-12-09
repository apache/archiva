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
 * UniqueGroupIdConstraint 
 *
 * @version $Id$
 */
public class UniqueGroupIdConstraint
    extends AbstractSimpleConstraint
    implements Constraint
{
    private StringBuffer sql = new StringBuffer();

    public UniqueGroupIdConstraint()
    {
        /* this assumes search for no groupId prefix */
        appendGroupBy( sql );
    }

    public UniqueGroupIdConstraint( List<String> selectedRepositories )
    {
        sql.append( " WHERE " );
        SqlBuilder.appendWhereSelectedRepositories( sql, "repositoryId", selectedRepositories );
        appendGroupBy( sql );
    }

    public UniqueGroupIdConstraint( List<String> selectedRepositories, String groupIdPrefix )
    {
        sql.append( " WHERE " );
        SqlBuilder.appendWhereSelectedRepositories( sql, "repositoryId", selectedRepositories );
        sql.append( " && " );
        appendWhereGroupIdStartsWith( sql );
        appendGroupBy( sql );

        super.params = new Object[] { groupIdPrefix };
    }

    public UniqueGroupIdConstraint( String groupIdPrefix )
    {
        sql.append( " WHERE " );
        appendWhereGroupIdStartsWith( sql );
        appendGroupBy( sql );

        super.params = new Object[] { groupIdPrefix };
    }

    @SuppressWarnings("unchecked")
    public Class getResultClass()
    {
        return String.class;
    }

    public String getSelectSql()
    {
        StringBuffer buf = new StringBuffer();
        appendSelect( buf );
        buf.append( sql );
        return buf.toString();
    }

    @Override
    public String getCountSql()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "SELECT count(groupId) FROM " ).append( ArchivaArtifactModel.class.getName() );
        buf.append( sql );
        return buf.toString();
    }

    private void appendGroupBy( StringBuffer buf )
    {
        buf.append( " GROUP BY groupId ORDER BY groupId ASCENDING" );
    }

    private void appendSelect( StringBuffer buf )
    {
        buf.append( "SELECT groupId FROM " ).append( ArchivaArtifactModel.class.getName() );
    }

    private void appendWhereGroupIdStartsWith( StringBuffer buf )
    {
        buf.append( " groupId.startsWith(groupIdPrefix) PARAMETERS String groupIdPrefix" );
    }
}
