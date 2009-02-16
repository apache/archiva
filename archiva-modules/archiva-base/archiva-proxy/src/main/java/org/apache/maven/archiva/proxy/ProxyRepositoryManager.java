package org.apache.maven.archiva.proxy;

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

import org.apache.archiva.repository.api.MutableResourceContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerException;
import org.apache.archiva.repository.api.RepositoryManagerWeight;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.SystemRepositoryManager;
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.ConfigurationListener;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.policies.ProxyDownloadException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RepositoryManagerWeight(500)
public class ProxyRepositoryManager implements RepositoryManager
{
    private static final Logger log = LoggerFactory.getLogger(ProxyRepositoryManager.class);

    private final SystemRepositoryManager repositoryManager;

    private final RepositoryProxyConnectors repositoryProxyConnectors;

    private final RepositoryContentFactory repositoryFactory;

    private final RepositoryRequest repositoryRequest;

    public ProxyRepositoryManager(SystemRepositoryManager repositoryManager, RepositoryProxyConnectors repositoryProxyConnectors, RepositoryContentFactory repositoryFactory, RepositoryRequest repositoryRequest)
    {
        this.repositoryManager = repositoryManager;
        this.repositoryProxyConnectors = repositoryProxyConnectors;
        this.repositoryFactory = repositoryFactory;
        this.repositoryRequest = repositoryRequest;
    }

    public boolean exists(String repositoryId)
    {
        return repositoryManager.exists(repositoryId);
    }

    public ResourceContext handles(ResourceContext context)
    {
        final List<ProxyConnector> connectors = repositoryProxyConnectors.getProxyConnectors(context.getRepositoryId());
        if (connectors != null && connectors.size() > 0)
        {
            return context;
        }
        return null;
    }

    public boolean read(ResourceContext context, OutputStream os)
    {
        return repositoryManager.read(context, os);
    }

    public List<Status> stat(ResourceContext context)
    {
        //Return the stat() result from the system repository if we are simply getting a collection

        ManagedRepositoryContent managedRepository = null;
        try
        {
            managedRepository = repositoryFactory.getManagedRepositoryContent(context.getRepositoryId());
        }
        catch (RepositoryNotFoundException e)
        {
            throw new RepositoryManagerException(e.getMessage(), e);
        }
        catch (RepositoryException e)
        {
            throw new RepositoryManagerException(e.getMessage(), e);
        }

        //Check if logical path can be mushed into a valid path. This is so ugly.
        boolean isCollection = false;
        try
        {
            this.repositoryRequest.toArtifactReference(context.getLogicalPath());
        }
        catch (LayoutException e)
        {
            try
            {
                repositoryRequest.toNativePath(context.getLogicalPath(), managedRepository);
            }
            catch (LayoutException ex)
            {
                isCollection = true;
            }
        }

        if (isCollection)
        {
            return repositoryManager.stat(context);
        }

        final Status status = doProxy(managedRepository, context);
        if (status != null)
        {
            return Arrays.asList(status);
        }
        return Collections.EMPTY_LIST;
    }

    public boolean write(ResourceContext context, InputStream is)
    {
        return repositoryManager.write(context, is);
    }

    private Status doProxy( ManagedRepositoryContent managedRepository, ResourceContext context )
        throws RepositoryManagerException
    {
        File resourceFile = new File( managedRepository.getRepoRoot(), context.getLogicalPath() );
        
        // At this point the incoming request can either be in default or
        // legacy layout format.
        Status status = fetchContentFromProxies( managedRepository, context );

        try
        {
            // Perform an adjustment of the resource to the managed
            // repository expected path.
            String localResourcePath = repositoryRequest.toNativePath( context.getLogicalPath(), managedRepository );
            resourceFile = new File( managedRepository.getId(), localResourcePath );
        }
        catch ( LayoutException e )
        {
            return Status.fromFile(resourceFile);
        }

        // Attempt to fetch the resource from any defined proxy.
        if ( status != null )
        {
            String repositoryId = context.getRepositoryId();
//            String event = ( previouslyExisted ? AuditEvent.MODIFY_FILE : AuditEvent.CREATE_FILE ) + PROXIED_SUFFIX;
//            triggerAuditEvent( request.getRemoteAddr(), repositoryId, logicalResource.getPath(), event );
        }

        return status;
    }

