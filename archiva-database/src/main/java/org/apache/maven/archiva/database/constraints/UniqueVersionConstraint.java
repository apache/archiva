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

/**
 * Obtain the list of version's for specific GroupId and ArtifactId.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class UniqueVersionConstraint
    extends AbstractSimpleConstraint
    implements Constraint
{
    private String sql;

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

        sql = "SELECT version FROM " + ArchivaArtifactModel.class.getName()
            + " WHERE groupId == selectedGroupId && artifactId == selectedArtifactId"
            + " PARAMETERS String selectedGroupId, String selectedArtifactId"
            + " GROUP BY version ORDER BY version ASCENDING";

        super.params = new Object[] { groupId, artifactId };
    }

    public Class getResultClass()
    {
        return String.class;
    }

    public String getSelectSql()
    {
        return sql;
    }
}
