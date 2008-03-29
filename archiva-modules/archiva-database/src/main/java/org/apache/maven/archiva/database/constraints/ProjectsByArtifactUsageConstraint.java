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

import org.apache.maven.archiva.database.DeclarativeConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.Dependency;

/**
 * ProjectsByArtifactUsageConstraint 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectsByArtifactUsageConstraint
    extends AbstractDeclarativeConstraint
    implements DeclarativeConstraint
{
    private String filter;
    
    public ProjectsByArtifactUsageConstraint( ArchivaArtifact artifact )
    {
        this( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion() );
    }
    
    public ProjectsByArtifactUsageConstraint( String groupId, String artifactId, String version )
    {
        super.declImports = new String[] {
            "import " + Dependency.class.getName()
        };
        
        super.variables = new String[] {
            "Dependency dep"
        };
        
        super.declParams = new String[] {
            "String selectedGroupId",
            "String selectedArtifactId",
            "String selectedVersion"
        };
        
        filter = "dependencies.contains( dep ) && " +
                 "dep.groupId == selectedGroupId && " +
                 "dep.artifactId == selectedArtifactId && " +
                 "dep.version == selectedVersion";
   
        super.params = new Object[] { groupId, artifactId, version };
    }

    public String getSortColumn()
    {
        return "groupId";
    }

    public String getWhereCondition()
    {
        return null;
    }
    
    public String getFilter()
    {
        return filter;
    }
}
