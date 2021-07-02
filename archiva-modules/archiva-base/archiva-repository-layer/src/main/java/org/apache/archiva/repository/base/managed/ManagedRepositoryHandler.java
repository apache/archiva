package org.apache.archiva.repository.base.managed;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.validation.RepositoryChecker;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationResponse;

import javax.inject.Named;
import java.util.Collection;
import java.util.Map;

/**
 * Handler implementation for managed repositories.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ManagedRepositoryHandler
implements RepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration>
{

    public ManagedRepositoryHandler( ArchivaRepositoryRegistry repositoryRegistry,
                                     ConfigurationHandler configurationHandler,
                                     @Named( "repositoryValidator#common#managed") RepositoryValidator<ManagedRepository> managedRepositoryValidator )
    {
    }

    @Override
    public Map<String, ManagedRepository> newInstancesFromConfig( )
    {
        return null;
    }

    @Override
    public ManagedRepository newInstance( RepositoryType type, String id ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository newInstance( ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository put( ManagedRepository repository ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository put( ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository put( ManagedRepositoryConfiguration repositoryConfiguration, Configuration configuration ) throws RepositoryException
    {
        return null;
    }

    @Override
    public <D> CheckedResult<ManagedRepository, D> putWithCheck( ManagedRepositoryConfiguration repositoryConfiguration, RepositoryChecker<ManagedRepository, D> checker ) throws RepositoryException
    {
        return null;
    }

    @Override
    public void remove( String id ) throws RepositoryException
    {

    }

    @Override
    public void remove( String id, Configuration configuration ) throws RepositoryException
    {

    }

    @Override
    public ManagedRepository get( String id )
    {
        return null;
    }

    @Override
    public ManagedRepository clone( ManagedRepository repo ) throws RepositoryException
    {
        return null;
    }

    @Override
    public void updateReferences( ManagedRepository repo, ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {

    }

    @Override
    public Collection<ManagedRepository> getAll( )
    {
        return null;
    }

    @Override
    public RepositoryValidator<ManagedRepository> getValidator( )
    {
        return null;
    }

    @Override
    public ValidationResponse<ManagedRepository> validateRepository( ManagedRepository repository )
    {
        return null;
    }

    @Override
    public ValidationResponse<ManagedRepository> validateRepositoryForUpdate( ManagedRepository repository )
    {
        return null;
    }

    @Override
    public boolean has( String id )
    {
        return false;
    }

    @Override
    public void init( )
    {

    }

    @Override
    public void close( )
    {

    }
}
