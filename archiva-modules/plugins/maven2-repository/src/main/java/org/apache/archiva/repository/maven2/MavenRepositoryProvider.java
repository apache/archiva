package org.apache.archiva.repository.maven2;

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

import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * Provider for the maven2 repository implementations
 */
public class MavenRepositoryProvider implements RepositoryProvider
{
    private static final Logger log = LoggerFactory.getLogger( MavenRepositoryProvider.class );

    static final Set<RepositoryType> TYPES = new HashSet<>(  );
    static {
        TYPES.add( RepositoryType.MAVEN);
    }

    @Override
    public Set<RepositoryType> provides( )
    {
        return TYPES;
    }

    @Override
    public ManagedRepository createManagedInstance( ManagedRepositoryConfiguration cfg )
    {
        MavenManagedRepository repo = new MavenManagedRepository(cfg.getId() ,cfg.getName());
        try
        {
            if (cfg.getLocation().startsWith("file:")) {
                    repo.setLocation( new URI(cfg.getLocation()) );
            } else {
                repo.setLocation( new URI("file://"+cfg.getLocation()) );
            }
        }
        catch ( URISyntaxException e )
        {
            log.error("Could not set repository uri "+cfg.getLocation());
        }
        cfg.getRefreshCronExpression();


        StagingRepositoryFeature feature = repo.getFeature( StagingRepositoryFeature.class ).get();
        return null;
    }

    @Override
    public RemoteRepository createRemoteInstance( RemoteRepositoryConfiguration configuration )
    {
        return null;
    }
}