    private Status fetchContentFromProxies( ManagedRepositoryContent managedRepository, ResourceContext context )
        throws RepositoryManagerException
    {
        if ( repositoryRequest.isSupportFile( context.getLogicalPath() ) )
        {
            File proxiedFile = repositoryProxyConnectors.fetchFromProxies( managedRepository, context.getLogicalPath() );
            if (proxiedFile != null)
            {
                return Status.fromFile(proxiedFile);
            }
        }

        // Is it a Metadata resource?
        if ( repositoryRequest.isDefault( context.getLogicalPath() ) && repositoryRequest.isMetadata( context.getLogicalPath() ) )
        {
            final File result = repositoryProxyConnectors.fetchMetatadaFromProxies(managedRepository, context.getLogicalPath());
            if (result != null)
            {
                return Status.fromFile(result);
            }
            return null;
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        try
        {
            // Get the artifact reference in a layout neutral way.
            ArtifactReference artifact = repositoryRequest.toArtifactReference( context.getLogicalPath() );

            if ( artifact != null )
            {
                applyServerSideRelocation( managedRepository, artifact );

                File proxiedFile = repositoryProxyConnectors.fetchFromProxies( managedRepository, artifact );

                MutableResourceContext resourceContext = new MutableResourceContext(context);
                resourceContext.setLogicalPath(managedRepository.toPath( artifact ));

                if (proxiedFile != null)
                {
                    return Status.fromFile(proxiedFile);
                }
            }
        }
        catch ( LayoutException e )
        {
            /* eat it */
        }
        catch ( ProxyDownloadException e )
        {
            throw new RepositoryManagerException(e.getMessage(), e);
        }

        File resourceFile = new File(managedRepository.getRepoRoot(), context.getLogicalPath());
        if (resourceFile.exists())
        {
            return Status.fromFile(resourceFile);
        }
        return null;
    }

    /**
     * A relocation capable client will request the POM prior to the artifact, and will then read meta-data and do
     * client side relocation. A simplier client (like maven 1) will only request the artifact and not use the
     * metadatas.
     * <p>
     * For such clients, archiva does server-side relocation by reading itself the &lt;relocation&gt; element in
     * metadatas and serving the expected artifact.
     */
    protected void applyServerSideRelocation( ManagedRepositoryContent managedRepository, ArtifactReference artifact )
        throws ProxyDownloadException
    {
        if ( "pom".equals( artifact.getType() ) )
        {
            return;
        }

        // Build the artifact POM reference
        ArtifactReference pomReference = new ArtifactReference();
        pomReference.setGroupId( artifact.getGroupId() );
        pomReference.setArtifactId( artifact.getArtifactId() );
        pomReference.setVersion( artifact.getVersion() );
        pomReference.setType( "pom" );

        // Get the artifact POM from proxied repositories if needed
        repositoryProxyConnectors.fetchFromProxies( managedRepository, pomReference );

        // Open and read the POM from the managed repo
        File pom = managedRepository.toFile( pomReference );

        if ( !pom.exists() )
        {
            return;
        }

        try
        {
            // MavenXpp3Reader leaves the file open, so we need to close it ourselves.
            FileReader reader = new FileReader( pom );
            Model model = null;
            try
            {
                model = new MavenXpp3Reader().read( reader );
            }
            finally
            {
                if (reader != null)
                {
                    reader.close();
                }
            }

            final DistributionManagement dist = model.getDistributionManagement();
            if ( dist != null )
            {
                final Relocation relocation = dist.getRelocation();
                if ( relocation != null )
                {
                    // artifact is relocated : update the repositoryPath
                    if ( relocation.getGroupId() != null )
                    {
                        artifact.setGroupId( relocation.getGroupId() );
                    }
                    if ( relocation.getArtifactId() != null )
                    {
                        artifact.setArtifactId( relocation.getArtifactId() );
                    }
                    if ( relocation.getVersion() != null )
                    {
                        artifact.setVersion( relocation.getVersion() );
                    }
                }
            }
        }
        catch ( FileNotFoundException e )
        {
            // Artifact has no POM in repo : ignore
        }
        catch ( IOException e )
        {
            // Unable to read POM : ignore.
        }
        catch ( XmlPullParserException e )
        {
            // Invalid POM : ignore
        }
    }
}
