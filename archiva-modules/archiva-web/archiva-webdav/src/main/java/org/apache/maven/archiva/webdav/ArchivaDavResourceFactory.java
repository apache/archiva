package org.apache.maven.archiva.webdav;

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

import com.opensymphony.xwork.ActionContext;
import org.apache.jackrabbit.webdav.*;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.apache.maven.archiva.repository.audit.AuditListener;
import org.apache.maven.archiva.repository.audit.Auditable;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.webdav.util.WebdavMethodUtil;
import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.apache.maven.archiva.webdav.util.RepositoryPathUtil;
import org.apache.maven.archiva.proxy.RepositoryProxyConnectors;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.policies.ProxyDownloadException;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.archiva.security.ServletAuthenticator;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.redback.xwork.filter.authentication.HttpAuthenticator;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 * @plexus.component role="org.apache.maven.archiva.webdav.ArchivaDavResourceFactory"
 */
public class ArchivaDavResourceFactory
    implements DavResourceFactory, Auditable
{
    private Logger log = LoggerFactory.getLogger( ArchivaDavResourceFactory.class );

    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.audit.AuditListener"
     */
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryRequest repositoryRequest;

    /**
     * @plexus.requirement role-hint="default"
     */
    private RepositoryProxyConnectors connectors;

    /**
     * @plexus.requirement
     */
    private MetadataTools metadataTools;

    /**
     * @plexus.requirement
     */
    private MimeTypes mimeTypes;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;
    
    /**
     * @plexus.requirement
     */
    private ServletAuthenticator servletAuth;

    /**
     * @plexus.requirement role-hint="basic"
     */
    private HttpAuthenticator httpAuth;
    
    public DavResource createResource( final DavResourceLocator locator, final DavServletRequest request,
                                       final DavServletResponse response )
        throws DavException
    {   
        checkLocatorIsInstanceOfRepositoryLocator( locator );
        ArchivaDavResourceLocator archivaLocator = (ArchivaDavResourceLocator) locator;
        
        RepositoryGroupConfiguration repoGroupConfig =
            archivaConfiguration.getConfiguration().getRepositoryGroupsAsMap().get(
                                                                                    ( (RepositoryLocator) locator ).getRepositoryId() );
        List<String> repositories = new ArrayList<String>();

        boolean isGet = WebdavMethodUtil.isReadMethod( request.getMethod() );
        boolean isPut = WebdavMethodUtil.isWriteMethod( request.getMethod() );
                
        if ( repoGroupConfig != null )
        {   
            if( WebdavMethodUtil.isWriteMethod( request.getMethod() ) )
            {
                throw new DavException( HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                        "Write method not allowed for repository groups." );
            }
            repositories.addAll( repoGroupConfig.getRepositories() );
            
            // handle browse requests for virtual repos
            if ( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ).endsWith( "/" ) )                
            {                    
                return getResource( request, repositories, archivaLocator );                
            }            
        }
        else
        {
            repositories.add( ( (RepositoryLocator) locator ).getRepositoryId() );
        }

        DavResource resource = null;
        DavException e = null;

        for ( String repositoryId : repositories )
        {
            ManagedRepositoryContent managedRepository = null;

            try
            {
                managedRepository = getManagedRepository( repositoryId );
            }
            catch ( DavException de )
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Invalid managed repository <" +
                    repositoryId + ">" );
            }

            if ( !locator.getResourcePath().startsWith( ArchivaDavResource.HIDDEN_PATH_PREFIX ) )
            {
                if ( managedRepository != null )
                {
                    try
                    {
                        if( isAuthorized( request, repositoryId ) )
                        {                        
                            LogicalResource logicalResource =
                                new LogicalResource( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ) );
                                                
                            if ( isGet )
                            {
                                resource = doGet( managedRepository, request, archivaLocator, logicalResource );
                            }
        
                            if ( isPut )
                            {
                                resource = doPut( managedRepository, request, archivaLocator, logicalResource );
                            }
                        }
                    }
                    catch ( DavException de )
                    {   
                        e = de;
                        continue;
                    }
                                        
                    if( resource == null )
                    {
                        e = new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource does not exist" );
                    }
                    else
                    {
                        setHeaders( locator, response );

                        // compatibility with MRM-440 to ensure browsing the repository works ok
                        if ( resource.isCollection() && !resource.getLocator().getResourcePath().endsWith( "/" ) )
                        {
                            throw new BrowserRedirectException( resource.getHref() );
                        }

                        return resource;
                    }
                }
                else
                {
                    e = new DavException( HttpServletResponse.SC_NOT_FOUND, "Repository does not exist" );
                }
            }
        }

        throw e;
    }
    
    public DavResource createResource( final DavResourceLocator locator, final DavSession davSession )
        throws DavException
    {        
        checkLocatorIsInstanceOfRepositoryLocator( locator );
        ArchivaDavResourceLocator archivaLocator = (ArchivaDavResourceLocator) locator;

        DavResource resource = null;
        if ( !locator.getResourcePath().startsWith( ArchivaDavResource.HIDDEN_PATH_PREFIX ) )
        {
            ManagedRepositoryContent managedRepository = getManagedRepository( archivaLocator.getRepositoryId() );
            String logicalResource = RepositoryPathUtil.getLogicalResource( locator.getResourcePath() );
            File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource );
            resource =
                new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource, mimeTypes, archivaLocator,
                                        this );
        }
        return resource;
    }

    private DavResource doGet( ManagedRepositoryContent managedRepository, DavServletRequest request,
                               ArchivaDavResourceLocator locator, LogicalResource logicalResource )
        throws DavException
    {
        File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource.getPath() );
        ArchivaDavResource resource =
            new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource.getPath(), mimeTypes, locator, this );

        if ( !resource.isCollection() )
        {
            // At this point the incoming request can either be in default or
            // legacy layout format.
            boolean fromProxy = fetchContentFromProxies( managedRepository, request, logicalResource );

            boolean previouslyExisted = resourceFile.exists();

            try
            {
                // Perform an adjustment of the resource to the managed
                // repository expected path.
                String localResourcePath =
                    repositoryRequest.toNativePath( logicalResource.getPath(), managedRepository );
                resourceFile = new File( managedRepository.getRepoRoot(), localResourcePath );
            }
            catch ( LayoutException e )
            {
                if ( previouslyExisted )
                {
                    return resource;
                }
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
            }

            // Attempt to fetch the resource from any defined proxy.
            if ( fromProxy )
            {
                processAuditEvents( request, locator.getWorkspaceName(), logicalResource.getPath(), previouslyExisted,
                                    resourceFile, " (proxied)" );
            }
            resource =
                new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource.getPath(), mimeTypes, locator,
                                        this );

            if ( !resourceFile.exists() )
            {
                resource = null;
            }
        }
        return resource;
    }

    private DavResource doPut( ManagedRepositoryContent managedRepository, DavServletRequest request,
                               ArchivaDavResourceLocator locator, LogicalResource logicalResource )
        throws DavException
    {
        /*
         * Create parent directories that don't exist when writing a file This actually makes this implementation not
         * compliant to the WebDAV RFC - but we have enough knowledge about how the collection is being used to do this
         * reasonably and some versions of Maven's WebDAV don't correctly create the collections themselves.
         */

        File rootDirectory = new File( managedRepository.getRepoRoot() );
        File destDir = new File( rootDirectory, logicalResource.getPath() ).getParentFile();
        if ( !destDir.exists() )
        {
            destDir.mkdirs();
            String relPath = PathUtil.getRelative( rootDirectory.getAbsolutePath(), destDir );
            triggerAuditEvent( request, logicalResource.getPath(), relPath, AuditEvent.CREATE_DIR );
        }

        File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource.getPath() );

        boolean previouslyExisted = resourceFile.exists();

        processAuditEvents( request, locator.getRepositoryId(), logicalResource.getPath(), previouslyExisted,
                            resourceFile, null );

        return new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource.getPath(), mimeTypes, locator,
                                       this );
    }

    private boolean fetchContentFromProxies( ManagedRepositoryContent managedRepository, DavServletRequest request,
                                             LogicalResource resource )
        throws DavException
    {
        if ( repositoryRequest.isSupportFile( resource.getPath() ) )
        {
            // Checksums are fetched with artifact / metadata.

            // Need to adjust the path for the checksum resource.
            return false;
        }

        // Is it a Metadata resource?
        if ( repositoryRequest.isDefault( resource.getPath() ) && repositoryRequest.isMetadata( resource.getPath() ) )
        {
            return fetchMetadataFromProxies( managedRepository, request, resource );
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        try
        {
            // Get the artifact reference in a layout neutral way.
            ArtifactReference artifact = repositoryRequest.toArtifactReference( resource.getPath() );

            if ( artifact != null )
            {
                applyServerSideRelocation( managedRepository, artifact );

                File proxiedFile = connectors.fetchFromProxies( managedRepository, artifact );

                resource.setPath( managedRepository.toPath( artifact ) );

                return ( proxiedFile != null );
            }
        }
        catch ( LayoutException e )
        {
            /* eat it */
        }
        catch ( ProxyDownloadException e )
        {
            log.error( e.getMessage(), e );
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to fetch artifact resource." );
        }
        return false;
    }

    private boolean fetchMetadataFromProxies( ManagedRepositoryContent managedRepository, DavServletRequest request,
                                              LogicalResource resource )
        throws DavException
    {
        ProjectReference project;
        VersionedReference versioned;

        try
        {

            versioned = metadataTools.toVersionedReference( resource.getPath() );
            if ( versioned != null )
            {
                connectors.fetchFromProxies( managedRepository, versioned );
                return true;
            }
        }
        catch ( RepositoryMetadataException e )
        {
            /* eat it */
        }

        try
        {
            project = metadataTools.toProjectReference( resource.getPath() );
            if ( project != null )
            {
                connectors.fetchFromProxies( managedRepository, project );
                return true;
            }
        }
        catch ( RepositoryMetadataException e )
        {
            /* eat it */
        }

        return false;
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
        connectors.fetchFromProxies( managedRepository, pomReference );

        // Open and read the POM from the managed repo
        File pom = managedRepository.toFile( pomReference );

        if ( !pom.exists() )
        {
            return;
        }

        try
        {
            Model model = new MavenXpp3Reader().read( new FileReader( pom ) );
            DistributionManagement dist = model.getDistributionManagement();
            if ( dist != null )
            {
                Relocation relocation = dist.getRelocation();
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

    private void processAuditEvents( DavServletRequest request, String repositoryId, String resource,
                                     boolean previouslyExisted, File resourceFile, String suffix )
    {
        if ( suffix == null )
        {
            suffix = "";
        }

        // Process Create Audit Events.
        if ( !previouslyExisted && resourceFile.exists() )
        {
            if ( resourceFile.isFile() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.CREATE_FILE + suffix );
            }
            else if ( resourceFile.isDirectory() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.CREATE_DIR + suffix );
            }
        }
        // Process Remove Audit Events.
        else if ( previouslyExisted && !resourceFile.exists() )
        {
            if ( resourceFile.isFile() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.REMOVE_FILE + suffix );
            }
            else if ( resourceFile.isDirectory() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.REMOVE_DIR + suffix );
            }
        }
        // Process modify events.
        else
        {
            if ( resourceFile.isFile() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.MODIFY_FILE + suffix );
            }
        }
    }

    private void triggerAuditEvent( String user, String remoteIP, String repositoryId, String resource, String action )
    {
        AuditEvent event = new AuditEvent( repositoryId, user, resource, action );
        event.setRemoteIP( remoteIP );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    private void triggerAuditEvent( DavServletRequest request, String repositoryId, String resource, String action )
    {
        triggerAuditEvent( ArchivaXworkUser.getActivePrincipal( ActionContext.getContext().getSession() ),
                           getRemoteIP( request ), repositoryId, resource, action );
    }

    private String getRemoteIP( DavServletRequest request )
    {
        return request.getRemoteAddr();
    }

    public void addAuditListener( AuditListener listener )
    {
        this.auditListeners.add( listener );
    }

    public void clearAuditListeners()
    {
        this.auditListeners.clear();
    }

    public void removeAuditListener( AuditListener listener )
    {
        this.auditListeners.remove( listener );
    }

    private void setHeaders( DavResourceLocator locator, DavServletResponse response )
    {
        // [MRM-503] - Metadata file need Pragma:no-cache response
        // header.
        if ( locator.getResourcePath().endsWith( "/maven-metadata.xml" ) )
        {
            response.addHeader( "Pragma", "no-cache" );
            response.addHeader( "Cache-Control", "no-cache" );
        }

        // TODO: [MRM-524] determine http caching options for other types of files (artifacts, sha1, md5, snapshots)
    }

    private ManagedRepositoryContent getManagedRepository( String respositoryId )
        throws DavException
    {
        if ( respositoryId != null )
        {
            try
            {
                return repositoryFactory.getManagedRepositoryContent( respositoryId );
            }
            catch ( RepositoryNotFoundException e )
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
            }
            catch ( RepositoryException e )
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
            }
        }
        return null;
    }

    private void checkLocatorIsInstanceOfRepositoryLocator( DavResourceLocator locator )
        throws DavException
    {
        if ( !( locator instanceof RepositoryLocator ) )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Locator does not implement RepositoryLocator" );
        }
    }

    class LogicalResource
    {
        private String path;

        public LogicalResource( String path )
        {
            this.path = path;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath( String path )
        {
            this.path = path;
        }
    }
    
    protected boolean isAuthorized( DavServletRequest request, String repositoryId )
        throws DavException
    {
        try
        {
            AuthenticationResult result = httpAuth.getAuthenticationResult( request, null );            
            SecuritySession securitySession = httpAuth.getSecuritySession();
                       
            return servletAuth.isAuthenticated( request, result ) &&
                servletAuth.isAuthorized( request, securitySession, repositoryId,
                                          WebdavMethodUtil.isWriteMethod( request.getMethod() ) );
        }
        catch ( AuthenticationException e )
        {            
            throw new UnauthorizedDavException( repositoryId, "You are not authenticated" );
        }
        catch ( MustChangePasswordException e )
        {         
            throw new UnauthorizedDavException( repositoryId, "You must change your password." );
        }
        catch ( AccountLockedException e )
        {            
            throw new UnauthorizedDavException( repositoryId, "User account is locked." );
        }
        catch ( AuthorizationException e )
        {         
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Fatal Authorization Subsystem Error." );
        }
        catch ( UnauthorizedException e )
        {         
            throw new UnauthorizedDavException( repositoryId, e.getMessage() );
        }
    }
    
    private DavResource getResource( DavServletRequest request, List<String> repositories, ArchivaDavResourceLocator locator )
        throws DavException
    {
        List<File> mergedRepositoryContents = new ArrayList<File>();        
        LogicalResource logicalResource =
            new LogicalResource( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ) );
        
        // flow: 
        // if the current user logged in has permission to any of the repositories, allow user to
        // browse the repo group but displaying only the repositories which the user has permission to access.
        // otherwise, prompt for authentication.
        
        // put the current session in the session map which will be passed to ArchivaXworkUser
        Map<String, Object> sessionMap = new HashMap<String, Object>();
        if( request.getSession().getAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY ) != null )
        {
            sessionMap.put( SecuritySystemConstants.SECURITY_SESSION_KEY, 
                            request.getSession().getAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY ) );
        }
        
        String activePrincipal = ArchivaXworkUser.getActivePrincipal( sessionMap );        
        boolean allow = isAllowedToContinue( request, repositories, activePrincipal );
              
        if( allow )
        {            
            for( String repository : repositories )
            {    
                // for prompted authentication
                if( httpAuth.getSecuritySession() != null )
                {
                    try
                    {   
                        if( isAuthorized( request, repository ) )                        
                        {
                            getResource( locator, mergedRepositoryContents, logicalResource, repository );
                        }
                    }                    
                    catch ( DavException e )
                    {                        
                        continue;
                    }
                }
                else
                {
                    // for the current user logged in 
                    try
                    {
                        if( servletAuth.isAuthorizedToAccessVirtualRepository( activePrincipal, repository ) )
                        {
                            getResource( locator, mergedRepositoryContents, logicalResource, repository );
                        }
                    }
                    catch ( UnauthorizedException e )                    
                    {                        
                        continue;
                    }
                }                
            }
        }
        else
        {
            throw new UnauthorizedDavException( locator.getRepositoryId(), "User not authorized." );
        }
        
        ArchivaVirtualDavResource resource =
            new ArchivaVirtualDavResource( mergedRepositoryContents, logicalResource.getPath(), mimeTypes, locator, this );
       
        // compatibility with MRM-440 to ensure browsing the repository group works ok
        if ( resource.isCollection() && !resource.getLocator().getResourcePath().endsWith( "/" ) )
        {
            throw new BrowserRedirectException( resource.getHref() );
        }
        
        return resource;
    }

    private void getResource( ArchivaDavResourceLocator locator, List<File> mergedRepositoryContents,
                              LogicalResource logicalResource, String repository )
        throws DavException
    {
        ManagedRepositoryContent managedRepository = null;

        try
        {
            managedRepository = getManagedRepository( repository );
        }
        catch ( DavException de )
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Invalid managed repository <" +
                repository + ">" );
        }                            
        
        if ( !locator.getResourcePath().startsWith( ArchivaVirtualDavResource.HIDDEN_PATH_PREFIX ) )
        {
            if( managedRepository != null )
            {   
                File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource.getPath() );
                if( resourceFile.exists() )
                {                    
                    mergedRepositoryContents.add( resourceFile );
                }                    
            }
        }
    }
    
    /**
     * Check if the current user is authorized to access any of the repos
     *  
     * @param request
     * @param repositories
     * @param activePrincipal
     * @return
     */
    private boolean isAllowedToContinue( DavServletRequest request, List<String> repositories, String activePrincipal )    
    {
        boolean allow = false;
        
              
        // if securitySession != null, it means that the user was prompted for authentication
        if( httpAuth.getSecuritySession() != null )
        {
            for( String repository : repositories )
            {
                try
                {
                    if( isAuthorized( request, repository ) )
                    {
                        allow = true;
                        break;
                    }
                }
                catch( DavException e )
                {                    
                    continue;
                }
            }  
        }
        else
        {   
            for( String repository : repositories )
            {
                try
                {
                    if( servletAuth.isAuthorizedToAccessVirtualRepository( activePrincipal, repository ) )
                    {
                        allow = true;
                        break;
                    }
                }
                catch ( UnauthorizedException e )
                {                    
                    continue;
                }
            }  
        }
        
        return allow;
    }
        
}
