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

import org.apache.commons.collections.Transformer;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.constraints.MostRecentRepositoryScanStatistics;
import org.apache.maven.archiva.model.RepositoryContentStatistics;

import java.util.List;

/**
 * RepositoryConfigurationAdminTransformer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.commons.collections.Transformer"
 *                   role-hint="adminrepoconfig"
 */
public class RepositoryConfigurationAdminTransformer
    implements Transformer
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    public Object transform( Object input )
    {
        if ( input instanceof RepositoryConfiguration )
        {
            RepositoryConfiguration repoconfig = (RepositoryConfiguration) input;
            AdminRepositoryConfiguration arepo = new AdminRepositoryConfiguration( repoconfig );

            if ( arepo.isManaged() )
            {
                arepo.setStats( getLatestStats( arepo.getId() ) );
            }

            return arepo;
        }

        return null;
    }

    private RepositoryContentStatistics getLatestStats( String repoId )
    {
        List results = dao.query( new MostRecentRepositoryScanStatistics( repoId ) );
        if ( results.isEmpty() )
        {
            return null;
        }

        return (RepositoryContentStatistics) results.get( 0 );
    }
}
