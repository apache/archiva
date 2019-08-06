package org.apache.archiva.metadata.repository;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The repository session provides a single interface to accessing Archiva repositories. It provides access to three
 * resources:
 * <ul>
 * <li>{@link MetadataRepository} - the metadata content repository for read/write access, in its current state (no
 * remote resources will be retrieved in the process</li>
 * <li>{@link MetadataResolver} - access to resolve metadata content, accommodating metadata not yet stored or up to
 * date in the content repository (i.e. virtualised repositories, remote proxied content, or metadata in a different
 * model format in the repository storage)</li>
 * <li>{@link org.apache.archiva.metadata.repository.storage.RepositoryStorage} - access to the physical storage of a
 * repository and the source artifacts and project models</li>
 * </ul>
 */
public class RepositorySession
    implements AutoCloseable
{
    private final MetadataRepository repository;

    private final MetadataResolver resolver;

    private boolean dirty;

    private Logger log = LoggerFactory.getLogger( getClass() );

    // FIXME: include storage here too - perhaps a factory based on repository ID, or one per type to retrieve and
    //        operate on a given repo within the storage API

    public RepositorySession( MetadataRepository metadataRepository, MetadataResolver resolver )
    {
        this.repository = metadataRepository;
        this.resolver = resolver;
    }

    public MetadataRepository getRepository()
    {
        return repository;
    }

    public MetadataResolver getResolver()
    {
        return resolver;
    }

    protected boolean isDirty() {
        return dirty;
    }

    public void save() throws MetadataSessionException
    {
        this.dirty = false;
    }

    public void revert() throws MetadataSessionException
    {
        this.dirty = false;
    }

    /**
     * Close the session. Required to be called for all open sessions to ensure resources are properly released.
     * If the session has been marked as dirty, it will be saved. This may save partial changes in the case of a typical
     * <code>try { ... } finally { ... }</code> approach - if this is a problem, ensure you revert changes when an
     * exception occurs.
     * <b>can throw RuntimeException</b>
     */
    @Override
    public void close()
    {
        try
        {
            if ( isDirty() )
            {
                save();
            }
        }
        catch ( MetadataSessionException e )
        {
            throw new RuntimeException( "Could not save the session " + e.getMessage( ), e );
        }
    }

    /**
     * ignore RuntimeException when closing repository
     * @since 1.4-M4
     */
    public void closeQuietly()
    {
        try
        {
            this.close();
        }
        catch ( RuntimeException e )
        {
            log.warn( "ignore Runtime exception while closing: {}", e.getMessage(), e );
        }
    }


    public void markDirty()
    {
        this.dirty = true;
    }

    public void refresh() throws MetadataSessionException {

    }

    public void refreshAndDiscard() throws MetadataSessionException {

    }
}
