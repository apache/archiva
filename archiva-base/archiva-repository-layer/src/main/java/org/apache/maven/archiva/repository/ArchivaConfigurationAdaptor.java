package org.apache.maven.archiva.repository;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;

/**
 * ArchivaConfigurationAdaptor
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @todo the whole need for 2 objects is a consequence of using jpox. hopefully JPA will address some of this mess.
 */
public class ArchivaConfigurationAdaptor
{
    private ArchivaConfigurationAdaptor()
    {
    }

    public static ArchivaRepository toArchivaRepository( ManagedRepositoryConfiguration config )
    {
        if ( config == null )
        {
            throw new IllegalArgumentException( "Unable to convert null repository config to archiva repository." );
        }

        if ( StringUtils.isBlank( config.getId() ) )
        {
            throw new IllegalArgumentException( "Unable to repository config with blank ID to archiva repository." );
        }

        if ( StringUtils.isBlank( config.getLocation() ) )
        {
            throw new IllegalArgumentException(
                "Unable to convert repository config with blank location to archiva repository." );
        }

        ArchivaRepository repository =
            new ArchivaRepository( config.getId(), config.getName(), PathUtil.toUrl( config.getLocation() ) );

        repository.getModel().setLayoutName( config.getLayout() );
        repository.getModel().setReleasePolicy( config.isReleases() );
        repository.getModel().setSnapshotPolicy( config.isSnapshots() );

        return repository;
    }
}
