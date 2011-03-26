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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.jackrabbit.commons.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * @todo below: revise storage format for project version metadata
 * @todo revise reference storage
 */
public class JcrMetadataRepository
    implements MetadataRepository
{
    private static final String JCR_LAST_MODIFIED = "jcr:lastModified";

    static final String NAMESPACE_NODE_TYPE = "archiva:namespace";

    static final String PROJECT_NODE_TYPE = "archiva:project";

    static final String PROJECT_VERSION_NODE_TYPE = "archiva:projectVersion";

    static final String ARTIFACT_NODE_TYPE = "archiva:artifact";

    static final String FACET_NODE_TYPE = "archiva:facet";

    private static final String DEPENDENCY_NODE_TYPE = "archiva:dependency";

    private final Map<String, MetadataFacetFactory> metadataFacetFactories;

    private static final Logger log = LoggerFactory.getLogger( JcrMetadataRepository.class );

    private Repository repository;

    private Session session;

    public JcrMetadataRepository( Map<String, MetadataFacetFactory> metadataFacetFactories, Repository repository )
        throws RepositoryException
    {
        this.metadataFacetFactories = metadataFacetFactories;
        this.repository = repository;

        session = repository.login( new SimpleCredentials( "admin", "admin".toCharArray() ) );
    }

    static void initialize( Session session )
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

        NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();
        registerMixinNodeType( nodeTypeManager, JcrMetadataRepository.NAMESPACE_NODE_TYPE );
        registerMixinNodeType( nodeTypeManager, JcrMetadataRepository.PROJECT_NODE_TYPE );
        registerMixinNodeType( nodeTypeManager, JcrMetadataRepository.PROJECT_VERSION_NODE_TYPE );
        registerMixinNodeType( nodeTypeManager, JcrMetadataRepository.ARTIFACT_NODE_TYPE );
        registerMixinNodeType( nodeTypeManager, JcrMetadataRepository.FACET_NODE_TYPE );
        registerMixinNodeType( nodeTypeManager, JcrMetadataRepository.DEPENDENCY_NODE_TYPE );
    }

    private static void registerMixinNodeType( NodeTypeManager nodeTypeManager, String name )
        throws RepositoryException
    {
        NodeTypeTemplate nodeType = nodeTypeManager.createNodeTypeTemplate();
        nodeType.setMixin( true );
        nodeType.setName( name );

        // for now just don't re-create - but in future if we change the definition, make sure to remove first as an
        // upgrade path
        if ( !nodeTypeManager.hasNodeType( name ) )
        {
            nodeTypeManager.registerNodeType( nodeType, false );
        }
    }

    public void updateProject( String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException
    {
        updateProject( repositoryId, project.getNamespace(), project.getId() );
    }

    private void updateProject( String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException
    {
        updateNamespace( repositoryId, namespace );

        try
        {
            getOrAddProjectNode( repositoryId, namespace, projectId );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    public void updateArtifact( String repositoryId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException
    {
        updateNamespace( repositoryId, namespace );

        try
        {
            Node node = getOrAddArtifactNode( repositoryId, namespace, projectId, projectVersion,
                                              artifactMeta.getId() );

            Calendar cal = Calendar.getInstance();
            cal.setTime( artifactMeta.getFileLastModified() );
            node.setProperty( JCR_LAST_MODIFIED, cal );

            cal = Calendar.getInstance();
            cal.setTime( artifactMeta.getWhenGathered() );
            node.setProperty( "whenGathered", cal );

            node.setProperty( "size", artifactMeta.getSize() );
            node.setProperty( "md5", artifactMeta.getMd5() );
            node.setProperty( "sha1", artifactMeta.getSha1() );

            node.setProperty( "version", artifactMeta.getVersion() );

            for ( MetadataFacet facet : artifactMeta.getFacetList() )
            {
                if ( node.hasNode( facet.getFacetId() ) )
                {
                    node.getNode( facet.getFacetId() ).remove();
                }

                // recreate, to ensure properties are removed
                Node n = node.addNode( facet.getFacetId() );
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

    public void updateProjectVersion( String repositoryId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException
    {
        updateProject( repositoryId, namespace, projectId );

        try
        {
            Node versionNode = getOrAddProjectVersionNode( repositoryId, namespace, projectId,
                                                           versionMetadata.getId() );

            versionNode.setProperty( "name", versionMetadata.getName() );
            versionNode.setProperty( "description", versionMetadata.getDescription() );
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
                versionNode.setProperty( "mailingList." + i + ".otherArchives", join(
                    mailingList.getOtherArchives() ) );
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

                    Node n = JcrUtils.getOrAddNode( dependenciesNode, id );
                    n.addMixin( DEPENDENCY_NODE_TYPE );

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

    // FIXME: remove this and projectversionreference
    public void updateProjectReference( String repositoryId, String namespace, String projectId, String projectVersion,
                                        ProjectVersionReference reference )
        throws MetadataRepositoryException
    {
        // not using weak references, since they still need to exist upfront to be referred to
        try
        {
            Node node = getOrAddRepositoryContentNode( repositoryId );
            node = JcrUtils.getOrAddNode( node, namespace );
            node = JcrUtils.getOrAddNode( node, projectId );
            node = JcrUtils.getOrAddNode( node, projectVersion );
            node = JcrUtils.getOrAddNode( node, "references" );
            node = JcrUtils.getOrAddNode( node, reference.getNamespace() );
            node = JcrUtils.getOrAddNode( node, reference.getProjectId() );
            node = JcrUtils.getOrAddNode( node, reference.getProjectVersion() );
            node.setProperty( "type", reference.getReferenceType().toString() );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    public void updateNamespace( String repositoryId, String namespace )
        throws MetadataRepositoryException
    {
        try
        {
            Node node = getOrAddNamespaceNode( repositoryId, namespace );
            node.setProperty( "namespace", namespace );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    public List<String> getMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        List<String> facets = new ArrayList<String>();

        try
        {
            // no need to construct node-by-node here, as we'll find in the next instance, the facet names have / and
            // are paths themselves
            Node node = session.getRootNode().getNode( getFacetPath( repositoryId, facetId ) );

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

    public MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException
    {
        MetadataFacet metadataFacet = null;
        try
        {
            Node root = session.getRootNode();
            Node node = root.getNode( getFacetPath( repositoryId, facetId, name ) );

            MetadataFacetFactory metadataFacetFactory = metadataFacetFactories.get( facetId );
            if ( metadataFacetFactory != null )
            {
                metadataFacet = metadataFacetFactory.createMetadataFacet( repositoryId, name );
                Map<String, String> map = new HashMap<String, String>();
                for ( Property property : JcrUtils.getProperties( node ) )
                {
                    String p = property.getName();
                    if ( !p.startsWith( "jcr:" ) )
                    {
                        map.put( p, property.getString() );
                    }
                }
                metadataFacet.fromProperties( map );
            }
        }
        catch ( PathNotFoundException e )
        {
            // ignored - the facet doesn't exist, so return null
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        return metadataFacet;
    }

    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        try
        {
            Node repo = getOrAddRepositoryNode( repositoryId );
            Node facets = JcrUtils.getOrAddNode( repo, "facets" );

            String id = metadataFacet.getFacetId();
            Node facetNode = JcrUtils.getOrAddNode( facets, id );

            Node node = getOrAddNodeByPath( facetNode, metadataFacet.getName() );

            for ( Map.Entry<String, String> entry : metadataFacet.toProperties().entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    public void removeMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        try
        {
            Node root = session.getRootNode();
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

    public void removeMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException
    {
        try
        {
            Node root = session.getRootNode();
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

    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
        throws MetadataRepositoryException
    {
        List<ArtifactMetadata> artifacts;

        String q = getArtifactQuery( repoId );

        if ( startTime != null )
        {
            q += " AND [whenGathered] >= $start";
        }
        if ( endTime != null )
        {
            q += " AND [whenGathered] <= $end";
        }

        try
        {
            Query query = session.getWorkspace().getQueryManager().createQuery( q, Query.JCR_SQL2 );
            ValueFactory valueFactory = session.getValueFactory();
            if ( startTime != null )
            {
                query.bindValue( "start", valueFactory.createValue( createCalendar( startTime ) ) );
            }
            if ( endTime != null )
            {
                query.bindValue( "end", valueFactory.createValue( createCalendar( endTime ) ) );
            }
            QueryResult result = query.execute();

            artifacts = new ArrayList<ArtifactMetadata>();
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

    public Collection<String> getRepositories()
        throws MetadataRepositoryException
    {
        List<String> repositories;

        try
        {
            Node root = session.getRootNode();
            if ( root.hasNode( "repositories" ) )
            {
                Node node = root.getNode( "repositories" );

                repositories = new ArrayList<String>();
                NodeIterator i = node.getNodes();
                while ( i.hasNext() )
                {
                    Node n = i.nextNode();
                    repositories.add( n.getName() );
                }
            }
            else
            {
                repositories = Collections.emptyList();
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
        return repositories;
    }

    public List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
        throws MetadataRepositoryException
    {
        List<ArtifactMetadata> artifacts;

        String q = getArtifactQuery( repositoryId ) + " AND ([sha1] = $checksum OR [md5] = $checksum)";

        try
        {
            Query query = session.getWorkspace().getQueryManager().createQuery( q, Query.JCR_SQL2 );
            ValueFactory valueFactory = session.getValueFactory();
            query.bindValue( "checksum", valueFactory.createValue( checksum ) );
            QueryResult result = query.execute();

            artifacts = new ArrayList<ArtifactMetadata>();
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

    public void removeArtifact( String repositoryId, String namespace, String projectId, String projectVersion,
                                String id )
        throws MetadataRepositoryException
    {
        try
        {
            Node root = session.getRootNode();
            String path = getArtifactPath( repositoryId, namespace, projectId, projectVersion, id );
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

    public void removeRepository( String repositoryId )
        throws MetadataRepositoryException
    {
        try
        {
            Node root = session.getRootNode();
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

    public List<ArtifactMetadata> getArtifacts( String repositoryId )
        throws MetadataRepositoryException
    {
        List<ArtifactMetadata> artifacts;

        String q = getArtifactQuery( repositoryId );

        try
        {
            Query query = session.getWorkspace().getQueryManager().createQuery( q, Query.JCR_SQL2 );
            QueryResult result = query.execute();

            artifacts = new ArrayList<ArtifactMetadata>();
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

    private static String getArtifactQuery( String repositoryId )
    {
        return "SELECT * FROM [" + ARTIFACT_NODE_TYPE + "] AS artifact WHERE ISDESCENDANTNODE(artifact,'/" +
            getRepositoryContentPath( repositoryId ) + "')";
    }

    public ProjectMetadata getProject( String repositoryId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        ProjectMetadata metadata = null;

        try
        {
            Node root = session.getRootNode();

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

    public ProjectVersionMetadata getProjectVersion( String repositoryId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolutionException
    {
        ProjectVersionMetadata versionMetadata;

        try
        {
            Node root = session.getRootNode();

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
            versionMetadata.setIncomplete( node.hasProperty( "incomplete" ) && node.getProperty(
                "incomplete" ).getBoolean() );

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

            for ( Node n : JcrUtils.getChildNodes( node ) )
            {
                if ( n.isNodeType( FACET_NODE_TYPE ) )
                {
                    String name = n.getName();
                    MetadataFacetFactory factory = metadataFacetFactories.get( name );
                    if ( factory == null )
                    {
                        log.error( "Attempted to load unknown project version metadata facet: " + name );
                    }
                    else
                    {
                        MetadataFacet facet = factory.createMetadataFacet();
                        Map<String, String> map = new HashMap<String, String>();
                        for ( Property property : JcrUtils.getProperties( n ) )
                        {
                            String p = property.getName();
                            if ( !p.startsWith( "jcr:" ) )
                            {
                                map.put( p, property.getString() );
                            }
                        }
                        facet.fromProperties( map );
                        versionMetadata.addFacet( facet );
                    }
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return versionMetadata;
    }

    public Collection<String> getArtifactVersions( String repositoryId, String namespace, String projectId,
                                                   String projectVersion )
        throws MetadataResolutionException
    {
        Set<String> versions = new LinkedHashSet<String>();

        try
        {
            Node root = session.getRootNode();

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

    public Collection<ProjectVersionReference> getProjectReferences( String repositoryId, String namespace,
                                                                     String projectId, String projectVersion )
        throws MetadataResolutionException
    {
        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();

        try
        {
            Node root = session.getRootNode();

            String path = getProjectVersionPath( repositoryId, namespace, projectId, projectVersion ) + "/references";
            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );

                NodeIterator i = node.getNodes();
                while ( i.hasNext() )
                {
                    Node ns = i.nextNode();

                    NodeIterator j = ns.getNodes();

                    while ( j.hasNext() )
                    {
                        Node project = j.nextNode();

                        NodeIterator k = project.getNodes();

                        while ( k.hasNext() )
                        {
                            Node version = k.nextNode();

                            ProjectVersionReference ref = new ProjectVersionReference();
                            ref.setNamespace( ns.getName() );
                            ref.setProjectId( project.getName() );
                            ref.setProjectVersion( version.getName() );
                            String type = version.getProperty( "type" ).getString();
                            ref.setReferenceType( ProjectVersionReference.ReferenceType.valueOf( type ) );
                            references.add( ref );
                        }
                    }
                }
            }
        }
        catch ( RepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

        return references;
    }

    public Collection<String> getRootNamespaces( String repositoryId )
        throws MetadataResolutionException
    {
        return getNamespaces( repositoryId, null );
    }

    public Collection<String> getNamespaces( String repositoryId, String baseNamespace )
        throws MetadataResolutionException
    {
        String path = baseNamespace != null
            ? getNamespacePath( repositoryId, baseNamespace )
            : getRepositoryContentPath( repositoryId );

        return getNodeNames( path, NAMESPACE_NODE_TYPE );
    }

    public Collection<String> getProjects( String repositoryId, String namespace )
        throws MetadataResolutionException
    {
        return getNodeNames( getNamespacePath( repositoryId, namespace ), PROJECT_NODE_TYPE );
    }

    public Collection<String> getProjectVersions( String repositoryId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        return getNodeNames( getProjectPath( repositoryId, namespace, projectId ), PROJECT_VERSION_NODE_TYPE );
    }

    public Collection<ArtifactMetadata> getArtifacts( String repositoryId, String namespace, String projectId,
                                                      String projectVersion )
        throws MetadataResolutionException
    {
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();

        try
        {
            Node root = session.getRootNode();
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

    public void save()
        throws MetadataRepositoryException
    {
        try
        {
            session.save();
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    public void revert()
        throws MetadataRepositoryException
    {
        try
        {
            session.refresh( false );
        }
        catch ( RepositoryException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    public boolean canObtainAccess( Class<?> aClass )
    {
        return aClass == Session.class;
    }

    public Object obtainAccess( Class<?> aClass )
    {
        if ( aClass == Session.class )
        {
            return session;
        }
        throw new IllegalArgumentException(
            "Access using " + aClass + " is not supported on the JCR metadata storage" );
    }

    public void close()
    {
        session.logout();
    }

    private ArtifactMetadata getArtifactFromNode( String repositoryId, Node artifactNode )
        throws RepositoryException
    {
        String id = artifactNode.getName();

        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setId( id );
        artifact.setRepositoryId( repositoryId );

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
            artifact.setWhenGathered( artifactNode.getProperty( "whenGathered" ).getDate().getTime() );
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

        for ( Node n : JcrUtils.getChildNodes( artifactNode ) )
        {
            if ( n.isNodeType( FACET_NODE_TYPE ) )
            {
                String name = n.getName();
                MetadataFacetFactory factory = metadataFacetFactories.get( name );
                if ( factory == null )
                {
                    log.error( "Attempted to load unknown project version metadata facet: " + name );
                }
                else
                {
                    MetadataFacet facet = factory.createMetadataFacet();
                    Map<String, String> map = new HashMap<String, String>();
                    for ( Property p : JcrUtils.getProperties( n ) )
                    {
                        String property = p.getName();
                        if ( !property.startsWith( "jcr:" ) )
                        {
                            map.put( property, p.getString() );
                        }
                    }
                    facet.fromProperties( map );
                    artifact.addFacet( facet );
                }
            }
        }
        return artifact;
    }

    private static String getPropertyString( Node node, String name )
        throws RepositoryException
    {
        return node.hasProperty( name ) ? node.getProperty( name ).getString() : null;
    }

    private Collection<String> getNodeNames( String path, String nodeType )
        throws MetadataResolutionException
    {
        List<String> names = new ArrayList<String>();

        try
        {
            Node root = session.getRootNode();

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
        return getRepositoryPath( repositoryId ) + "/content/";
    }

    private static String getFacetPath( String repositoryId, String facetId )
    {
        return getRepositoryPath( repositoryId ) + "/facets/" + facetId;
    }

    private static String getNamespacePath( String repositoryId, String namespace )
    {
        return getRepositoryContentPath( repositoryId ) + namespace.replace( '.', '/' );
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
        Node node = baseNode;
        for ( String n : name.split( "/" ) )
        {
            node = JcrUtils.getOrAddNode( node, n );
            if ( nodeType != null )
            {
                node.addMixin( nodeType );
            }
        }
        return node;
    }

    private static String getFacetPath( String repositoryId, String facetId, String name )
    {
        return getFacetPath( repositoryId, facetId ) + "/" + name;
    }

    private Node getOrAddRepositoryNode( String repositoryId )
        throws RepositoryException
    {
        Node root = session.getRootNode();
        Node node = JcrUtils.getOrAddNode( root, "repositories" );
        node = JcrUtils.getOrAddNode( node, repositoryId );
        return node;
    }

    private Node getOrAddRepositoryContentNode( String repositoryId )
        throws RepositoryException
    {
        Node node = getOrAddRepositoryNode( repositoryId );
        return JcrUtils.getOrAddNode( node, "content" );
    }

    private Node getOrAddNamespaceNode( String repositoryId, String namespace )
        throws RepositoryException
    {
        Node repo = getOrAddRepositoryContentNode( repositoryId );
        return getOrAddNodeByPath( repo, namespace.replace( '.', '/' ), NAMESPACE_NODE_TYPE );
    }

    private Node getOrAddProjectNode( String repositoryId, String namespace, String projectId )
        throws RepositoryException
    {
        Node namespaceNode = getOrAddNamespaceNode( repositoryId, namespace );
        Node node = JcrUtils.getOrAddNode( namespaceNode, projectId );
        node.addMixin( PROJECT_NODE_TYPE );
        return node;
    }

    private Node getOrAddProjectVersionNode( String repositoryId, String namespace, String projectId,
                                             String projectVersion )
        throws RepositoryException
    {
        Node projectNode = getOrAddProjectNode( repositoryId, namespace, projectId );
        Node node = JcrUtils.getOrAddNode( projectNode, projectVersion );
        node.addMixin( PROJECT_VERSION_NODE_TYPE );
        return node;
    }

    private Node getOrAddArtifactNode( String repositoryId, String namespace, String projectId, String projectVersion,
                                       String id )
        throws RepositoryException
    {
        Node versionNode = getOrAddProjectVersionNode( repositoryId, namespace, projectId, projectVersion );
        Node node = JcrUtils.getOrAddNode( versionNode, id );
        node.addMixin( ARTIFACT_NODE_TYPE );
        return node;
    }

    private static Calendar createCalendar( Date time )
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( time );
        return cal;
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

    public Session getJcrSession()
    {
        return session;
    }
}
