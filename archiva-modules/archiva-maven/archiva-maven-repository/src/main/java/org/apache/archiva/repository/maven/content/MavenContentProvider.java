package org.apache.archiva.repository.maven.content;

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

import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryContent;
import org.apache.archiva.repository.RepositoryContentProvider;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryType;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maven implementation of the repository content provider. Only default layout and
 * maven repository types are supported.
 */
@Service("repositoryContentProvider#maven")
public class MavenContentProvider implements RepositoryContentProvider
{

    @Inject
    @Named( "fileTypes" )
    private FileTypes filetypes;

    @Inject
    private FileLockManager fileLockManager;

    @Inject
    protected List<? extends ArtifactMappingProvider> artifactMappingProviders;

    @Inject
    @Named("MavenContentHelper")
    MavenContentHelper mavenContentHelper;

    private static final Set<RepositoryType> REPOSITORY_TYPES = new HashSet<>(  );
    static {
        REPOSITORY_TYPES.add(RepositoryType.MAVEN);
    }

    @Override
    public boolean supportsLayout( String layout )
    {
        return "default".equals( layout );
    }

    @Override
    public Set<RepositoryType> getSupportedRepositoryTypes( )
    {
        return REPOSITORY_TYPES;
    }

    @Override
    public boolean supports( RepositoryType type )
    {
        return type.equals( RepositoryType.MAVEN );
    }

    @Override
    public RemoteRepositoryContent createRemoteContent( RemoteRepository repository ) throws RepositoryException
    {
        if (!supports( repository.getType() )) {
            throw new RepositoryException( "Repository type "+repository.getType()+" is not supported by this implementation." );
        }
        if (!supportsLayout( repository.getLayout() )) {
            throw new RepositoryException( "Repository layout "+repository.getLayout()+" is not supported by this implementation." );
        }
        RemoteDefaultRepositoryContent content = new RemoteDefaultRepositoryContent(artifactMappingProviders);
        content.setRepository( repository );
        return content;
    }

    @Override
    public ManagedRepositoryContent createManagedContent( ManagedRepository repository ) throws RepositoryException
    {
        if (!supports( repository.getType() )) {
            throw new RepositoryException( "Repository type "+repository.getType()+" is not supported by this implementation." );
        }
        if (!supportsLayout( repository.getLayout() )) {
            throw new RepositoryException( "Repository layout "+repository.getLayout()+" is not supported by this implementation." );
        }
        ManagedDefaultRepositoryContent content = new ManagedDefaultRepositoryContent(repository, artifactMappingProviders, filetypes ,fileLockManager);
        content.setMavenContentHelper( mavenContentHelper );
        return content;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T extends RepositoryContent, V extends Repository> T createContent( Class<T> clazz, V repository ) throws RepositoryException
    {
        if (!supports( repository.getType() )) {
            throw new RepositoryException( "Repository type "+repository.getType()+" is not supported by this implementation." );
        }
        if (repository instanceof ManagedRepository && ManagedRepositoryContent.class.isAssignableFrom( clazz ) ) {
            return (T) this.createManagedContent( (ManagedRepository) repository );
        } else if (repository instanceof RemoteRepository && RemoteRepository.class.isAssignableFrom( clazz )) {
            return (T) this.createRemoteContent( (RemoteRepository) repository );
        } else {
            throw new RepositoryException( "Repository flavour is not supported: "+repository.getClass().getName() );
        }
    }

}
