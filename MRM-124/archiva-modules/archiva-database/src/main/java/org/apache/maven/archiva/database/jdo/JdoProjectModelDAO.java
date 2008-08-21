package org.apache.maven.archiva.database.jdo;

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

import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.jpox.ArchivaProjectModelKey;

import java.util.List;

/**
 * JdoProjectModelDAO 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="jdo"
 */
public class JdoProjectModelDAO
    implements ProjectModelDAO
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private JdoAccess jdo;

    public ArchivaProjectModel createProjectModel( String groupId, String artifactId, String version )
    {
        ArchivaProjectModel model;

        try
        {
            model = getProjectModel( groupId, artifactId, version );
        }
        catch ( ArchivaDatabaseException e )
        {
            model = new ArchivaProjectModel();
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setVersion( version );
        }

        return model;
    }

    public ArchivaProjectModel getProjectModel( String groupId, String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaProjectModelKey key = new ArchivaProjectModelKey();
        key.groupId = groupId;
        key.artifactId = artifactId;
        key.version = version;

        return (ArchivaProjectModel) jdo.getObjectById( ArchivaProjectModel.class, key, null );
    }

    public List<ArchivaProjectModel> queryProjectModels( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.queryObjects( ArchivaProjectModel.class, constraint );
    }

    public ArchivaProjectModel saveProjectModel( ArchivaProjectModel model )
        throws ArchivaDatabaseException
    {
        return (ArchivaProjectModel) jdo.saveObject( model );
    }

    public void deleteProjectModel( ArchivaProjectModel model )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( model );
    }
}
