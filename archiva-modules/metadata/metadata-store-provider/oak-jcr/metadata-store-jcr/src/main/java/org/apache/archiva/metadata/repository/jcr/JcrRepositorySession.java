package org.apache.archiva.metadata.repository.jcr;

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

import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.MetadataSessionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 * Session implementation for a JCR repository.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class JcrRepositorySession extends RepositorySession implements AutoCloseable
{

    private static final Logger log = LoggerFactory.getLogger( JcrRepositorySession.class );

    private Session jcrSession;
    private JcrMetadataRepository repository;

    public JcrRepositorySession( JcrMetadataRepository metadataRepository, MetadataResolver resolver) throws RepositoryException
    {
        super( metadataRepository, resolver );
        this.repository = metadataRepository;
        this.jcrSession = metadataRepository.login();
    }

    public Session getJcrSession() {
        return jcrSession;
    }

    public JcrMetadataRepository getJcrRepository() {
        return repository;
    }

    @Override
    public void close( )
    {
        super.close( );
        jcrSession.logout();
    }

    @Override
    protected boolean isDirty( )
    {
        if (super.isDirty()) {
            return true;
        }
        try
        {
            return jcrSession.hasPendingChanges( );
        }
        catch ( RepositoryException e )
        {
            log.error( "Could not check pending changes {}", e.getMessage( ) );
            return true;
        }
    }

    @Override
    public void save( ) throws MetadataSessionException
    {
        super.save( );
        try
        {
            jcrSession.save();
        }
        catch ( RepositoryException e )
        {
            throw new MetadataSessionException( e.getMessage( ), e );
        }
    }

    @Override
    public void revert( ) throws MetadataSessionException
    {
        super.revert( );
        try
        {
            jcrSession.refresh( false );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataSessionException( e.getMessage( ), e );
        }
    }

    @Override
    public void refresh() throws MetadataSessionException {
        try {
            jcrSession.refresh(true);
        } catch (RepositoryException e) {
            throw new MetadataSessionException(e.getMessage(), e);
        }
    }

    @Override
    public void refreshAndDiscard() throws MetadataSessionException {
        try {
            jcrSession.refresh(false);
        } catch (RepositoryException e) {
            throw new MetadataSessionException(e.getMessage(), e);
        }
    }
}
