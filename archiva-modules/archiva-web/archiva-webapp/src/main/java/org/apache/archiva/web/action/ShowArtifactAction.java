package org.apache.archiva.web.action;

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

import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.metadata.generic.GenericMetadataFacet;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.storage.maven2.ArtifactMetadataVersionComparator;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.rest.services.utils.ArtifactDownloadInfoBuilder;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Browse the repository.
 * <p/>
 * TODO change name to ShowVersionedAction to conform to terminology.
 */
@SuppressWarnings( "serial" )
@Controller( "showArtifactAction" )
@Scope( "prototype" )
public class ShowArtifactAction
    extends AbstractRepositoryBasedAction
    implements Validateable
{
    /* .\ Not Exposed \._____________________________________________ */

    @Inject
    private RepositoryContentFactory repositoryFactory;

    /* .\ Exposed Output Objects \.__________________________________ */

    private String groupId;

    private String artifactId;

    private String version;

    private String repositoryId;

    /**
     * The model of this versioned project.
     */
    private ProjectVersionMetadata model;

    /**
     * The list of artifacts that depend on this versioned project.
     */
    private List<ProjectVersionReference> dependees;

    private List<MailingList> mailingLists;

    private List<Dependency> dependencies;

    private Map<String, List<Artifact>> artifacts;

    private boolean dependencyTree = false;

    private String deleteItem;

    private Map<String, String> genericMetadata;

    private String propertyName;

    private String propertyValue;

    /**
     * Show the versioned project information tab. TODO: Change name to 'project' - we are showing project versions
     * here, not specific artifact information (though that is rendered in the download box).
     */
    public String artifact()
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            return handleArtifact( repositorySession );
        }
        catch ( Exception e )
        {
            log.warn( "Unable to getProjectVersionMetadata: " + e.getMessage(), e );
            addActionError( "Unable to getProjectVersionMetadata - consult application logs." );
            return ERROR;
        }
        finally

        {
            repositorySession.close();
        }

    }

    private String handleArtifact( RepositorySession session )
        throws RepositoryNotFoundException, RepositoryException
    {
        // In the future, this should be replaced by the repository grouping mechanism, so that we are only making
        // simple resource requests here and letting the resolver take care of it
        ProjectVersionMetadata versionMetadata = getProjectVersionMetadata( session );

        if ( versionMetadata == null )
        {
            addActionError( "Artifact not found" );
            return ERROR;
        }

        if ( versionMetadata.isIncomplete() )
        {
            addIncompleteModelWarning( "Artifact metadata is incomplete." );
        }

        model = versionMetadata;

        return SUCCESS;
    }

    private ProjectVersionMetadata getProjectVersionMetadata( RepositorySession session )
        throws RepositoryNotFoundException, RepositoryException
    {
        ProjectVersionMetadata versionMetadata = null;
        artifacts = new LinkedHashMap<String, List<Artifact>>();

        List<String> repos = getObservableRepos();

        MetadataResolver metadataResolver = session.getResolver();
        for ( String repoId : repos )
        {
            if ( versionMetadata == null )
            {
                // we don't want the implementation being that intelligent - so another resolver to do the
                // "just-in-time" nature of picking up the metadata (if appropriate for the repository type) is used
                try
                {
                    versionMetadata =
                        metadataResolver.resolveProjectVersion( session, repoId, groupId, artifactId, version );
                    if ( versionMetadata != null )
                    {
                        MetadataFacet repoProbFacet;
                        if ( ( repoProbFacet = versionMetadata.getFacet( RepositoryProblemFacet.FACET_ID ) ) != null )
                        {
                            addIncompleteModelWarning( "Artifact metadata is incomplete: "
                                                           + ( (RepositoryProblemFacet) repoProbFacet ).getProblem() );
                            //set metadata to complete so that no additional 'Artifact metadata is incomplete' warning is logged
                            versionMetadata.setIncomplete( false );
                        }
                    }

                }
                catch ( MetadataResolutionException e )
                {
                    addIncompleteModelWarning( "Error resolving artifact metadata: " + e.getMessage() );

                    // TODO: need a consistent way to construct this - same in ArchivaMetadataCreationConsumer
                    versionMetadata = new ProjectVersionMetadata();
                    versionMetadata.setId( version );
                }
                if ( versionMetadata != null )
                {
                    repositoryId = repoId;

                    List<ArtifactMetadata> artifacts;
                    try
                    {
                        artifacts = new ArrayList<ArtifactMetadata>(
                            metadataResolver.resolveArtifacts( session, repoId, groupId, artifactId, version ) );
                    }
                    catch ( MetadataResolutionException e )
                    {
                        addIncompleteModelWarning( "Error resolving artifact metadata: " + e.getMessage() );
                        artifacts = Collections.emptyList();
                    }
                    Collections.sort( artifacts, ArtifactMetadataVersionComparator.INSTANCE );

                    for ( ArtifactMetadata artifact : artifacts )
                    {
                        List<Artifact> l = this.artifacts.get( artifact.getVersion() );
                        if ( l == null )
                        {
                            l = new ArrayList<Artifact>();
                            this.artifacts.put( artifact.getVersion(), l );
                        }

                        ArtifactDownloadInfoBuilder builder = new ArtifactDownloadInfoBuilder().forArtifactMetadata(
                            artifact ).withManagedRepositoryContent(
                            repositoryFactory.getManagedRepositoryContent( repositoryId ) );
                        l.add( builder.build() );
                    }
                }
            }
        }

        return versionMetadata;
    }

    private void addIncompleteModelWarning( String warningMessage )
    {
        addActionError( warningMessage );
        //"The model may be incomplete due to a previous error in resolving information. Refer to the repository problem reports for more information." );
    }

    /**
     * Show the artifact information tab.
     */
    public String dependencies()
    {
        String result = artifact();

        this.dependencies = model.getDependencies();

        return result;
    }

    /**
     * Show the mailing lists information tab.
     */
    public String mailingLists()
    {
        String result = artifact();

        this.mailingLists = model.getMailingLists();

        return result;
    }

    /**
     * Show the reports tab.
     */
    public String reports()
    {
        // TODO: hook up reports on project

        return SUCCESS;
    }

    /**
     * Show the dependees (other artifacts that depend on this project) tab.
     */
    public String dependees()
        throws MetadataResolutionException
    {
        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();
        // TODO: what if we get duplicates across repositories?
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataResolver metadataResolver = repositorySession.getResolver();
            for ( String repoId : getObservableRepos() )
            {
                // TODO: what about if we want to see this irrespective of version?
                references.addAll(
                    metadataResolver.resolveProjectReferences( repositorySession, repoId, groupId, artifactId,
                                                               version ) );
            }
        }
        finally
        {
            repositorySession.close();
        }

        this.dependees = references;

        // TODO: may need to note on the page that references will be incomplete if the other artifacts are not yet
        // stored in the content repository
        // (especially in the case of pre-population import)

        return artifact();
    }

    /**
     * Show the dependencies of this versioned project tab.
     */
    public String dependencyTree()
    {
        // temporarily use this as we only need the model for the tag to perform, but we should be resolving the
        // graph here instead

        // TODO: may need to note on the page that tree will be incomplete if the other artifacts are not yet stored in
        // the content repository
        // (especially in the case of pre-population import)

        // TODO: a bit ugly, should really be mapping all these results differently now
        this.dependencyTree = true;

        return artifact();
    }

    public String projectMetadata()
    {
        String result = artifact();

        if ( model.getFacet( GenericMetadataFacet.FACET_ID ) != null )
        {
            genericMetadata = model.getFacet( GenericMetadataFacet.FACET_ID ).toProperties();
        }

        if ( genericMetadata == null )
        {
            genericMetadata = new HashMap<String, String>();
        }

        return result;
    }

    public String addMetadataProperty()
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        ProjectVersionMetadata projectMetadata;
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            projectMetadata = getProjectVersionMetadata( repositorySession );
            if ( projectMetadata == null )
            {
                addActionError( "Artifact not found" );
                return ERROR;
            }

            if ( projectMetadata.getFacet( GenericMetadataFacet.FACET_ID ) == null )
            {
                genericMetadata = new HashMap<String, String>();
            }
            else
            {
                genericMetadata = projectMetadata.getFacet( GenericMetadataFacet.FACET_ID ).toProperties();
            }

            if ( propertyName == null || "".equals( propertyName.trim() ) || propertyValue == null || "".equals(
                propertyValue.trim() ) )
            {
                model = projectMetadata;
                addActionError( "Property Name and Property Value are required." );
                return INPUT;
            }

            genericMetadata.put( propertyName, propertyValue );

            try
            {
                updateProjectMetadata( projectMetadata, metadataRepository );
                repositorySession.save();
            }
            catch ( MetadataRepositoryException e )
            {
                log.warn( "Unable to persist modified project metadata after adding entry: " + e.getMessage(), e );
                addActionError(
                    "Unable to add metadata item to underlying content storage - consult application logs." );
                return ERROR;
            }

            // TODO: why re-retrieve?
            projectMetadata = getProjectVersionMetadata( repositorySession );
        }
        catch ( Exception e )
        {
            log.warn( "Unable to getProjectVersionMetadata: " + e.getMessage(), e );
            addActionError( "Unable to getProjectVersionMetadata - consult application logs." );
            return ERROR;
        }
        finally
        {
            repositorySession.close();
        }

        genericMetadata = projectMetadata.getFacet( GenericMetadataFacet.FACET_ID ).toProperties();

        model = projectMetadata;

        propertyName = "";
        propertyValue = "";

        return SUCCESS;
    }

    public String deleteMetadataEntry()
    {
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            ProjectVersionMetadata projectMetadata = getProjectVersionMetadata( repositorySession );

            if ( projectMetadata == null )
            {
                addActionError( "Artifact not found" );
                return ERROR;
            }

            if ( projectMetadata.getFacet( GenericMetadataFacet.FACET_ID ) != null )
            {
                genericMetadata = projectMetadata.getFacet( GenericMetadataFacet.FACET_ID ).toProperties();

                if ( !StringUtils.isEmpty( deleteItem ) )
                {
                    genericMetadata.remove( deleteItem );

                    try
                    {
                        updateProjectMetadata( projectMetadata, metadataRepository );
                        repositorySession.save();
                    }
                    catch ( MetadataRepositoryException e )
                    {
                        log.warn( "Unable to persist modified project metadata after removing entry: " + e.getMessage(),
                                  e );
                        addActionError(
                            "Unable to remove metadata item to underlying content storage - consult application logs." );
                        return ERROR;
                    }

                    // TODO: why re-retrieve?
                    projectMetadata = getProjectVersionMetadata( repositorySession );

                    genericMetadata = projectMetadata.getFacet( GenericMetadataFacet.FACET_ID ).toProperties();

                    model = projectMetadata;

                    addActionMessage( "Property successfully deleted." );
                }

                deleteItem = "";
            }
            else
            {
                addActionError( "No generic metadata facet for this artifact." );
                return ERROR;
            }
        }
        catch ( Exception e )
        {
            log.warn( "Unable to getProjectVersionMetadata: " + e.getMessage(), e );
            addActionError( "Unable to getProjectVersionMetadata - consult application logs." );
            return ERROR;

        }
        finally
        {
            repositorySession.close();
        }

        return SUCCESS;
    }

    private void updateProjectMetadata( ProjectVersionMetadata projectMetadata, MetadataRepository metadataRepository )
        throws MetadataRepositoryException
    {
        GenericMetadataFacet genericMetadataFacet = new GenericMetadataFacet();
        genericMetadataFacet.fromProperties( genericMetadata );

        projectMetadata.addFacet( genericMetadataFacet );

        metadataRepository.updateProjectVersion( repositoryId, groupId, artifactId, projectMetadata );
    }

    @Override
    public void validate()
    {
        if ( StringUtils.isBlank( groupId ) )
        {
            addActionError( "You must specify a group ID to browse" );
        }

        if ( StringUtils.isBlank( artifactId ) )
        {
            addActionError( "You must specify a artifact ID to browse" );
        }

        if ( StringUtils.isBlank( version ) )
        {
            addActionError( "You must specify a version to browse" );
        }
    }

    public ProjectVersionMetadata getModel()
    {
        return model;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public List<MailingList> getMailingLists()
    {
        return mailingLists;
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public List<ProjectVersionReference> getDependees()
    {
        return dependees;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public Map<String, List<Artifact>> getArtifacts()
    {
        return artifacts;
    }

    public Collection<String> getSnapshotVersions()
    {
        return artifacts.keySet();
    }

    public boolean isDependencyTree()
    {
        return dependencyTree;
    }

    public void setDeleteItem( String deleteItem )
    {
        this.deleteItem = deleteItem;
    }

    public Map<String, String> getGenericMetadata()
    {
        return genericMetadata;
    }

    public void setGenericMetadata( Map<String, String> genericMetadata )
    {
        this.genericMetadata = genericMetadata;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public void setPropertyName( String propertyName )
    {
        this.propertyName = propertyName;
    }

    public String getPropertyValue()
    {
        return propertyValue;
    }

    public void setPropertyValue( String propertyValue )
    {
        this.propertyValue = propertyValue;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }


}
