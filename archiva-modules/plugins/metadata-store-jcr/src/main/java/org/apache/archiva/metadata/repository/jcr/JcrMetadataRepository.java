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
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.jackrabbit.core.TransientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.MetadataRepository"
 * @todo path construction should be centralised
 * @todo review all methods for alternate implementations (use of queries)
 * @todo below: exception handling
 * @todo below: revise storage format for project version metadata
 */
public class JcrMetadataRepository
    implements MetadataRepository
{
    /**
     * @plexus.requirement role="org.apache.archiva.metadata.model.MetadataFacetFactory"
     */
    private Map<String, MetadataFacetFactory> metadataFacetFactories;

    private static final Logger log = LoggerFactory.getLogger( JcrMetadataRepository.class );

    private static Repository repository;

    private Session session;

    public JcrMetadataRepository()
    {
        // TODO: need to close this at the end - do we need to add it in the API?

        try
        {
            // TODO: push this in from the test, and make it possible from the webapp
            if ( repository == null )
            {
                repository = new TransientRepository( new File( "src/test/repository.xml" ), new File( "target/jcr" ) );
            }
            // TODO: shouldn't do this in constructor since it's a singleton
            session = repository.login( new SimpleCredentials( "username", "password".toCharArray() ) );
        }
        catch ( LoginException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public void updateProject( String repositoryId, ProjectMetadata project )
    {
        String namespace = project.getNamespace();
        String projectId = project.getId();
        updateProject( repositoryId, namespace, projectId );
    }

    private void updateProject( String repositoryId, String namespace, String projectId )
    {
        updateNamespace( repositoryId, namespace );

        try
        {
            Node namespaceNode = getOrCreateNamespaceNode( repositoryId, namespace );
            getOrCreateNode( namespaceNode, projectId );
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public void updateArtifact( String repositoryId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
    {
        try
        {
            Node node = getOrCreateArtifactNode( repositoryId, namespace, projectId, projectVersion,
                                                 artifactMeta.getId() );

            Calendar cal = Calendar.getInstance();
            cal.setTime( artifactMeta.getFileLastModified() );
            node.setProperty( "updated", cal );

            cal = Calendar.getInstance();
            cal.setTime( artifactMeta.getWhenGathered() );
            node.setProperty( "whenGathered", cal );

            node.setProperty( "size", artifactMeta.getSize() );
            node.setProperty( "md5", artifactMeta.getMd5() );
            node.setProperty( "sha1", artifactMeta.getSha1() );

            node.setProperty( "version", artifactMeta.getVersion() );

            // TODO: namespaced properties instead?
            Node facetNode = getOrCreateNode( node, "facets" );
            for ( MetadataFacet facet : artifactMeta.getFacetList() )
            {
                // TODO: need to clear it?
                Node n = getOrCreateNode( facetNode, facet.getFacetId() );

                for ( Map.Entry<String, String> entry : facet.toProperties().entrySet() )
                {
                    n.setProperty( entry.getKey(), entry.getValue() );
                }
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    private Node getOrCreateArtifactNode( String repositoryId, String namespace, String projectId,
                                          String projectVersion, String id )
        throws RepositoryException
    {
        Node versionNode = getOrCreateProjectVersionNode( repositoryId, namespace, projectId, projectVersion );
        return getOrCreateNode( versionNode, id );
    }

    public void updateProjectVersion( String repositoryId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
    {
        updateProject( repositoryId, namespace, projectId );

        try
        {
            Node versionNode = getOrCreateProjectVersionNode( repositoryId, namespace, projectId,
                                                              versionMetadata.getId() );

            versionNode.setProperty( "name", versionMetadata.getName() );
            versionNode.setProperty( "description", versionMetadata.getDescription() );
            versionNode.setProperty( "url", versionMetadata.getUrl() );
            versionNode.setProperty( "incomplete", versionMetadata.isIncomplete() );

            // TODO: decide how to treat these in the content repo
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
            i = 0;
            for ( Dependency dependency : versionMetadata.getDependencies() )
            {
                versionNode.setProperty( "dependency." + i + ".classifier", dependency.getClassifier() );
                versionNode.setProperty( "dependency." + i + ".scope", dependency.getScope() );
                versionNode.setProperty( "dependency." + i + ".systemPath", dependency.getSystemPath() );
                versionNode.setProperty( "dependency." + i + ".artifactId", dependency.getArtifactId() );
                versionNode.setProperty( "dependency." + i + ".groupId", dependency.getGroupId() );
                versionNode.setProperty( "dependency." + i + ".version", dependency.getVersion() );
                versionNode.setProperty( "dependency." + i + ".type", dependency.getType() );
                i++;
            }

            // TODO: namespaced properties instead?
            Node facetNode = getOrCreateNode( versionNode, "facets" );
            for ( MetadataFacet facet : versionMetadata.getFacetList() )
            {
                // TODO: shouldn't need to recreate, just update
                if ( facetNode.hasNode( facet.getFacetId() ) )
                {
                    facetNode.getNode( facet.getFacetId() ).remove();
                }
                Node n = facetNode.addNode( facet.getFacetId() );
                
                for ( Map.Entry<String, String> entry : facet.toProperties().entrySet() )
                {
                    n.setProperty( entry.getKey(), entry.getValue() );
                }
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    private Node getOrCreateProjectVersionNode( String repositoryId, String namespace, String projectId,
                                                String projectVersion )
        throws RepositoryException
    {
        Node namespaceNode = getOrCreateNamespaceNode( repositoryId, namespace );
        Node projectNode = getOrCreateNode( namespaceNode, projectId );
        return getOrCreateNode( projectNode, projectVersion );
    }

    private Node getOrCreateNode( Node baseNode, String name )
        throws RepositoryException
    {
        return baseNode.hasNode( name ) ? baseNode.getNode( name ) : baseNode.addNode( name );
    }

    private Node getOrCreateNamespaceNode( String repositoryId, String namespace )
        throws RepositoryException
    {
        Node repo = getOrCreateRepositoryContentNode( repositoryId );
        return getOrCreateNode( repo, namespace );
    }

    private Node getOrCreateRepositoryContentNode( String repositoryId )
        throws RepositoryException
    {
        Node node = getOrCreateRepositoryNode( repositoryId );
        return getOrCreateNode( node, "content" );
    }

    private Node getOrCreateRepositoryNode( String repositoryId )
        throws RepositoryException
    {
        Node root = session.getRootNode();
        Node node = getOrCreateNode( root, "repositories" );
        node = getOrCreateNode( node, repositoryId );
        return node;
    }

    public void updateProjectReference( String repositoryId, String namespace, String projectId, String projectVersion,
                                        ProjectVersionReference reference )
    {
        // TODO: try weak reference?
        // TODO: is this tree the right way up? It differs from the content model
        try
        {
            Node node = getOrCreateRepositoryContentNode( repositoryId );
            node = getOrCreateNode( node, namespace );
            node = getOrCreateNode( node, projectId );
            node = getOrCreateNode( node, projectVersion );
            node = getOrCreateNode( node, "references" );
            node = getOrCreateNode( node, reference.getNamespace() );
            node = getOrCreateNode( node, reference.getProjectId() );
            node = getOrCreateNode( node, reference.getProjectVersion() );
            node.setProperty( "type", reference.getReferenceType().toString() );
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public void updateNamespace( String repositoryId, String namespace )
    {
        try
        {
            Node node = getOrCreateNamespaceNode( repositoryId, namespace );
            node.setProperty( "namespace", namespace );
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public List<String> getMetadataFacets( String repositoryId, String facetId )
    {
        List<String> facets = new ArrayList<String>();

        try
        {
            Node root = session.getRootNode();
            Node node = root.getNode( "repositories/" + repositoryId + "/facets/" + facetId );

            // TODO: could we simply query all nodes with no children?
            recurse( facets, "", node );
        }
        catch ( PathNotFoundException e )
        {
            // TODO: handle this case differently?
            // currently ignored
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
        return facets;
    }

    private void recurse( List<String> facets, String prefix, Node node )
        throws RepositoryException
    {
        NodeIterator iterator = node.getNodes();
        while ( iterator.hasNext() )
        {
            Node n = iterator.nextNode();
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
    {
        MetadataFacet metadataFacet = null;
        try
        {
            Node root = session.getRootNode();
            Node node = root.getNode( "repositories/" + repositoryId + "/facets/" + facetId + "/" + name );

            MetadataFacetFactory metadataFacetFactory = metadataFacetFactories.get( facetId );
            if ( metadataFacetFactory != null )
            {
                metadataFacet = metadataFacetFactory.createMetadataFacet( repositoryId, name );
                Map<String, String> map = new HashMap<String, String>();
                PropertyIterator iterator = node.getProperties();
                while ( iterator.hasNext() )
                {
                    Property property = iterator.nextProperty();
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
            // TODO: handle this case differently?
            // currently ignored
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
        return metadataFacet;
    }

    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
    {
        try
        {
            Node repo = getOrCreateRepositoryNode( repositoryId );
            Node facets = getOrCreateNode( repo, "facets" );

            String id = metadataFacet.getFacetId();
            Node facetNode = getOrCreateNode( facets, id );

            Node node = getOrCreatePath( facetNode, metadataFacet.getName() );

            for ( Map.Entry<String, String> entry : metadataFacet.toProperties().entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    private Node getOrCreatePath( Node baseNode, String name )
        throws RepositoryException
    {
        Node node = baseNode;
        for ( String n : name.split( "/" ) )
        {
            node = getOrCreateNode( node, n );
        }
        return node;
    }

    public void removeMetadataFacets( String repositoryId, String facetId )
    {
        try
        {
            Node root = session.getRootNode();
            String path = "repositories/" + repositoryId + "/facets/" + facetId;
            // TODO: exception if missing?
            if ( root.hasNode( path ) )
            {
                root.getNode( path ).remove();
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public void removeMetadataFacet( String repositoryId, String facetId, String name )
    {
        try
        {
            Node root = session.getRootNode();
            String path = "repositories/" + repositoryId + "/facets/" + facetId + "/" + name;
            // TODO: exception if missing?
            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );
                do
                {
                    Node parent = node.getParent();
                    node.remove();
                    node = parent;
                }
                while ( !node.hasNodes() );
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime )
    {
        // TODO: this is quite slow - if we are to persist with this repository implementation we should build an index
        //  of this information (eg. in Lucene, as before)

        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();
        for ( String ns : getRootNamespaces( repoId ) )
        {
            getArtifactsByDateRange( artifacts, repoId, ns, startTime, endTime );
        }
        Collections.sort( artifacts, new ArtifactComparator() );
        return artifacts;
    }

    private void getArtifactsByDateRange( List<ArtifactMetadata> artifacts, String repoId, String ns, Date startTime,
                                          Date endTime )
    {
        for ( String namespace : getNamespaces( repoId, ns ) )
        {
            getArtifactsByDateRange( artifacts, repoId, ns + "." + namespace, startTime, endTime );
        }

        for ( String project : getProjects( repoId, ns ) )
        {
            for ( String version : getProjectVersions( repoId, ns, project ) )
            {
                for ( ArtifactMetadata artifact : getArtifacts( repoId, ns, project, version ) )
                {
                    if ( startTime == null || startTime.before( artifact.getWhenGathered() ) )
                    {
                        if ( endTime == null || endTime.after( artifact.getWhenGathered() ) )
                        {
                            artifacts.add( artifact );
                        }
                    }
                }
            }
        }
    }

    public Collection<String> getRepositories()
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
            // TODO
            throw new RuntimeException( e );
        }
        return repositories;
    }

    public List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
    {
        // TODO: this is quite slow - if we are to persist with this repository implementation we should build an index
        //  of this information (eg. in Lucene, as before)
        // alternatively, we could build a referential tree in the content repository, however it would need some levels
        // of depth to avoid being too broad to be useful (eg. /repository/checksums/a/ab/abcdef1234567)

        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();
        for ( String ns : getRootNamespaces( repositoryId ) )
        {
            getArtifactsByChecksum( artifacts, repositoryId, ns, checksum );
        }
        return artifacts;
    }

    private void getArtifactsByChecksum( List<ArtifactMetadata> artifacts, String repositoryId, String ns,
                                         String checksum )
    {
        for ( String namespace : getNamespaces( repositoryId, ns ) )
        {
            getArtifactsByChecksum( artifacts, repositoryId, ns + "." + namespace, checksum );
        }

        for ( String project : getProjects( repositoryId, ns ) )
        {
            for ( String version : getProjectVersions( repositoryId, ns, project ) )
            {
                for ( ArtifactMetadata artifact : getArtifacts( repositoryId, ns, project, version ) )
                {
                    if ( checksum.equals( artifact.getMd5() ) || checksum.equals( artifact.getSha1() ) )
                    {
                        artifacts.add( artifact );
                    }
                }
            }
        }
    }

    public void deleteArtifact( String repositoryId, String namespace, String projectId, String projectVersion,
                                String id )
    {
        try
        {
            Node root = session.getRootNode();
            String path =
                "repositories/" + repositoryId + "/content/" + namespace + "/" + projectId + "/" + projectVersion +
                    "/" + id;
            // TODO: exception if missing?
            if ( root.hasNode( path ) )
            {
                root.getNode( path ).remove();
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public void deleteRepository( String repositoryId )
    {
        try
        {
            Node root = session.getRootNode();
            String path = "repositories/" + repositoryId;
            // TODO: exception if missing?
            if ( root.hasNode( path ) )
            {
                root.getNode( path ).remove();
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
    }

    public List<ArtifactMetadata> getArtifacts( String repositoryId )
    {
        // TODO: query faster?
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();
        for ( String ns : getRootNamespaces( repositoryId ) )
        {
            getArtifacts( artifacts, repositoryId, ns );
        }
        return artifacts;
    }

    private void getArtifacts( List<ArtifactMetadata> artifacts, String repoId, String ns )
    {
        for ( String namespace : getNamespaces( repoId, ns ) )
        {
            getArtifacts( artifacts, repoId, ns + "." + namespace );
        }

        for ( String project : getProjects( repoId, ns ) )
        {
            for ( String version : getProjectVersions( repoId, ns, project ) )
            {
                for ( ArtifactMetadata artifact : getArtifacts( repoId, ns, project, version ) )
                {
                    artifacts.add( artifact );
                }
            }
        }
    }

    public ProjectMetadata getProject( String repositoryId, String namespace, String projectId )
    {
        ProjectMetadata metadata = null;

        try
        {
            Node root = session.getRootNode();

            // basically just checking it exists
            String path = "repositories/" + repositoryId + "/content/" + namespace + "/" + projectId;
            if ( root.hasNode( path ) )
            {
                metadata = new ProjectMetadata();
                metadata.setId( projectId );
                metadata.setNamespace( namespace );
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
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

            String path =
                "repositories/" + repositoryId + "/content/" + namespace + "/" + projectId + "/" + projectVersion;
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

            // TODO: decide how to treat these in the content repo
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

            done = false;
            i = 0;
            while ( !done )
            {
                String dependencyArtifactId = getPropertyString( node, "dependency." + i + ".artifactId" );
                if ( dependencyArtifactId != null )
                {
                    Dependency dependency = new Dependency();
                    dependency.setArtifactId( dependencyArtifactId );
                    dependency.setGroupId( getPropertyString( node, "dependency." + i + ".groupId" ) );
                    dependency.setClassifier( getPropertyString( node, "dependency." + i + ".classifier" ) );
                    dependency.setOptional( Boolean.valueOf( getPropertyString( node,
                                                                                "dependency." + i + ".optional" ) ) );
                    dependency.setScope( getPropertyString( node, "dependency." + i + ".scope" ) );
                    dependency.setSystemPath( getPropertyString( node, "dependency." + i + ".systemPath" ) );
                    dependency.setType( getPropertyString( node, "dependency." + i + ".type" ) );
                    dependency.setVersion( getPropertyString( node, "dependency." + i + ".version" ) );
                    versionMetadata.addDependency( dependency );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            if ( node.hasNode( "facets" ) )
            {
                NodeIterator j = node.getNode( "facets" ).getNodes();

                while ( j.hasNext() )
                {
                    Node facetNode = j.nextNode();

                    MetadataFacetFactory factory = metadataFacetFactories.get( facetNode.getName() );
                    if ( factory == null )
                    {
                        log.error( "Attempted to load unknown project version metadata facet: " + facetNode.getName() );
                    }
                    else
                    {
                        MetadataFacet facet = factory.createMetadataFacet();
                        Map<String, String> map = new HashMap<String, String>();
                        PropertyIterator iterator = facetNode.getProperties();
                        while ( iterator.hasNext() )
                        {
                            Property property = iterator.nextProperty();
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
            // TODO
            throw new RuntimeException( e );
        }

        return versionMetadata;
    }

    private static String getPropertyString( Node node, String name )
        throws RepositoryException
    {
        return node.hasProperty( name ) ? node.getProperty( name ).getString() : null;
    }

    public Collection<String> getArtifactVersions( String repositoryId, String namespace, String projectId,
                                                   String projectVersion )
    {
        Set<String> versions = new LinkedHashSet<String>();

        try
        {
            Node root = session.getRootNode();

            Node node = root.getNode(
                "repositories/" + repositoryId + "/content/" + namespace + "/" + projectId + "/" + projectVersion );

            NodeIterator iterator = node.getNodes();
            while ( iterator.hasNext() )
            {
                Node n = iterator.nextNode();

                versions.add( n.getProperty( "version" ).getString() );
            }
        }
        catch ( PathNotFoundException e )
        {
            // ignore repo not found for now
            // TODO: throw specific exception if repo doesn't exist
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }

        return versions;
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repositoryId, String namespace,
                                                                     String projectId, String projectVersion )
    {
        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();

        try
        {
            Node root = session.getRootNode();

            String path =
                "repositories/" + repositoryId + "/content/" + namespace + "/" + projectId + "/" + projectVersion +
                    "/references";
            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );

                // TODO: use query by reference type
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
            // TODO
            throw new RuntimeException( e );
        }

        return references;
    }

    public Collection<String> getRootNamespaces( String repositoryId )
    {
        return getNamespaces( repositoryId, null );
    }

    private Collection<String> getNodeNames( String path )
    {
        List<String> names = new ArrayList<String>();

        try
        {
            Node root = session.getRootNode();

            Node repository = root.getNode( path );

            NodeIterator nodes = repository.getNodes();
            while ( nodes.hasNext() )
            {
                Node node = nodes.nextNode();
                names.add( node.getName() );
            }
        }
        catch ( PathNotFoundException e )
        {
            // ignore repo not found for now
            // TODO: throw specific exception if repo doesn't exist
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }

        return names;
    }

    public Collection<String> getNamespaces( String repositoryId, String baseNamespace )
    {
        // TODO: could be simpler with pathed namespaces, rely on namespace property
        Collection<String> allNamespaces = getNodeNames( "repositories/" + repositoryId + "/content" );

        Set<String> namespaces = new LinkedHashSet<String>();
        int fromIndex = baseNamespace != null ? baseNamespace.length() + 1 : 0;
        for ( String namespace : allNamespaces )
        {
            if ( baseNamespace == null || namespace.startsWith( baseNamespace + "." ) )
            {
                int i = namespace.indexOf( '.', fromIndex );
                if ( i >= 0 )
                {
                    namespaces.add( namespace.substring( fromIndex, i ) );
                }
                else
                {
                    namespaces.add( namespace.substring( fromIndex ) );
                }
            }
        }
        return new ArrayList<String>( namespaces );
    }

    public Collection<String> getProjects( String repositoryId, String namespace )
    {
        // TODO: could be simpler with pathed namespaces, rely on namespace property
        return getNodeNames( "repositories/" + repositoryId + "/content/" + namespace );
    }

    public Collection<String> getProjectVersions( String repositoryId, String namespace, String projectId )
    {
        // TODO: could be simpler with pathed namespaces, rely on namespace property
        return getNodeNames( "repositories/" + repositoryId + "/content/" + namespace + "/" + projectId );
    }

    public Collection<ArtifactMetadata> getArtifacts( String repositoryId, String namespace, String projectId,
                                                      String projectVersion )
    {
        List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();

        try
        {
            Node root = session.getRootNode();
            String path =
                "repositories/" + repositoryId + "/content/" + namespace + "/" + projectId + "/" + projectVersion;

            if ( root.hasNode( path ) )
            {
                Node node = root.getNode( path );

                NodeIterator iterator = node.getNodes();
                while ( iterator.hasNext() )
                {
                    Node artifactNode = iterator.nextNode();

                    String id = artifactNode.getName();

                    ArtifactMetadata artifact = new ArtifactMetadata();
                    artifact.setId( id );
                    artifact.setRepositoryId( repositoryId );
                    artifact.setNamespace( namespace );
                    artifact.setProject( projectId );
                    artifact.setProjectVersion( projectVersion );
                    artifact.setVersion( artifactNode.hasProperty( "version" ) ? artifactNode.getProperty(
                        "version" ).getString() : projectVersion );

                    if ( artifactNode.hasProperty( "updated" ) )
                    {
                        artifact.setFileLastModified( artifactNode.getProperty(
                            "updated" ).getDate().getTimeInMillis() );
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

                    if ( artifactNode.hasNode( "facets" ) )
                    {
                        NodeIterator j = artifactNode.getNode( "facets" ).getNodes();

                        while ( j.hasNext() )
                        {
                            Node facetNode = j.nextNode();

                            MetadataFacetFactory factory = metadataFacetFactories.get( facetNode.getName() );
                            if ( factory == null )
                            {
                                log.error( "Attempted to load unknown project version metadata facet: " + facetNode.getName() );
                            }
                            else
                            {
                                MetadataFacet facet = factory.createMetadataFacet();
                                Map<String, String> map = new HashMap<String, String>();
                                PropertyIterator i = facetNode.getProperties();
                                while ( i.hasNext() )
                                {
                                    Property p = i.nextProperty();
                                    String property = p.getName();
                                    map.put( property, p.getString() );
                                }
                                facet.fromProperties( map );
                                artifact.addFacet( facet );
                            }
                        }
                    }
                    artifacts.add( artifact );
                }
            }
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }

        return artifacts;
    }

    void close()
    {
        try
        {
            // TODO: this shouldn't be here! Repository may need a context
            session.save();
        }
        catch ( RepositoryException e )
        {
            // TODO
            throw new RuntimeException( e );
        }
        session.logout();
    }

    public void setMetadataFacetFactories( Map<String, MetadataFacetFactory> metadataFacetFactories )
    {
        this.metadataFacetFactories = metadataFacetFactories;

        // TODO: check if actually called by normal injection

//        for ( String facetId : metadataFacetFactories.keySet() )
//        {
//            // TODO: second arg should be a better URL for the namespace
//            session.getWorkspace().getNamespaceRegistry().registerNamespace( facetId, facetId );
//        }
    }

    private static class ArtifactComparator
        implements Comparator<ArtifactMetadata>
    {
        public int compare( ArtifactMetadata artifact1, ArtifactMetadata artifact2 )
        {
            if ( artifact1.getWhenGathered() == artifact2.getWhenGathered() )
            {
                return 0;
            }
            if ( artifact1.getWhenGathered() == null )
            {
                return 1;
            }
            if ( artifact2.getWhenGathered() == null )
            {
                return -1;
            }
            return artifact1.getWhenGathered().compareTo( artifact2.getWhenGathered() );
        }
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
}
