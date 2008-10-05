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
 * ArtifactVersionsConstraint
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class ArtifactVersionsConstraint
    extends AbstractDeclarativeConstraint
    implements Constraint
{
    private String whereClause = "";
    
    private String sortColumn = "repositoryId";
    
    public ArtifactVersionsConstraint( String repoId, String groupId, String artifactId )
    {        
        if( repoId != null )
        {   
            whereClause = "repositoryId.equals(selectedRepoId) && groupId.equals(selectedGroupId) && artifactId.equals(selectedArtifactId) " +
            		"&& whenGathered != null";
            declParams = new String[] { "String selectedRepoId", "String selectedGroupId", "String selectedArtifactId" };
            params = new Object[] { repoId, groupId, artifactId };
        }
        else
        {
            whereClause = "groupId.equals(selectedGroupId) && artifactId.equals(selectedArtifactId) && this.whenGathered != null";            
            declParams = new String[] { "String selectedGroupId", "String selectedArtifactId" };
            params = new Object[] { groupId, artifactId };
        }
    }
    
    public ArtifactVersionsConstraint( String repoId, String groupId, String artifactId, String sortColumn )
    {   
        this( repoId, groupId, artifactId );
        this.sortColumn = sortColumn;        
    }
        
    public String getSortColumn()
    {        
        return sortColumn;
    }

    public String getWhereCondition()
    {        
        return whereClause;
    }

}
