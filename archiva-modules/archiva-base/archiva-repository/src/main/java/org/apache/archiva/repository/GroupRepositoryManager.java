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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.archiva.repository.api.InvalidOperationException;
import org.apache.archiva.repository.api.MutableResourceContext;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerException;
import org.apache.archiva.repository.api.RepositoryManagerWeight;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.SystemRepositoryManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataMerge;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RepositoryManagerWeight(400)
public class GroupRepositoryManager implements RepositoryManager
{
    private static final Logger log = LoggerFactory.getLogger(GroupRepositoryManager.class);

    private final ArchivaConfiguration archivaConfiguration;

    private final RepositoryManager proxyRepositoryManager;

    private final SystemRepositoryManager systemRepositoryManager;

    private final LegacyRepositoryManager legacyRepositoryManager;

    private final RepositoryRequest repositoryRequest;

    private final MetadataTools metadataTools;

    public GroupRepositoryManager(ArchivaConfiguration archivaConfiguration, RepositoryManager proxyRepositoryManager, SystemRepositoryManager systemRepositoryManager, LegacyRepositoryManager legacyRepositoryManager, RepositoryRequest repositoryRequest, MetadataTools metadataTools)
    {
        this.archivaConfiguration = archivaConfiguration;
        this.proxyRepositoryManager = proxyRepositoryManager;
        this.systemRepositoryManager = systemRepositoryManager;
        this.legacyRepositoryManager = legacyRepositoryManager;
        this.repositoryRequest = repositoryRequest;
        this.metadataTools = metadataTools;
    }

    public boolean exists(String repositoryId)
    {
        return (getGroupConfiguration(repositoryId) != null);
    }

    public ResourceContext handles(ResourceContext context)
    {
        if (getGroupConfiguration(context.getRepositoryId()) != null)
        {
            return context;
        }
        return null;
    }

    public boolean read(ResourceContext context, OutputStream os)
    {
        final RepositoryGroupConfiguration groupConfiguration = getGroupConfiguration(context.getRepositoryId());
        if (isMetadataRequest(context) && isProjectReference(context))
        {
            ArchivaRepositoryMetadata mainMetadata = null;
            for (final String repositoryId : groupConfiguration.getRepositories() )
            {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final MutableResourceContext resourceContext = new MutableResourceContext(context);
                resourceContext.setRepositoryId(repositoryId);

                if (systemRepositoryManager.read(resourceContext, baos))
                {
                    try
                    {
                        ArchivaRepositoryMetadata metadata = RepositoryMetadataReader.read(new ByteArrayInputStream(baos.toByteArray()));
                        if (metadata != null && mainMetadata != null)
                        {
                            mainMetadata = RepositoryMetadataMerge.merge(mainMetadata, metadata);
                        }
                        else if (metadata != null && mainMetadata == null)
                        {
                            mainMetadata = metadata;
                        }
                    }
                    catch (RepositoryMetadataException e)
                    {
                        log.error("Could not merge " + resourceContext.getLogicalPath() + "from repository " + context.getRepositoryId(), e);
                    }
                }
            }

            try
            {
                final OutputStreamWriter writer = new OutputStreamWriter(os);
                if (context.getLogicalPath().endsWith(".md5"))
                {
                    writer.write(DigestUtils.md5Hex(getMetadataAsByteArray(mainMetadata)));
                }
                else if (context.getLogicalPath().endsWith(".sha1"))
                {
                    writer.write(DigestUtils.shaHex(getMetadataAsByteArray(mainMetadata)));
                }
                else
                {
                    RepositoryMetadataWriter.write(mainMetadata, writer);
                }
                writer.flush();
                writer.close();
                return true;
            }
            catch (IOException e)
            {
                throw new RepositoryManagerException("Could complete request in repository " + context.getRepositoryId() + " for " + context.getLogicalPath(), e);
            }
            catch (RepositoryMetadataException e)
            {
                throw new RepositoryManagerException("Could complete request in repository " + context.getRepositoryId() + " for " + context.getLogicalPath(), e);
            }
        }
        else
        {
            return readFromGroup(groupConfiguration, context, os);
        }
    }
    
    public boolean write(ResourceContext context, InputStream is)
    {
        throw new InvalidOperationException("Repository Groups are not writable: " + context.getRepositoryId());
    }

    public List<Status> stat(ResourceContext context)
    {
        final RepositoryGroupConfiguration groupConfiguration = getGroupConfiguration(context.getRepositoryId());
        
        final LinkedHashMap<String, Status> statusMap = new LinkedHashMap<String, Status>();
        for (final String repositoryId : groupConfiguration.getRepositories())
        {
            final MutableResourceContext resourceContext = new MutableResourceContext(context);
            resourceContext.setRepositoryId(repositoryId);

            ResourceContext rc = proxyRepositoryManager.handles(resourceContext);
            if (rc != null)
            {
                addStatResultToMap(statusMap, rc, proxyRepositoryManager);
            }
            else
            {
                rc = legacyRepositoryManager.handles(resourceContext);
                if (rc != null)
                {
                    addStatResultToMap(statusMap, rc, proxyRepositoryManager);
                }
                else
                {
                    rc = systemRepositoryManager.handles(resourceContext);
                    if (rc != null)
                    {
                        addStatResultToMap(statusMap, rc, systemRepositoryManager);
                    }
                }
            }
        }
        return new ArrayList<Status>(statusMap.values());
    }

    private void addStatResultToMap(final Map<String, Status> statusMap, final ResourceContext resourceContext, final RepositoryManager repositoryManager)
    {
        for (final Status status : repositoryManager.stat(resourceContext))
        {
            statusMap.put(status.getName(), status);
        }
    }

    private boolean readFromGroup(final RepositoryGroupConfiguration groupConfiguration, ResourceContext context, OutputStream os)
    {
        for (final String repositoryId : groupConfiguration.getRepositories())
        {
            final MutableResourceContext resourceContext = new MutableResourceContext(context);
            resourceContext.setRepositoryId(repositoryId);
            if (systemRepositoryManager.read(resourceContext, os))
            {
                return true;
            }
        }
        return false;
    }

    private byte[] getMetadataAsByteArray(ArchivaRepositoryMetadata metadata)
        throws RepositoryMetadataException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputStreamWriter writer = new OutputStreamWriter(baos);
        RepositoryMetadataWriter.write(metadata, writer);
        return baos.toByteArray();
    }

    private RepositoryGroupConfiguration getGroupConfiguration(final String repositoryId)
    {
        return archivaConfiguration.getConfiguration().getRepositoryGroupsAsMap().get( repositoryId );
    }

    private boolean isMetadataRequest(ResourceContext context)
    {
        return repositoryRequest.isMetadata(context.getLogicalPath()) || context.getLogicalPath().endsWith( "metadata.xml.sha1" ) || context.getLogicalPath().endsWith( "metadata.xml.md5" );
    }

    private boolean isProjectReference( ResourceContext context )
    {
       try
       {
           metadataTools.toVersionedReference( context.getLogicalPath() );
           return false;
       }
       catch ( RepositoryMetadataException e )
       {
           return true;
       }
    }
}
