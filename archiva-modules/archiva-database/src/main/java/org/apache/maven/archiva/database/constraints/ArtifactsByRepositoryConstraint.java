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
 * ArtifactsByRepositoryConstraint
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class ArtifactsByRepositoryConstraint 
	extends AbstractDeclarativeConstraint
	implements Constraint 
{
	private String whereClause;
	
	private String sortColumn = "groupId";
	
	public ArtifactsByRepositoryConstraint( String repoId )
	{
		whereClause = "repositoryId == repoId";        
        declParams = new String[] { "String repoId" };
        params = new Object[] { repoId };
	}
		
	public ArtifactsByRepositoryConstraint( String repoId, Date targetWhenGathered, String sortColumn )
    {
	    declImports = new String[] { "import java.util.Date" };
	    whereClause = "this.repositoryId == repoId && this.whenGathered >= targetWhenGathered";        
        declParams = new String[] { "String repoId", "Date targetWhenGathered" };
        params = new Object[] { repoId, targetWhenGathered };        
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
