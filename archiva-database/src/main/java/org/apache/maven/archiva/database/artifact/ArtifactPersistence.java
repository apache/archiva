package org.apache.maven.archiva.database.artifact;

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

import com.ibatis.sqlmap.client.SqlMapClient;

import org.apache.maven.archiva.database.AbstractIbatisStore;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.artifact.Artifact;

import java.sql.SQLException;

/**
 * ArtifactPersistence
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.artifact.ArtifactPersistence"
 */
public class ArtifactPersistence
    extends AbstractIbatisStore
{
    protected String[] getTableNames()
    {
        return new String[] { "ArtifactKeys" };
    }
    
    private ArtifactKey toKey(Artifact artifact)
    {
        ArtifactKey key = new ArtifactKey();
        key.setGroupId( artifact.getGroupId() );
        key.setArtifactId( artifact.getArtifactId() );
        key.set
        return key;
    }

    public void create( Artifact artifact ) throws ArchivaDatabaseException
    {
        SqlMapClient sqlMap = ibatisHelper.getSqlMapClient();

        try
        {
            sqlMap.startTransaction();

            getLogger().info( "Adding artifact." );
            sqlMap.update( "addArtifact", artifact );
            

            sqlMap.commitTransaction();
        }
        catch ( SQLException e )
        {
            getLogger().error( "Error while executing statement, showing all linked exceptions in SQLException." );

            while ( e != null )
            {
                getLogger().error( e.getMessage(), e );

                e = e.getNextException();
            }

            throw new ArchivaDatabaseException( "Error while executing statement.", e );
        }
        finally
        {
            try
            {
                sqlMap.endTransaction();
            }
            catch ( SQLException e )
            {
                e.printStackTrace();
            }
        }
    }

    public Artifact read( String groupId, String artifactId, String version )
    {
        return null;
    }

    public Artifact read( String groupId, String artifactId, String version, String type )
    {
        return null;
    }

    public Artifact read( String groupId, String artifactId, String version, String classifier, String type )
    {
        return null;
    }

    public void update( Artifact artifact )
    {

    }

    public void delete( Artifact artifact )
    {

    }

    public void delete( String groupId, String artifactId, String version )
    {

    }

    public void delete( String groupId, String artifactId, String version, String type )
    {

    }

    public void delete( String groupId, String artifactId, String version, String classifier, String type )
    {

    }

}
