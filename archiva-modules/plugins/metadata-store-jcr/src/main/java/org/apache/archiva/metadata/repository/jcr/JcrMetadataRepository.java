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

import com.google.common.collect.ImmutableMap;
import org.apache.archiva.metadata.QueryParameter;
import org.apache.archiva.metadata.model.*;
import org.apache.archiva.metadata.model.maven2.MavenArtifactFacet;
import org.apache.archiva.metadata.repository.AbstractMetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataService;
import org.apache.archiva.metadata.repository.MetadataSessionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static javax.jcr.Property.JCR_LAST_MODIFIED;
import static org.apache.archiva.metadata.repository.jcr.JcrConstants.*;

/**
 * TODO below: revise storage format for project version metadata
 * TODO revise reference storage
 */
public class JcrMetadataRepository
    extends AbstractMetadataRepository implements MetadataRepository, RepositoryStatisticsProvider
{


    private static final String QUERY_ARTIFACT_1 = "SELECT * FROM [" + ARTIFACT_NODE_TYPE + "] AS artifact WHERE ISDESCENDANTNODE(artifact,'/";

    static final String QUERY_ARTIFACTS_BY_PROJECT_VERSION_1 = "SELECT * FROM [" + PROJECT_VERSION_NODE_TYPE + "] AS projectVersion INNER JOIN [" + ARTIFACT_NODE_TYPE
        + "] AS artifact ON ISCHILDNODE(artifact, projectVersion) INNER JOIN [" + FACET_NODE_TYPE
        + "] AS facet ON ISCHILDNODE(facet, projectVersion) WHERE ([facet].[";
    static final String QUERY_ARTIFACTS_BY_PROJECT_VERSION_2= "] = $value)";

    static final String QUERY_ARTIFACTS_BY_METADATA_1 = "SELECT * FROM [" + ARTIFACT_NODE_TYPE + "] AS artifact INNER JOIN [" + FACET_NODE_TYPE
        + "] AS facet ON ISCHILDNODE(facet, artifact) WHERE ([facet].[";
    static final String QUERY_ARTIFACTS_BY_METADATA_2 = "] = $value)";

    static final String QUERY_ARTIFACTS_BY_PROPERTY_1 = "SELECT * FROM [" + PROJECT_VERSION_NODE_TYPE + "] AS projectVersion INNER JOIN [" + ARTIFACT_NODE_TYPE
           + "] AS artifact ON ISCHILDNODE(artifact, projectVersion) WHERE ([projectVersion].[";
    static final String QUERY_ARTIFACTS_BY_PROPERTY_2 = "] = $value)";


    private static final String QUERY_ARTIFACT_2 = "')";

    private Logger log = LoggerFactory.getLogger( JcrMetadataRepository.class );

    private Repository repository;

    public JcrMetadataRepository( MetadataService metadataService, Repository repository )
        throws RepositoryException
    {
        super( metadataService );
        this.repository = repository;
    }


    public static void initializeNodeTypes( Session session )
        throws RepositoryException
    {

        // TODO: consider using namespaces for facets instead of the current approach:
        // (if used, check if actually called by normal injection)
//        for ( String facetId : metadataFacetFactories.keySet() )
//        {
//            session.getWorkspace().getNamespaceRegistry().registerNamespace( facetId, facetId );
//        }
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry registry = workspace.getNamespaceRegistry();

        if ( !Arrays.asList( registry.getPrefixes() ).contains( "archiva" ) )
        {
            registry.registerNamespace( "archiva", "http://archiva.apache.org/jcr/" );
        }

        try(
            Reader cndReader = new InputStreamReader(
                Thread.currentThread( ).getContextClassLoader( ).getResourceAsStream( "org/apache/archiva/metadata/repository/jcr/jcr-schema.cnd" ) ))
        {
            CndImporter.registerNodeTypes( cndReader, session );
        }
        catch ( ParseException e )
        {
            e.printStackTrace( );
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
        }

    }

    private Session getSession(RepositorySession repositorySession) throws MetadataRepositoryException {
        if (repositorySession instanceof JcrRepositorySession ) {
            return ( (JcrRepositorySession) repositorySession ).getJcrSession();
        } else {
            throw new MetadataRepositoryException( "The given session object is not a JcrSession instance: " + repositorySession.getClass( ).getName( ) );
        }
    }

    @Override
    public void updateProject( RepositorySession session, String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        updateProject( jcrSession, repositoryId, project.getNamespace(), project.getId() );
    }

    private void updateProject( Session jcrSession, String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException
    {
        updateNamespace( jcrSession , repositoryId, namespace );

        try
        {
            getOrAddProjectNode( jcrSession, repositoryId, namespace, projectId );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void updateArtifact( RepositorySession session, String repositoryId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        updateNamespace( session, repositoryId, namespace );

        try
        {
            Node node =
                getOrAddArtifactNode( jcrSession, repositoryId, namespace, projectId, projectVersion, artifactMeta.getId() );

            node.setProperty( "id", artifactMeta.getId( ) );
            Calendar cal = GregorianCalendar.from(artifactMeta.getFileLastModified());
            node.setProperty( JCR_LAST_MODIFIED, cal );

            cal = GregorianCalendar.from(artifactMeta.getWhenGathered());
            node.setProperty( "whenGathered", cal );

            node.setProperty( "size", artifactMeta.getSize() );
            node.setProperty( "md5", artifactMeta.getMd5() );
            node.setProperty( "sha1", artifactMeta.getSha1() );

            node.setProperty( "version", artifactMeta.getVersion() );

            // iterate over available facets to update/add/remove from the artifactMetadata
            for ( String facetId : metadataService.getSupportedFacets() )
            {
                MetadataFacet metadataFacet = artifactMeta.getFacet( facetId );
                if ( metadataFacet == null )
                {
                    continue;
                }
                if ( node.hasNode( facetId ) )
                {
                    node.getNode( facetId ).remove();
                }
                if ( metadataFacet != null )
                {
                    // recreate, to ensure properties are removed
                    Node n = node.addNode( facetId);
                    n.addMixin( FACET_NODE_TYPE );
                    n.setProperty( "facetId", facetId );

                    for ( Map.Entry<String, String> entry : metadataFacet.toProperties().entrySet() )
                    {
                        n.setProperty( entry.getKey(), entry.getValue() );
                    }
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void updateProjectVersion( RepositorySession session, String repositoryId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        updateProject( jcrSession, repositoryId, namespace, projectId );

        try
        {
            Node versionNode =
                getOrAddProjectVersionNode( jcrSession, repositoryId, namespace, projectId, versionMetadata.getId() );
            versionNode.setProperty( "id", versionMetadata.getId( ) );
            versionNode.setProperty( "name", StringUtils.isEmpty( versionMetadata.getName() ) ? "" : versionMetadata.getName() );
            versionNode.setProperty( "description", StringUtils.isEmpty( versionMetadata.getDescription() ) ? "" : versionMetadata.getDescription() );
            versionNode.setProperty( "url", versionMetadata.getUrl() );
            versionNode.setProperty( "incomplete", versionMetadata.isIncomplete() );

            // FIXME: decide how to treat these in the content repo
            if ( versionMetadata.getScm() != null )
            {
                versionNode.setProperty( "scm.connection", versionMetadata.getScm().getConnection() );
                versionNode.setProperty( "scm.developerConnection", versionMetadata.getScm().getDeveloperConnection() );
                versionNode.setProperty( "scm.url", versionMetadata.getScm().getUrl() );
            }
            if ( versionMetadata.getCiManagement() != null )
            {
                versionNode.setProperty( "ci.system", versionMetadata.getCiManagement().getSystem() );
                versionNode.setProperty( "ci.url", versionMetadata.getCiManagement().getUrl() );
            }
            if ( versionMetadata.getIssueManagement() != null )
            {
                versionNode.setProperty( "issue.system", versionMetadata.getIssueManagement().getSystem() );
                versionNode.setProperty( "issue.url", versionMetadata.getIssueManagement().getUrl() );
            }
            if ( versionMetadata.getOrganization() != null )
            {
                versionNode.setProperty( "org.name", versionMetadata.getOrganization().getName() );
                versionNode.setProperty( "org.url", versionMetadata.getOrganization().getUrl() );
            }
            int i = 0;
            for ( License license : versionMetadata.getLicenses() )
            {
                versionNode.setProperty( "license." + i + ".name", license.getName() );
                versionNode.setProperty( "license." + i + ".url", license.getUrl() );
                i++;
            }
            i = 0;
            for ( MailingList mailingList : versionMetadata.getMailingLists() )
            {
                versionNode.setProperty( "mailingList." + i + ".archive", mailingList.getMainArchiveUrl() );
                versionNode.setProperty( "mailingList." + i + ".name", mailingList.getName() );
                versionNode.setProperty( "mailingList." + i + ".post", mailingList.getPostAddress() );
                versionNode.setProperty( "mailingList." + i + ".unsubscribe", mailingList.getUnsubscribeAddress() );
                versionNode.setProperty( "mailingList." + i + ".subscribe", mailingList.getSubscribeAddress() );
                versionNode.setProperty( "mailingList." + i + ".otherArchives",
                                         join( mailingList.getOtherArchives() ) );
                i++;
            }

            if ( !versionMetadata.getDependencies().isEmpty() )
            {
                Node dependenciesNode = JcrUtils.getOrAddNode( versionNode, "dependencies" );

                for ( Dependency dependency : versionMetadata.getDependencies() )
                {
                    // Note that we deliberately don't alter the namespace path - not enough dependencies for
                    // number of nodes at a given depth to be an issue. Similarly, we don't add subnodes for each
                    // component of the ID as that creates extra depth and causes a great cost in space and memory

                    // FIXME: change group ID to namespace
                    // FIXME: change to artifact's ID - this is constructed by the Maven 2 format for now.
                    //        This won't support types where the extension doesn't match the type.
                    //        (see also Maven2RepositoryStorage#readProjectVersionMetadata construction of POM)
                    String id =
                        dependency.getGroupId() + ";" + dependency.getArtifactId() + "-" + dependency.getVersion();
                    if ( dependency.getClassifier() != null )
                    {
                        id += "-" + dependency.getClassifier();
                    }
                    id += "." + dependency.getType();

                    Node n = JcrUtils.getOrAddNode( dependenciesNode, id, DEPENDENCY_NODE_TYPE );
                    n.setProperty( "id", id );

                    // FIXME: remove temp code just to make it keep working
                    n.setProperty( "groupId", dependency.getGroupId() );
                    n.setProperty( "artifactId", dependency.getArtifactId() );
                    n.setProperty( "version", dependency.getVersion() );
                    n.setProperty( "type", dependency.getType() );
                    n.setProperty( "classifier", dependency.getClassifier() );
                    n.setProperty( "scope", dependency.getScope() );
                    n.setProperty( "systemPath", dependency.getSystemPath() );
                    n.setProperty( "optional", dependency.isOptional() );

                    // node has no native content at this time, just facets
                    // no need to list a type as it's implied by the path. Parents are Maven specific.

                    // FIXME: add scope, systemPath, type, version, classifier & maven2 specific IDs as a facet
                    //        (should also have been added to the Dependency)

                    // TODO: add a property that is a weak reference to the originating artifact, creating it if
                    //       necessary (without adding the archiva:artifact mixin so that it doesn't get listed as an
                    //       artifact, which gives a different meaning to "incomplete" which is a known local project
                    //       that doesn't have metadata yet but has artifacts). (Though we may want to give it the
                    //       artifact mixin and another property to identify all non-local artifacts for the closure
                    //       reports)
                }
            }

            for ( MetadataFacet facet : versionMetadata.getFacetList() )
            {
                // recreate, to ensure properties are removed
                if ( versionNode.hasNode( facet.getFacetId() ) )
                {
                    versionNode.getNode( facet.getFacetId() ).remove();
                }
                Node n = versionNode.addNode( facet.getFacetId() );
                n.addMixin( FACET_NODE_TYPE );

                for ( Map.Entry<String, String> entry : facet.toProperties().entrySet() )
                {
                    n.setProperty( entry.getKey(), entry.getValue() );
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    private void updateNamespace(Session jcrSession, String repositoryId, String namespace) throws MetadataRepositoryException
    {
        try
        {
            Node node = getOrAddNamespaceNode( jcrSession, repositoryId, namespace );
            node.setProperty( "id", namespace );
            node.setProperty( "namespace", namespace );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void updateNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException
    {
        updateNamespace( getSession(session), repositoryId, namespace );
    }

    @Override
    public void removeProject( RepositorySession session, String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node root = jcrSession.getRootNode();
            String namespacePath = getNamespacePath( repositoryId, namespace );

            if ( root.hasNode( namespacePath ) )
            {
                Iterator<Node> nodeIterator = JcrUtils.getChildNodes( root.getNode( namespacePath ) ).iterator();
                while ( nodeIterator.hasNext() )
                {
                    Node node = nodeIterator.next();
                    if ( node.isNodeType( org.apache.archiva.metadata.repository.jcr.JcrConstants.PROJECT_NODE_TYPE ) && projectId.equals( node.getName() ) )
                    {
                        node.remove();
                    }
                }

            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

    }


    @Override
    public boolean hasMetadataFacet( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node node = jcrSession.getRootNode().getNode( getFacetPath( repositoryId, facetId ) );
            return node.getNodes().hasNext();
        }
        catch ( PathNotFoundException e )
        {
            // ignored - the facet doesn't exist, so return false
            return false;
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public List<String> getMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        List<String> facets = new ArrayList<>();

        try
        {
            // no need to construct node-by-node here, as we'll find in the next instance, the facet names have / and
            // are paths themselves
            Node node = jcrSession.getRootNode().getNode( getFacetPath( repositoryId, facetId ) );

            // TODO: this is a bit awkward. Might be better to review the purpose of this function - why is the list of
            //   paths helpful?
            recurse( facets, "", node );
        }
        catch ( PathNotFoundException e )
        {
            // ignored - the facet doesn't exist, so return the empty list
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        return facets;
    }

    private <T> Spliterator<T> createResultSpliterator(QueryResult result, Function<Row, T> converter) throws MetadataRepositoryException
    {
        final RowIterator rowIterator;
        try
        {
            rowIterator = result.getRows();
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage( ), e );
        }
        return new Spliterator<T>( )
        {
            @Override
            public boolean tryAdvance( Consumer<? super T> action )
            {
                while (rowIterator.hasNext()) {
                    T item = converter.apply( rowIterator.nextRow() );
                    if (item!=null)
                    {
                        action.accept( item );
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Spliterator<T> trySplit( )
            {
                return null;
            }

            @Override
            public long estimateSize( )
            {
                return 0;
            }

            @Override
            public int characteristics( )
            {
                return ORDERED+NONNULL;
            }
        };
    }

    private StringBuilder appendQueryParams(StringBuilder query, String selector, String defaultProperty, QueryParameter queryParameter) {
        if (queryParameter.getSortFields().size()==0) {
            query.append(" ORDER BY [").append(selector).append("].[").append(defaultProperty).append("]");
            if (queryParameter.isAscending()) {
                query.append(" ASC");
            } else {
                query.append(" DESC");
            }
        } else {
            query.append(" ORDER BY");
            for (String property : queryParameter.getSortFields()) {
                query.append(" [").append(selector).append("].[").append(property).append("]");
                if (queryParameter.isAscending()) {
                    query.append(" ASC");
                } else {
                    query.append(" DESC");
                }
            }
        }
        return query;
    }

    @Override
    public <T extends MetadataFacet> Stream<T> getMetadataFacetStream(RepositorySession session, String repositoryId, Class<T> facetClazz, QueryParameter queryParameter) throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        final MetadataFacetFactory<T> factory = metadataService.getFactory( facetClazz );
        final String facetId = factory.getFacetId( );
        final String facetPath = '/'+getFacetPath( repositoryId, facetId );
        StringBuilder query = new StringBuilder("SELECT * FROM [");
        query.append(FACET_NODE_TYPE).append("] AS facet WHERE ISDESCENDANTNODE(facet, [")
                .append(facetPath).append("])");
        appendQueryParams(query, "facet", "archiva:name", queryParameter);
        String q = query.toString();
        Map<String, String> params = new HashMap<>( );
        QueryResult result = runNativeJcrQuery( jcrSession, q, params, queryParameter.getOffset(), queryParameter.getLimit());
        return StreamSupport.stream( createResultSpliterator( result, (Row row)-> {
            try
            {
                Node node = row.getNode( "facet" );
                String facetName = node.getProperty( "archiva:name" ).getString();
                return createFacetFromNode( factory, node, repositoryId, facetName );
            }
            catch ( RepositoryException e )
            {
                return null;
            }
        }), false );

    }

    private void recurse( List<String> facets, String prefix, Node node )
        throws RepositoryException
    {
        for ( Node n : JcrUtils.getChildNodes( node ) )
        {
            String name = prefix + "/" + n.getName();
            if ( n.hasNodes() )
            {
                recurse( facets, name, n );
            }
            else
            {
                // strip leading / first
                facets.add( name.substring( 1 ) );
            }
        }
    }


    @Override
    public <T extends MetadataFacet> T getMetadataFacet( RepositorySession session, String repositoryId, Class<T> clazz, String name ) throws MetadataRepositoryException
    {
        if (!metadataService.supportsFacet( clazz )) {
            log.warn( "The required metadata class is not supported: " + clazz );
            return null;
        }
        final Session jcrSession = getSession( session );
        final MetadataFacetFactory<T> factory = getFacetFactory( clazz );
        final String facetId = factory.getFacetId( );
        try
        {
            Node root = jcrSession.getRootNode();
            Node node = root.getNode( getFacetPath( repositoryId, facetId, name ) );

            if ( getSupportedFacets().size()==0)
            {
                return null;
            }

            return createFacetFromNode( factory, node, repositoryId, name );
        }
        catch ( PathNotFoundException e )
        {
            // ignored - the facet doesn't exist, so return null
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        return null;
    }

    private <T extends MetadataFacet> T createFacetFromNode(final MetadataFacetFactory<T> factory, final Node node) throws RepositoryException {
        return createFacetFromNode(factory, node, null, null);
    }

    private <T extends MetadataFacet> T createFacetFromNode(final MetadataFacetFactory<T> factory, final Node node,
                                                            final String repositoryId, final String name ) throws RepositoryException
    {
        if ( factory != null )
        {
            T metadataFacet;
            if (repositoryId!=null) {
                metadataFacet = factory.createMetadataFacet( repositoryId, name );
            } else {
                metadataFacet = factory.createMetadataFacet();
            }
            Map<String, String> map = new HashMap<>();
            for ( Property property : JcrUtils.getProperties( node ) )
            {
                String p = property.getName();
                if ( !p.startsWith( "jcr:" ) )
                {
                    map.put( p, property.getString() );
                }
            }
            metadataFacet.fromProperties( map );
            return metadataFacet;
        }
        return null;
    }

    @Override
    public void addMetadataFacet( RepositorySession session, String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node repo = getOrAddRepositoryNode( jcrSession, repositoryId );
            Node facets = JcrUtils.getOrAddNode( repo, "facets" );

            String id = metadataFacet.getFacetId();
            Node facetNode = JcrUtils.getOrAddNode( facets, id );

            Node facetInstance = getOrAddNodeByPath( facetNode, metadataFacet.getName() );
            if (!facetInstance.isNodeType( FACET_NODE_TYPE ))
            {
                facetInstance.addMixin( FACET_NODE_TYPE );
                facetInstance.setProperty( "archiva:facetId", id );
                facetInstance.setProperty( "archiva:name", metadataFacet.getName( ) );
            }

            for ( Map.Entry<String, String> entry : metadataFacet.toProperties().entrySet() )
            {
                facetInstance.setProperty( entry.getKey(), entry.getValue() );
            }
            session.save();
        }
        catch ( RepositoryException | MetadataSessionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeNamespace( RepositorySession session, String repositoryId, String projectId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node root = jcrSession.getRootNode();
            String path = getNamespacePath( repositoryId, projectId );
            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );
                if ( node.isNodeType( NAMESPACE_NODE_TYPE ) )
                {
                    node.remove();
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node root = jcrSession.getRootNode();
            String path = getFacetPath( repositoryId, facetId );
            if ( root.hasNode( path ) )
            {
                root.getNode( path ).remove();
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node root = jcrSession.getRootNode();
            String path = getFacetPath( repositoryId, facetId, name );
            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );
                do
                {
                    // also remove empty container nodes
                    Node parent = node.getParent();
                    node.remove();
                    node = parent;
                }
                while ( !node.hasNodes() );
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    private StringBuilder buildArtifactByDateRangeQuery(String repoId, ZonedDateTime startTime, ZonedDateTime endTime,
                                                        QueryParameter queryParameter) {
        StringBuilder q = getArtifactQuery( repoId );

        if ( startTime != null )
        {
            q.append(" AND [artifact].[whenGathered] >= $start");
        }
        if ( endTime != null )
        {
            q.append(" AND [artifact].[whenGathered] <= $end");
        }
        appendQueryParams(q, "artifact", "whenGathered", queryParameter);
        return q;
    }

    private QueryResult queryArtifactByDateRange(Session jcrSession, String repositoryId,
                                                 ZonedDateTime startTime, ZonedDateTime endTime,
                                                 QueryParameter queryParameter) throws MetadataRepositoryException {
        String q = buildArtifactByDateRangeQuery(repositoryId, startTime, endTime, queryParameter).toString();

        try {
            Query query = jcrSession.getWorkspace().getQueryManager().createQuery(q, Query.JCR_SQL2);
            query.setOffset(queryParameter.getOffset());
            query.setLimit(queryParameter.getLimit());
            ValueFactory valueFactory = jcrSession.getValueFactory();
            if (startTime != null) {
                query.bindValue("start", valueFactory.createValue(createCalendar(startTime.withZoneSameInstant(ModelInfo.STORAGE_TZ))));
            }
            if (endTime != null) {
                query.bindValue("end", valueFactory.createValue(createCalendar(endTime.withZoneSameInstant(ModelInfo.STORAGE_TZ))));
            }
            return query.execute();
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException(e.getMessage(), e);
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange(RepositorySession session, String repoId, ZonedDateTime startTime, ZonedDateTime endTime, QueryParameter queryParameter )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );

        List<ArtifactMetadata> artifacts;
        try
        {
            QueryResult result = queryArtifactByDateRange(jcrSession, repoId, startTime, endTime, queryParameter);

            artifacts = new ArrayList<>();
            for ( Node n : JcrUtils.getNodes( result ) )
            {
                artifacts.add( getArtifactFromNode( repoId, n ) );
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        return artifacts;
    }

    @Override
    public Stream<ArtifactMetadata> getArtifactsByDateRangeStream(RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime, QueryParameter queryParameter) throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        QueryResult result = queryArtifactByDateRange(jcrSession, repositoryId, startTime, endTime, queryParameter);
        return StreamSupport.stream(createResultSpliterator(result, (row) -> {
            try {
                return getArtifactFromNode(repositoryId, row.getNode("artifact"));
            } catch (RepositoryException e) {
                return null;
            }
        }), false);
    }


    @Override
    public List<ArtifactMetadata> getArtifactsByChecksum(RepositorySession session, String repositoryId, String checksum )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        List<ArtifactMetadata> artifacts;

        String q = getArtifactQuery( repositoryId ) + " AND ([sha1] = $checksum OR [md5] = $checksum)";

        try
        {
            Query query = jcrSession.getWorkspace().getQueryManager().createQuery( q, Query.JCR_SQL2 );
            ValueFactory valueFactory = jcrSession.getValueFactory();
            query.bindValue( "checksum", valueFactory.createValue( checksum ) );
            QueryResult result = query.execute();

            artifacts = new ArrayList<>();
            for ( Node n : JcrUtils.getNodes( result ) )
            {
                artifacts.add( getArtifactFromNode( repositoryId, n ) );
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        return artifacts;
    }

    public List<ArtifactMetadata> runJcrQuery( Session jcrSession, String repositoryId, String q, Map<String, String> bindingParam)
        throws MetadataRepositoryException
    {
        return runJcrQuery( jcrSession, repositoryId, q, bindingParam, true );
    }

    public List<ArtifactMetadata> runJcrQuery( final Session jcrSession, final String repositoryId, final String qParam,
                                               final Map<String, String> bindingParam, final boolean checkPath )
        throws MetadataRepositoryException
    {

        String q = qParam;
        List<ArtifactMetadata> artifacts;
        if ( repositoryId != null && checkPath )
        {
            q += " AND ISDESCENDANTNODE(artifact,'/" + getRepositoryContentPath( repositoryId ) + "')";
        }

        log.info( "Running JCR Query: {}", q );

        try
        {
            QueryResult result = runNativeJcrQuery( jcrSession, q, bindingParam );
            artifacts = new ArrayList<>();
            RowIterator rows = result.getRows();
            while ( rows.hasNext() )
            {
                Row row = rows.nextRow();
                Node node = row.getNode( "artifact" );
                artifacts.add( getArtifactFromNode( repositoryId, node ) );
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        log.info( "Artifacts found {}", artifacts.size() );
        for ( ArtifactMetadata meta : artifacts )
        {
            log.info( "Artifact: " + meta.getVersion() + " " + meta.getFacetList() );
        }
        return artifacts;
    }

    public QueryResult runNativeJcrQuery( final Session jcrSession, final String q, final Map<String, String> bindingParam) throws MetadataRepositoryException
    {
        return runNativeJcrQuery( jcrSession, q, bindingParam, 0, Long.MAX_VALUE );
    }

    public QueryResult runNativeJcrQuery( final Session jcrSession, final String q, final Map<String, String> bindingParam, long offset, long maxEntries)
        throws MetadataRepositoryException
    {
        Map<String, String> bindings;
        if (bindingParam==null) {
            bindings = new HashMap<>( );
        } else {
            bindings = bindingParam;
        }

        try
        {
            log.debug( "Query: offset={}, limit={}, query={}", offset, maxEntries, q );
            Query query = jcrSession.getWorkspace().getQueryManager().createQuery( q, Query.JCR_SQL2 );
            query.setLimit( maxEntries );
            query.setOffset( offset );
            ValueFactory valueFactory = jcrSession.getValueFactory();
            for ( Entry<String, String> entry : bindings.entrySet() )
            {
                log.debug( "Binding: {}={}", entry.getKey( ), entry.getValue( ) );
                Value value = valueFactory.createValue( entry.getValue( ) );
                log.debug( "Binding value {}={}", entry.getKey( ), value);
                query.bindValue( entry.getKey(), value );
            }
            long start = System.currentTimeMillis( );
            log.debug( "Execute query {}", query );
            QueryResult result = query.execute();
            long end = System.currentTimeMillis( );
            log.info( "JCR Query ran in {} milliseconds: {}", end - start, q );
            return result;
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionMetadata( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        final String q = new StringBuilder( QUERY_ARTIFACTS_BY_PROJECT_VERSION_1 ).append( key ).append( QUERY_ARTIFACTS_BY_PROJECT_VERSION_2 ).toString();
        return runJcrQuery( jcrSession, repositoryId, q, ImmutableMap.of( "value", value ) );
    }


    @Override
    public List<ArtifactMetadata> getArtifactsByMetadata( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        final String q = new StringBuilder( QUERY_ARTIFACTS_BY_METADATA_1 ).append( key ).append( QUERY_ARTIFACTS_BY_METADATA_2 ).toString( );
        return runJcrQuery( jcrSession, repositoryId, q, ImmutableMap.of( "value", value ) );
    }


    @Override
    public List<ArtifactMetadata> getArtifactsByProperty( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        final String q = new StringBuilder( QUERY_ARTIFACTS_BY_PROPERTY_1 ).append( key ).append( QUERY_ARTIFACTS_BY_PROPERTY_2 ).toString();
        return runJcrQuery( jcrSession, repositoryId, q, ImmutableMap.of( "value", value ) );
    }


    @Override
    public void removeRepository( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node root = jcrSession.getRootNode();
            String path = getRepositoryPath( repositoryId );
            if ( root.hasNode( path ) )
            {
                root.getNode( path ).remove();
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifacts( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        List<ArtifactMetadata> artifacts;

        String q = getArtifactQuery( repositoryId ).toString();

        try
        {
            Query query = jcrSession.getWorkspace().getQueryManager().createQuery( q, Query.JCR_SQL2 );
            QueryResult result = query.execute();

            artifacts = new ArrayList<>();
            for ( Node n : JcrUtils.getNodes( result ) )
            {
                if ( n.isNodeType( ARTIFACT_NODE_TYPE ) )
                {
                    artifacts.add( getArtifactFromNode( repositoryId, n ) );
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        return artifacts;
    }

    private static StringBuilder getArtifactQuery( String repositoryId )
    {
        return new StringBuilder(QUERY_ARTIFACT_1).append(getRepositoryContentPath( repositoryId )).append(QUERY_ARTIFACT_2);
    }

    @Override
    public ProjectMetadata getProject( RepositorySession session, String repositoryId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        final Session jcrSession;
        try
        {
            jcrSession = getSession( session );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage() );
        }
        ProjectMetadata metadata = null;

        try
        {
            Node root = jcrSession.getRootNode();

            // basically just checking it exists
            String path = getProjectPath( repositoryId, namespace, projectId );
            if ( root.hasNode( path ) )
            {
                metadata = new ProjectMetadata();
                metadata.setId( projectId );
                metadata.setNamespace( namespace );
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return metadata;
    }

    @Override
    public ProjectVersionMetadata getProjectVersion( RepositorySession session, String repositoryId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolutionException
    {
        final Session jcrSession;
        try
        {
            jcrSession = getSession( session );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage() );
        }
        ProjectVersionMetadata versionMetadata;

        try
        {
            Node root = jcrSession.getRootNode();

            String path = getProjectVersionPath( repositoryId, namespace, projectId, projectVersion );
            if ( !root.hasNode( path ) )
            {
                return null;
            }

            Node node = root.getNode( path );

            versionMetadata = new ProjectVersionMetadata();
            versionMetadata.setId( projectVersion );
            versionMetadata.setName( getPropertyString( node, "name" ) );
            versionMetadata.setDescription( getPropertyString( node, "description" ) );
            versionMetadata.setUrl( getPropertyString( node, "url" ) );
            versionMetadata.setIncomplete(
                node.hasProperty( "incomplete" ) && node.getProperty( "incomplete" ).getBoolean() );

            // FIXME: decide how to treat these in the content repo
            String scmConnection = getPropertyString( node, "scm.connection" );
            String scmDeveloperConnection = getPropertyString( node, "scm.developerConnection" );
            String scmUrl = getPropertyString( node, "scm.url" );
            if ( scmConnection != null || scmDeveloperConnection != null || scmUrl != null )
            {
                Scm scm = new Scm();
                scm.setConnection( scmConnection );
                scm.setDeveloperConnection( scmDeveloperConnection );
                scm.setUrl( scmUrl );
                versionMetadata.setScm( scm );
            }

            String ciSystem = getPropertyString( node, "ci.system" );
            String ciUrl = getPropertyString( node, "ci.url" );
            if ( ciSystem != null || ciUrl != null )
            {
                CiManagement ci = new CiManagement();
                ci.setSystem( ciSystem );
                ci.setUrl( ciUrl );
                versionMetadata.setCiManagement( ci );
            }

            String issueSystem = getPropertyString( node, "issue.system" );
            String issueUrl = getPropertyString( node, "issue.url" );
            if ( issueSystem != null || issueUrl != null )
            {
                IssueManagement issueManagement = new IssueManagement();
                issueManagement.setSystem( issueSystem );
                issueManagement.setUrl( issueUrl );
                versionMetadata.setIssueManagement( issueManagement );
            }

            String orgName = getPropertyString( node, "org.name" );
            String orgUrl = getPropertyString( node, "org.url" );
            if ( orgName != null || orgUrl != null )
            {
                Organization org = new Organization();
                org.setName( orgName );
                org.setUrl( orgUrl );
                versionMetadata.setOrganization( org );
            }

            boolean done = false;
            int i = 0;
            while ( !done )
            {
                String licenseName = getPropertyString( node, "license." + i + ".name" );
                String licenseUrl = getPropertyString( node, "license." + i + ".url" );
                if ( licenseName != null || licenseUrl != null )
                {
                    License license = new License();
                    license.setName( licenseName );
                    license.setUrl( licenseUrl );
                    versionMetadata.addLicense( license );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            done = false;
            i = 0;
            while ( !done )
            {
                String mailingListName = getPropertyString( node, "mailingList." + i + ".name" );
                if ( mailingListName != null )
                {
                    MailingList mailingList = new MailingList();
                    mailingList.setName( mailingListName );
                    mailingList.setMainArchiveUrl( getPropertyString( node, "mailingList." + i + ".archive" ) );
                    String n = "mailingList." + i + ".otherArchives";
                    if ( node.hasProperty( n ) )
                    {
                        mailingList.setOtherArchives( Arrays.asList( getPropertyString( node, n ).split( "," ) ) );
                    }
                    else
                    {
                        mailingList.setOtherArchives( Collections.<String>emptyList() );
                    }
                    mailingList.setPostAddress( getPropertyString( node, "mailingList." + i + ".post" ) );
                    mailingList.setSubscribeAddress( getPropertyString( node, "mailingList." + i + ".subscribe" ) );
                    mailingList.setUnsubscribeAddress( getPropertyString( node, "mailingList." + i + ".unsubscribe" ) );
                    versionMetadata.addMailingList( mailingList );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            if ( node.hasNode( "dependencies" ) )
            {
                Node dependenciesNode = node.getNode( "dependencies" );
                for ( Node n : JcrUtils.getChildNodes( dependenciesNode ) )
                {
                    if ( n.isNodeType( DEPENDENCY_NODE_TYPE ) )
                    {
                        Dependency dependency = new Dependency();
                        // FIXME: correct these properties
                        dependency.setArtifactId( getPropertyString( n, "artifactId" ) );
                        dependency.setGroupId( getPropertyString( n, "groupId" ) );
                        dependency.setClassifier( getPropertyString( n, "classifier" ) );
                        dependency.setOptional( Boolean.valueOf( getPropertyString( n, "optional" ) ) );
                        dependency.setScope( getPropertyString( n, "scope" ) );
                        dependency.setSystemPath( getPropertyString( n, "systemPath" ) );
                        dependency.setType( getPropertyString( n, "type" ) );
                        dependency.setVersion( getPropertyString( n, "version" ) );
                        versionMetadata.addDependency( dependency );
                    }
                }
            }

            retrieveFacetProperties( versionMetadata, node );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return versionMetadata;
    }

    private void retrieveFacetProperties( FacetedMetadata metadata, Node node ) throws RepositoryException
    {
        for ( Node n : JcrUtils.getChildNodes( node ) )
        {
            if ( n.isNodeType( FACET_NODE_TYPE ) )
            {
                String name = n.getName();
                MetadataFacetFactory factory = metadataService.getFactory( name );
                if ( factory == null )
                {
                    log.error( "Attempted to load unknown project version metadata facet: {}", name );
                }
                else
                {
                    MetadataFacet facet = createFacetFromNode(factory, n);
                    metadata.addFacet( facet );
                }
            }
        }
    }

    @Override
    public Collection<String> getArtifactVersions( RepositorySession session, String repositoryId, String namespace, String projectId,
                                                   String projectVersion )
        throws MetadataResolutionException
    {
        final Session jcrSession;
        try
        {
            jcrSession = getSession( session );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage() );
        }
        Set<String> versions = new LinkedHashSet<String>();

        try
        {
            Node root = jcrSession.getRootNode();

            Node node = root.getNode( getProjectVersionPath( repositoryId, namespace, projectId, projectVersion ) );

            for ( Node n : JcrUtils.getChildNodes( node ) )
            {
                versions.add( n.getProperty( "version" ).getString() );
            }
        }
        catch ( PathNotFoundException e )
        {
            // ignore repo not found for now
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return versions;
    }

    @Override
    public Collection<ProjectVersionReference> getProjectReferences( RepositorySession session, String repositoryId, String namespace,
                                                                     String projectId, String projectVersion )
        throws MetadataResolutionException
    {
        final Session jcrSession;
        try
        {
            jcrSession = getSession( session );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage() );
        }

        List<ProjectVersionReference> references = new ArrayList<>();

        // TODO: bind variables instead
        String q = "SELECT * FROM [archiva:dependency] WHERE ISDESCENDANTNODE([/repositories/" + repositoryId
            + "/content]) AND [groupId]='" + namespace + "' AND [artifactId]='" + projectId + "'";
        if ( projectVersion != null )
        {
            q += " AND [version]='" + projectVersion + "'";
        }
        try
        {
            Query query = jcrSession.getWorkspace().getQueryManager().createQuery( q, Query.JCR_SQL2 );
            QueryResult result = query.execute();

            for ( Node n : JcrUtils.getNodes( result ) )
            {
                n = n.getParent(); // dependencies grouping element

                n = n.getParent(); // project version
                String usedByProjectVersion = n.getName();

                n = n.getParent(); // project
                String usedByProject = n.getName();

                n = n.getParent(); // namespace
                String usedByNamespace = n.getProperty( "namespace" ).getString();

                ProjectVersionReference ref = new ProjectVersionReference();
                ref.setNamespace( usedByNamespace );
                ref.setProjectId( usedByProject );
                ref.setProjectVersion( usedByProjectVersion );
                ref.setReferenceType( ProjectVersionReference.ReferenceType.DEPENDENCY );
                references.add( ref );
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return references;
    }

    @Override
    public Collection<String> getRootNamespaces( RepositorySession session, String repositoryId )
        throws MetadataResolutionException
    {
        return getNamespaces(session , repositoryId, null );
    }

    @Override
    public Collection<String> getNamespaces( RepositorySession session, String repositoryId, String baseNamespace )
        throws MetadataResolutionException
    {
        String path = baseNamespace != null
            ? getNamespacePath( repositoryId, baseNamespace )
            : getRepositoryContentPath( repositoryId );

        try
        {
            return getNodeNames( getSession(session), path, NAMESPACE_NODE_TYPE );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage( ) );
        }
    }

    @Override
    public Collection<String> getProjects( RepositorySession session, String repositoryId, String namespace )
        throws MetadataResolutionException
    {
        try
        {
            return getNodeNames( getSession(session), getNamespacePath( repositoryId, namespace ), org.apache.archiva.metadata.repository.jcr.JcrConstants.PROJECT_NODE_TYPE );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage( ) );
        }
    }

    @Override
    public Collection<String> getProjectVersions( RepositorySession session, String repositoryId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        try
        {
            return getNodeNames( getSession(session), getProjectPath( repositoryId, namespace, projectId ), PROJECT_VERSION_NODE_TYPE );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage( ) );
        }
    }

    @Override
    public void removeArtifact( RepositorySession session, ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        String repositoryId = artifactMetadata.getRepositoryId();

        try
        {
            Node root = jcrSession.getRootNode();
            String path =
                getProjectVersionPath( repositoryId, artifactMetadata.getNamespace(), artifactMetadata.getProject(),
                                       baseVersion );

            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );

                for ( Node n : JcrUtils.getChildNodes( node ) )
                {
                    if ( n.isNodeType( ARTIFACT_NODE_TYPE ) )
                    {
                        if ( n.hasProperty( "version" ) )
                        {
                            String version = n.getProperty( "version" ).getString();
                            if ( StringUtils.equals( version, artifactMetadata.getVersion() ) )
                            {
                                n.remove();
                            }
                        }

                    }
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }


    }


    @Override
    public void removeProjectVersion( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {

            String path = getProjectPath( repoId, namespace, projectId );
            Node root = jcrSession.getRootNode();

            Node nodeAtPath = root.getNode( path );

            for ( Node node : JcrUtils.getChildNodes( nodeAtPath ) )
            {
                if ( node.isNodeType( PROJECT_VERSION_NODE_TYPE ) && StringUtils.equals( projectVersion,
                                                                                         node.getName() ) )
                {
                    node.remove();
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeArtifact( RepositorySession session, String repositoryId, String namespace, String projectId, String projectVersion,
                                String id )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node root = jcrSession.getRootNode();
            String path = getArtifactPath( repositoryId, namespace, projectId, projectVersion, id );
            if ( root.hasNode( path ) )
            {
                root.getNode( path ).remove();
            }

            // remove version

            path = getProjectPath( repositoryId, namespace, projectId );

            Node nodeAtPath = root.getNode( path );

            for ( Node node : JcrUtils.getChildNodes( nodeAtPath ) )
            {
                if ( node.isNodeType( PROJECT_VERSION_NODE_TYPE ) //
                    && StringUtils.equals( node.getName(), projectVersion ) )
                {
                    node.remove();
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public void removeArtifact( RepositorySession session, String repositoryId, String namespace, String project, String projectVersion,
                                MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        try
        {
            Node root = jcrSession.getRootNode();
            String path = getProjectVersionPath( repositoryId, namespace, project, projectVersion );

            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );

                for ( Node n : JcrUtils.getChildNodes( node ) )
                {
                    if ( n.isNodeType( ARTIFACT_NODE_TYPE ) )
                    {
                        ArtifactMetadata artifactMetadata = getArtifactFromNode( repositoryId, n );
                        log.debug( "artifactMetadata: {}", artifactMetadata );
                        MetadataFacet metadataFacetToRemove = artifactMetadata.getFacet( metadataFacet.getFacetId() );
                        if ( metadataFacetToRemove != null && metadataFacet.equals( metadataFacetToRemove ) )
                        {
                            n.remove();
                        }
                    }
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( RepositorySession session, String repositoryId, String namespace, String projectId,
                                                      String projectVersion )
        throws MetadataResolutionException
    {
        final Session jcrSession;
        try
        {
            jcrSession = getSession( session );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage( ) );
        }
        List<ArtifactMetadata> artifacts = new ArrayList<>();

        try
        {
            Node root = jcrSession.getRootNode();
            String path = getProjectVersionPath( repositoryId, namespace, projectId, projectVersion );

            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );

                for ( Node n : JcrUtils.getChildNodes( node ) )
                {
                    if ( n.isNodeType( ARTIFACT_NODE_TYPE ) )
                    {
                        artifacts.add( getArtifactFromNode( repositoryId, n ) );
                    }
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return artifacts;
    }


    @Override
    public boolean canObtainAccess( Class<?> aClass )
    {
        return aClass == Session.class;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T obtainAccess( RepositorySession session, Class<T> aClass )
        throws MetadataRepositoryException
    {
        if ( aClass == Session.class )
        {
            return (T) getSession( session );
        }
        throw new IllegalArgumentException(
            "Access using " + aClass + " is not supported on the JCR metadata storage" );
    }

    @Override
    public void close()
        throws MetadataRepositoryException
    {
    }


    /**
     * Exact is ignored as we can't do exact search in any property, we need a key
     */
    @Override
    public List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String text, boolean exact )
        throws MetadataRepositoryException
    {
        return searchArtifacts( session, repositoryId, null, text, exact );
    }

    @Override
    public List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String key, String text, boolean exact )
        throws MetadataRepositoryException
    {
        final Session jcrSession = getSession( session );
        String theKey = key == null ? "*" : "[" + key + "]";
        String projectVersionCondition =
            exact ? "(projectVersion." + theKey + " = $value)" : "contains([projectVersion]." + theKey + ", $value)";
        String facetCondition = exact ? "(facet." + theKey + " = $value)" : "contains([facet]." + theKey + ", $value)";
        String descendantCondition = repositoryId == null ?
            " AND [projectVersion].[jcr:path] LIKE '/repositories/%/content/%'" :
            " AND ISDESCENDANTNODE(projectVersion,'/" + getRepositoryContentPath( repositoryId ) + "')";
        List<ArtifactMetadata> result = new ArrayList<>( );
        if (key==null || (key!=null && Arrays.binarySearch( PROJECT_VERSION_VERSION_PROPERTIES, key )>=0))
        {
            // We search only for project version properties if the key is a valid property name
            String q1 =
                "SELECT * FROM [" + PROJECT_VERSION_NODE_TYPE
                    + "] AS projectVersion LEFT OUTER JOIN [" + ARTIFACT_NODE_TYPE
                    + "] AS artifact ON ISCHILDNODE(artifact, projectVersion) WHERE " + projectVersionCondition + descendantCondition;
             result.addAll(runJcrQuery( jcrSession, repositoryId, q1, ImmutableMap.of( "value", text ), false ));
        }
        String q2 =
            "SELECT * FROM [" + PROJECT_VERSION_NODE_TYPE
                + "] AS projectVersion LEFT OUTER JOIN [" + ARTIFACT_NODE_TYPE
                + "] AS artifact ON ISCHILDNODE(artifact, projectVersion) LEFT OUTER JOIN [" + FACET_NODE_TYPE
                + "] AS facet ON ISCHILDNODE(facet, projectVersion) WHERE " + facetCondition + descendantCondition;
        result.addAll( runJcrQuery( jcrSession, repositoryId, q2, ImmutableMap.of( "value", text ), false ) );
        return result;
    }

    private ArtifactMetadata getArtifactFromNode( String repositoryId, Node artifactNode )
        throws RepositoryException
    {
        String id = artifactNode.getName();

        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setId( id );
        artifact.setRepositoryId( repositoryId == null ? artifactNode.getAncestor( 2 ).getName() : repositoryId );

        Node projectVersionNode = artifactNode.getParent();
        Node projectNode = projectVersionNode.getParent();
        Node namespaceNode = projectNode.getParent();

        artifact.setNamespace( namespaceNode.getProperty( "namespace" ).getString() );
        artifact.setProject( projectNode.getName() );
        artifact.setProjectVersion( projectVersionNode.getName() );
        artifact.setVersion( artifactNode.hasProperty( "version" )
                                 ? artifactNode.getProperty( "version" ).getString()
                                 : projectVersionNode.getName() );

        if ( artifactNode.hasProperty( JCR_LAST_MODIFIED ) )
        {
            artifact.setFileLastModified( artifactNode.getProperty( JCR_LAST_MODIFIED ).getDate().getTimeInMillis() );
        }

        if ( artifactNode.hasProperty( "whenGathered" ) )
        {
            Calendar cal = artifactNode.getProperty("whenGathered").getDate();
            artifact.setWhenGathered( ZonedDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId()));
        }

        if ( artifactNode.hasProperty( "size" ) )
        {
            artifact.setSize( artifactNode.getProperty( "size" ).getLong() );
        }

        if ( artifactNode.hasProperty( "md5" ) )
        {
            artifact.setMd5( artifactNode.getProperty( "md5" ).getString() );
        }

        if ( artifactNode.hasProperty( "sha1" ) )
        {
            artifact.setSha1( artifactNode.getProperty( "sha1" ).getString() );
        }

        retrieveFacetProperties( artifact, artifactNode );
        return artifact;
    }

    private static String getPropertyString( Node node, String name )
        throws RepositoryException
    {
        return node.hasProperty( name ) ? node.getProperty( name ).getString() : null;
    }

    private Collection<String> getNodeNames( Session jcrSession, String path, String nodeType )
        throws MetadataResolutionException
    {

        List<String> names = new ArrayList<>();

        try
        {
            Node root = jcrSession.getRootNode();

            Node nodeAtPath = root.getNode( path );

            for ( Node node : JcrUtils.getChildNodes( nodeAtPath ) )
            {
                if ( node.isNodeType( nodeType ) )
                {
                    names.add( node.getName() );
                }
            }
        }
        catch ( PathNotFoundException e )
        {
            // ignore repo not found for now
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return names;
    }

    private static String getRepositoryPath( String repositoryId )
    {
        return "repositories/" + repositoryId;
    }

    private static String getRepositoryContentPath( String repositoryId )
    {
        return getRepositoryPath( repositoryId ) + "/content";
    }

    private static String getFacetPath( String repositoryId, String facetId )
    {
        return StringUtils.isEmpty( facetId ) ? getRepositoryPath( repositoryId ) + "/facets" :
            getRepositoryPath( repositoryId ) + "/facets/" + facetId;
    }

    private static String getNamespacePath( String repositoryId, String namespace )
    {
        return getRepositoryContentPath( repositoryId ) + "/" + namespace.replace( '.', '/' );
    }

    private static String getProjectPath( String repositoryId, String namespace, String projectId )
    {
        return getNamespacePath( repositoryId, namespace ) + "/" + projectId;
    }

    private static String getProjectVersionPath( String repositoryId, String namespace, String projectId,
                                                 String projectVersion )
    {
        return getProjectPath( repositoryId, namespace, projectId ) + "/" + projectVersion;
    }

    private static String getArtifactPath( String repositoryId, String namespace, String projectId,
                                           String projectVersion, String id )
    {
        return getProjectVersionPath( repositoryId, namespace, projectId, projectVersion ) + "/" + id;
    }

    private Node getOrAddNodeByPath( Node baseNode, String name )
        throws RepositoryException
    {
        return getOrAddNodeByPath( baseNode, name, null );
    }

    private Node getOrAddNodeByPath( Node baseNode, String name, String nodeType )
        throws RepositoryException
    {
        log.debug( "getOrAddNodeByPath " + baseNode + " " + name + " " + nodeType );
        Node node = baseNode;
        for ( String n : name.split( "/" ) )
        {
            node = JcrUtils.getOrAddNode( node, n );
            if ( nodeType != null && !node.isNodeType( nodeType ))
            {
                node.addMixin( nodeType );
            }
            if (!node.hasProperty( "id" )) {
                node.setProperty( "id", n );
            }
        }
        return node;
    }

    private static String getFacetPath( String repositoryId, String facetId, String name )
    {
        return getFacetPath( repositoryId, facetId ) + "/" + name;
    }

    private Node getOrAddRepositoryNode( Session jcrSession, String repositoryId )
        throws RepositoryException
    {
        log.debug( "getOrAddRepositoryNode " + repositoryId );
        Node root = jcrSession.getRootNode();
        Node node = JcrUtils.getOrAddNode( root, "repositories" );
        log.debug( "Repositories " + node );
        node = JcrUtils.getOrAddNode( node, repositoryId, JcrConstants.NT_UNSTRUCTURED );
        if (!node.isNodeType( org.apache.archiva.metadata.repository.jcr.JcrConstants.REPOSITORY_NODE_TYPE )) {
            node.addMixin( org.apache.archiva.metadata.repository.jcr.JcrConstants.REPOSITORY_NODE_TYPE );
        }
        if (!node.hasProperty( "id" )) {
            node.setProperty( "id", repositoryId );
        }
        return node;
    }

    private Node getOrAddRepositoryContentNode( Session jcrSession, String repositoryId )
        throws RepositoryException
    {
        Node node = getOrAddRepositoryNode( jcrSession, repositoryId );
        return JcrUtils.getOrAddNode( node, "content" );
    }

    private Node getOrAddNamespaceNode( Session jcrSession, String repositoryId, String namespace )
        throws RepositoryException
    {
        Node repo = getOrAddRepositoryContentNode( jcrSession, repositoryId );
        return getOrAddNodeByPath( repo, namespace.replace( '.', '/' ), NAMESPACE_NODE_TYPE );
    }

    private Node getOrAddProjectNode( Session jcrSession, String repositoryId, String namespace, String projectId )
        throws RepositoryException
    {
        Node namespaceNode = getOrAddNamespaceNode( jcrSession, repositoryId, namespace );
        Node node = JcrUtils.getOrAddNode( namespaceNode, projectId );
        if (!node.isNodeType( org.apache.archiva.metadata.repository.jcr.JcrConstants.PROJECT_NODE_TYPE ))
        {
            node.addMixin( org.apache.archiva.metadata.repository.jcr.JcrConstants.PROJECT_NODE_TYPE );
        }
        if (!node.hasProperty( "id" ))
        {
            node.setProperty( "id", projectId );
        }
        return node;
    }

    private Node getOrAddProjectVersionNode( Session jcrSession, String repositoryId, String namespace, String projectId,
                                             String projectVersion )
        throws RepositoryException
    {
        Node projectNode = getOrAddProjectNode( jcrSession, repositoryId, namespace, projectId );
        log.debug( "Project node {}", projectNode );
        Node projectVersionNode = JcrUtils.getOrAddNode( projectNode, projectVersion, JcrConstants.NT_UNSTRUCTURED);
        if (!projectVersionNode.isNodeType( PROJECT_VERSION_NODE_TYPE ))
        {
            projectVersionNode.addMixin( PROJECT_VERSION_NODE_TYPE );
        }
        if (!projectVersionNode.hasProperty( "id" ))
        {
            projectVersionNode.setProperty( "id", projectVersion );
        }

        log.debug( "Project version node {}", projectVersionNode );
        return projectVersionNode;
    }

    private Node getOrAddArtifactNode( Session jcrSession, String repositoryId, String namespace, String projectId, String projectVersion,
                                       String id )
        throws RepositoryException
    {
        Node versionNode = getOrAddProjectVersionNode( jcrSession, repositoryId, namespace, projectId, projectVersion );
        Node node = JcrUtils.getOrAddNode( versionNode, id);
        if (!node.isNodeType( ARTIFACT_NODE_TYPE ))
        {
            node.addMixin( ARTIFACT_NODE_TYPE );
        }
        if (!node.hasProperty( "id" )) {
            node.setProperty( "id", id );
        }
        return node;
    }

    private static Calendar createCalendar( ZonedDateTime time )
    {
        return GregorianCalendar.from(time);
    }

    private String join( Collection<String> ids )
    {
        if ( ids != null && !ids.isEmpty() )
        {
            StringBuilder s = new StringBuilder();
            for ( String id : ids )
            {
                s.append( id );
                s.append( "," );
            }
            return s.substring( 0, s.length() - 1 );
        }
        return null;
    }


    @Override
    public void populateStatistics( RepositorySession repositorySession, MetadataRepository repository, String repositoryId,
                                    RepositoryStatistics repositoryStatistics )
        throws MetadataRepositoryException
    {
        if ( !( repository instanceof JcrMetadataRepository ) )
        {
            throw new MetadataRepositoryException(
                "The statistics population is only possible for JcrMetdataRepository implementations" );
        }
        Session session = getSession( repositorySession );
        // TODO: these may be best as running totals, maintained by observations on the properties in JCR

        try
        {
            QueryManager queryManager = session.getWorkspace().getQueryManager();

            // TODO: Check, if this is still the case - Switched to Jackrabbit OAK with archiva 3.0
            // Former statement: JCR-SQL2 query will not complete on a large repo in Jackrabbit 2.2.0 - see JCR-2835
            //    Using the JCR-SQL2 variants gives
            //      "org.apache.lucene.search.BooleanQuery$TooManyClauses: maxClauseCount is set to 1024"
//            String whereClause = "WHERE ISDESCENDANTNODE([/repositories/" + repositoryId + "/content])";
//            Query query = queryManager.createQuery( "SELECT size FROM [archiva:artifact] " + whereClause,
//                                                    Query.JCR_SQL2 );
            String whereClause = "WHERE ISDESCENDANTNODE([/repositories/" + repositoryId + "/content])";
            Query query = queryManager.createQuery( "SELECT size FROM [archiva:artifact] " + whereClause, Query.JCR_SQL2 );

            QueryResult queryResult = query.execute();

            Map<String, Integer> totalByType = new HashMap<>();
            long totalSize = 0, totalArtifacts = 0;
            for ( Row row : JcrUtils.getRows( queryResult ) )
            {
                Node n = row.getNode();
                totalSize += row.getValue( "size" ).getLong();

                String type;
                if ( n.hasNode( MavenArtifactFacet.FACET_ID ) )
                {
                    Node facetNode = n.getNode( MavenArtifactFacet.FACET_ID );
                    type = facetNode.getProperty( "type" ).getString();
                }
                else
                {
                    type = "Other";
                }
                Integer prev = totalByType.get( type );
                totalByType.put( type, prev != null ? prev + 1 : 1 );

                totalArtifacts++;
            }

            repositoryStatistics.setTotalArtifactCount( totalArtifacts );
            repositoryStatistics.setTotalArtifactFileSize( totalSize );
            for ( Map.Entry<String, Integer> entry : totalByType.entrySet() )
            {
                log.info( "Setting count for type: {} = {}", entry.getKey(), entry.getValue() );
                repositoryStatistics.setTotalCountForType( entry.getKey(), entry.getValue() );
            }

            // The query ordering is a trick to ensure that the size is correct, otherwise due to lazy init it will be -1
//            query = queryManager.createQuery( "SELECT * FROM [archiva:project] " + whereClause, Query.JCR_SQL2 );
            query = queryManager.createQuery( "SELECT * FROM [archiva:project] " + whereClause + " ORDER BY [jcr:score]",
                                              Query.JCR_SQL2 );
            repositoryStatistics.setTotalProjectCount( query.execute().getRows().getSize() );

//            query = queryManager.createQuery(
//                "SELECT * FROM [archiva:namespace] " + whereClause + " AND namespace IS NOT NULL", Query.JCR_SQL2 );
            query = queryManager.createQuery(
                "SELECT * FROM [archiva:namespace] " + whereClause + " AND namespace IS NOT NULL ORDER BY [jcr:score]",
                Query.JCR_SQL2 );
            repositoryStatistics.setTotalGroupCount( query.execute().getRows().getSize() );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }


    public Session login() throws RepositoryException
    {
        return repository.login(new SimpleCredentials( "admin", "admin".toCharArray() ) );
    }
}
