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

import java.util.Date;

import org.apache.maven.archiva.database.Constraint;

/**
 * ArtifactsProcessedConstraint 
 *
 * @version $Id$
 */
public class ArtifactsProcessedConstraint
    extends AbstractDeclarativeConstraint
    implements Constraint
{
    private String whereClause;

    /**
     * A Constraint showing artifacts processed since date provided.
     * @param since
     */
    public ArtifactsProcessedConstraint( Date since )
    {
        whereClause = "whenProcessed > since";
        declImports = new String[] { "import java.util.Date" };
        declParams = new String[] { "Date since" };
        params = new Object[] { since };
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
