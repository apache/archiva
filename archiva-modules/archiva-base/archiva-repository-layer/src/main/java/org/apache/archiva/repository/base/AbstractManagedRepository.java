package org.apache.archiva.repository.base;

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


import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.storage.RepositoryStorage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Simple implementation of a managed repository.
 */
public abstract class AbstractManagedRepository extends AbstractRepository implements EditableManagedRepository
{
    private boolean blocksRedeployment = false;
    private ManagedRepositoryContent content;
    private Set<ReleaseScheme> activeReleaseSchemes = new HashSet<>(  );
    private Set<ReleaseScheme> uActiveReleaseSchemes = Collections.unmodifiableSet( activeReleaseSchemes );

    public AbstractManagedRepository( RepositoryType type, String id, String name, RepositoryStorage storage)
    {
        super( type, id, name, storage );
    }

    public AbstractManagedRepository( Locale primaryLocale, RepositoryType type, String id, String name, RepositoryStorage storage )
    {
        super( primaryLocale, type, id, name, storage );
    }

    @Override
    public ManagedRepositoryContent getContent( )
    {
        return content;
    }

    @Override
    public void setContent(ManagedRepositoryContent content) {
        this.content = content;
    }

    @Override
    public void setBlocksRedeployment( boolean blocksRedeployment )
    {
        this.blocksRedeployment = blocksRedeployment;
    }

    @Override
    public boolean blocksRedeployments( )
    {
        return blocksRedeployment;
    }

    @Override
    public Set<ReleaseScheme> getActiveReleaseSchemes( )
    {
        return uActiveReleaseSchemes;
    }

    @Override
    public void addActiveReleaseScheme( ReleaseScheme scheme )
    {
        this.activeReleaseSchemes.add(scheme);
    }

    @Override
    public void removeActiveReleaseScheme( ReleaseScheme scheme )
    {
        this.activeReleaseSchemes.remove(scheme);
    }

    @Override
    public void clearActiveReleaseSchemes( )
    {
        this.activeReleaseSchemes.clear();
    }


}
