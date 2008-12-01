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

import java.util.List;

/**
 * SqlBuilder - common sql building mechanisms. 
 *
 * @version $Id$
 */
public class SqlBuilder
{
    /**
     * Append a sql specific where clause within <code>"()"</code> braces that selects the specific
     * repository ids provided. 
     * 
     * NOTE: This does not append the "WHERE" statement itself.
     * 
     * @param sql the sql buffer to append to.
     * @param fieldId the field id for the repository Id.
     * @param selectedRepositoryIds the list of repository ids to provide.
     */
    public static void appendWhereSelectedRepositories( StringBuffer sql, String fieldId,
                                                        List<String> selectedRepositoryIds )
    {
        if ( fieldId == null )
        {
            throw new NullPointerException( "Null field id is not allowed." );
        }

        if ( StringUtils.isBlank( fieldId ) )
        {
            throw new IllegalArgumentException( "Blank field id is not allowed." );
        }

        if ( selectedRepositoryIds == null )
        {
            throw new NullPointerException( "Selected repositories cannot be null." );
        }

        if ( selectedRepositoryIds.isEmpty() )
        {
            throw new IllegalArgumentException( "Selected repositories cannot be null." );
        }

        sql.append( " (" );
        boolean multiple = false;
        for ( String repo : selectedRepositoryIds )
        {
            if ( multiple )
            {
                sql.append( " || " );
            }
            sql.append( " " ).append( fieldId ).append( " == \"" ).append( repo ).append( "\"" );
            multiple = true;
        }
        sql.append( " )" );
    }
}
