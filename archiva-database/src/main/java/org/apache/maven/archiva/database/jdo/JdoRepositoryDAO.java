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
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArchivaRepositoryModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * JdoRepositoryDAO 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
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

    public ArchivaRepository createRepository( String id, String name, String url )
    {
        ArchivaRepository repo;

        try
        {
            repo = getRepository( id );
        }
        catch ( ArchivaDatabaseException e )
        {
            repo = new ArchivaRepository( id, name, url );
        }

        return repo;
    }

    public List getRepositories()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        return queryRepositories( null );
    }

    public ArchivaRepository getRepository( String id )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaRepositoryModel model = (ArchivaRepositoryModel) jdo.getObjectById( ArchivaRepositoryModel.class, id,
                                                                                   null );
        return new ArchivaRepository( model );
    }

    public List queryRepositories( Constraint constraint )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        List results = jdo.queryObjects( ArchivaRepositoryModel.class, constraint );

        if ( ( results == null ) || results.isEmpty() )
        {
            return results;
        }

        List ret = new ArrayList();
        Iterator it = results.iterator();
        while ( it.hasNext() )
        {
            ArchivaRepositoryModel model = (ArchivaRepositoryModel) it.next();
            ret.add( new ArchivaRepository( model ) );
        }

        return ret;
    }

    public ArchivaRepository saveRepository( ArchivaRepository repository )
    {
        ArchivaRepositoryModel model = (ArchivaRepositoryModel) jdo.saveObject( repository.getModel() );
        if ( model == null )
        {
            return null;
        }

        return new ArchivaRepository( model );
    }

    public void deleteRepository( ArchivaRepository repository )
        throws ArchivaDatabaseException
    {
        jdo.removeObject( repository.getModel() );
    }
}
