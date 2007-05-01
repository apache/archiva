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

/**
 * UniqueGroupIdConstraint 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class UniqueGroupIdConstraint
    extends AbstractSimpleConstraint
    implements Constraint
{
    private String sql;

    public UniqueGroupIdConstraint()
    {
        /* this assumes search for no groupId prefix */
        sql = "SELECT groupId FROM " + ArchivaArtifactModel.class.getName()
            + " GROUP BY groupId ORDER BY groupId ASCENDING";
    }

    public UniqueGroupIdConstraint( String groupIdPrefix )
    {
        sql = "SELECT groupId FROM " + ArchivaArtifactModel.class.getName()
            + " WHERE groupId.startsWith(groupIdPrefix) PARAMETERS String groupIdPrefix"
            + " GROUP BY groupId ORDER BY groupId ASCENDING";

        super.params = new Object[] { groupIdPrefix };
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
