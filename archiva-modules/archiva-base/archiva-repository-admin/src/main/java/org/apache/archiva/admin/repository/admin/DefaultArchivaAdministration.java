package org.apache.archiva.admin.repository.admin;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.maven.archiva.configuration.Configuration;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service("archivaAdministration#default")
public class DefaultArchivaAdministration
    extends AbstractRepositoryAdmin
    implements ArchivaAdministration
{
    public List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws RepositoryAdminException
    {
        List<LegacyArtifactPath> legacyArtifactPaths = new ArrayList<LegacyArtifactPath>();
        for ( org.apache.maven.archiva.configuration.LegacyArtifactPath legacyArtifactPath : getArchivaConfiguration().getConfiguration().getLegacyArtifactPaths() )
        {
            legacyArtifactPaths.add(
                new BeanReplicator().replicateBean( legacyArtifactPath, LegacyArtifactPath.class ) );
        }
        return legacyArtifactPaths;
    }

    public void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        configuration.addLegacyArtifactPath( new BeanReplicator().replicateBean( legacyArtifactPath,
                                                                                 org.apache.maven.archiva.configuration.LegacyArtifactPath.class ) );

        saveConfiguration( configuration );
    }

    public void deleteLegacyArtifactPath( String path )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        org.apache.maven.archiva.configuration.LegacyArtifactPath legacyArtifactPath =
            new org.apache.maven.archiva.configuration.LegacyArtifactPath();

        legacyArtifactPath.setPath( path );
        configuration.removeLegacyArtifactPath( legacyArtifactPath );

        saveConfiguration( configuration );
    }
}
