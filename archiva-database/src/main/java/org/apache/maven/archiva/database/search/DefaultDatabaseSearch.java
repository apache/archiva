package org.apache.maven.archiva.database.search;

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

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.List;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @plexus.component role="org.apache.maven.archiva.database.search.DatabaseSearch" role-hint="default"
 */
public class DefaultDatabaseSearch
    extends AbstractLogEnabled
    implements DatabaseSearch
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    public List searchArtifactsByChecksum( String checksum )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        Constraint constraint = new ArtifactsByChecksumConstraint( checksum.toLowerCase().trim(), "" );
        List results = dao.getArtifactDAO().queryArtifacts( constraint );

        if ( results != null )
        {
            getLogger().info( "Number of database hits : " + results.size() );
        }

        return results;
    }

}
