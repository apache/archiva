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

import java.util.List;

/**
 * JdoProjectModelDAO 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
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
        
        return null;
    }

    public ArchivaProjectModel getProjectModel( String groupId, String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        
        return null;
    }

    public List queryProjectModel( Constraint constraint )
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

    public void deleteProjectModel( ArchivaProjectModel model )
        throws ArchivaDatabaseException
    {
        // TODO Auto-generated method stub

    }

}
