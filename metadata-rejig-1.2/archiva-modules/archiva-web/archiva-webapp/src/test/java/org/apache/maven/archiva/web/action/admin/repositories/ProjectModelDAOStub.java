package org.apache.maven.archiva.web.action.admin.repositories;

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

import java.util.List;

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.model.ArchivaProjectModel;

/**
 * ProjectModelDAOStub
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class ProjectModelDAOStub
    implements ProjectModelDAO
{

    public ArchivaProjectModel createProjectModel( String groupId, String artifactId, String version )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void deleteProjectModel( ArchivaProjectModel model )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub

    }

    public ArchivaProjectModel getProjectModel( String groupId, String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaProjectModel projectModel = new ArchivaProjectModel();
        projectModel.setGroupId( groupId );
        projectModel.setArtifactId( artifactId );
        projectModel.setVersion( version );

        return projectModel;
    }

    public List queryProjectModels( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ArchivaProjectModel saveProjectModel( ArchivaProjectModel model )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub
        return null;
    }

}
