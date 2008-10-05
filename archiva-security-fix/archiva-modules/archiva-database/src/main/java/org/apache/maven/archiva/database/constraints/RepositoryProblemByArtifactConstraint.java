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
import org.apache.maven.archiva.model.ArchivaArtifact;

/**
 * RepositoryProblemByArtifactConstraint
 */
public class RepositoryProblemByArtifactConstraint
    extends AbstractDeclarativeConstraint
    implements Constraint
{
    private String whereClause;

    private void createWhereClause( ArchivaArtifact artifact )
    {
        whereClause =
            "groupId.like(desiredGroupId) && artifactId.like(desiredArtifactId) && version.like(desiredVersion)";
        declParams = new String[] { "String desiredGroupId" , "String desiredArtifactId" , "String desiredVersion"};
        params = new Object[] { artifact.getGroupId() + "%" , artifact.getArtifactId() + "%", artifact.getVersion() + "%"};
    }

    public RepositoryProblemByArtifactConstraint( ArchivaArtifact desiredArtifact )
    {
        super();
        createWhereClause( desiredArtifact );
    }

    public String getSortColumn()
    {
        return "artifactId";
    }

    public String getWhereCondition()
    {
        return whereClause;
    }
}
