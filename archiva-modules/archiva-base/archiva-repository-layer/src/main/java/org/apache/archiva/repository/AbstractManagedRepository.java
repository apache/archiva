package org.apache.archiva.repository;

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


import java.util.Locale;

/**
 * Simple implementation of a managed repository.
 */
public abstract class AbstractManagedRepository extends AbstractRepository implements EditableManagedRepository
{
    private boolean blocksRedeployment = false;
    private ManagedRepositoryContent content;

    public AbstractManagedRepository( RepositoryType type, String id, String name )
    {
        super( type, id, name );
    }

    public AbstractManagedRepository( Locale primaryLocale, RepositoryType type, String id, String name )
    {
        super( primaryLocale, type, id, name );
    }

    @Override
    public ManagedRepositoryContent getContent( )
    {
        return content;
    }

    protected void setContent(ManagedRepositoryContent content) {
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
}
