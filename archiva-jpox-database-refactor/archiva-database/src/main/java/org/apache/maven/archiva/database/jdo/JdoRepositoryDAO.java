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
import org.apache.maven.archiva.database.RepositoryDAO;
import org.apache.maven.archiva.model.ArchivaRepositoryModel;

import java.util.List;

/**
 * JdoRepositoryDAO 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="jdo"
 */
public class JdoRepositoryDAO
    implements RepositoryDAO
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private JdoAccess jdo;
    
    /* .\ Archiva Repository \.____________________________________________________________ */

    public ArchivaRepositoryModel createRepository( String id, String url )
    {
        ArchivaRepositoryModel repo;

        try
        {
            repo = getRepository( id );
        }
        catch ( ArchivaDatabaseException e )
        {
            repo = new ArchivaRepositoryModel();
            repo.setId( id );
            repo.setUrl( url );
        }

        return repo;
    }

    public List getRepositories()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.getAllObjects( ArchivaRepositoryModel.class );
    }

    public ArchivaRepositoryModel getRepository( String id )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return (ArchivaRepositoryModel) jdo.getObjectById( ArchivaRepositoryModel.class, id, null );
    }

    public List queryRepository( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return jdo.getAllObjects( ArchivaRepositoryModel.class, constraint );
    }

    public ArchivaRepositoryModel saveRepository( ArchivaRepositoryModel repository )
    {
        return (ArchivaRepositoryModel) jdo.saveObject( repository );
    }

    public void deleteRepository( ArchivaRepositoryModel repository )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( repository );
    }
}
