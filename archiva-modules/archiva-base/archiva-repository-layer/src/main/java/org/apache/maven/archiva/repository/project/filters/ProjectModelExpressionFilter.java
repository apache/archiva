package org.apache.maven.archiva.repository.project.filters;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaModelCloner;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Exclusion;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.MailingList;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.ProjectRepository;
import org.apache.maven.archiva.model.Scm;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.codehaus.plexus.evaluator.DefaultExpressionEvaluator;
import org.codehaus.plexus.evaluator.EvaluatorException;
import org.codehaus.plexus.evaluator.ExpressionEvaluator;
import org.codehaus.plexus.evaluator.ExpressionSource;
import org.codehaus.plexus.evaluator.sources.PropertiesExpressionSource;
import org.codehaus.plexus.evaluator.sources.SystemPropertyExpressionSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * ProjectModelExpressionFilter 
 *
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.project.ProjectModelFilter"
 *                   role-hint="expression" 
 *                   instantiation-strategy="per-lookup"
 */
public class ProjectModelExpressionFilter
    implements ProjectModelFilter
{
    private ExpressionEvaluator evaluator = new DefaultExpressionEvaluator();

    /**
     * Find and Evaluate the Expressions present in the model.
     * 
     * @param model the model to correct.
     */
    public ArchivaProjectModel filter( final ArchivaProjectModel model )
        throws ProjectModelException
    {
        Properties props = new Properties();

        if ( model.getProperties() != null )
        {
            props.putAll( model.getProperties() );
        }

        ArchivaProjectModel ret = ArchivaModelCloner.clone( model );

        // TODO: should probably clone evaluator to prevent threading issues.
        synchronized ( evaluator )
        {
            // TODO: create .resetSources() method in ExpressionEvaluator project on plexus side.
            // Remove previous expression sources.
            List<ExpressionSource> oldSources = new ArrayList<ExpressionSource>();
            oldSources.addAll( evaluator.getExpressionSourceList() );
            for ( ExpressionSource exprSrc : oldSources )
            {
                evaluator.removeExpressionSource( exprSrc );
            }

            // Setup new sources (based on current model)
            PropertiesExpressionSource propsSource = new PropertiesExpressionSource();
            propsSource.setProperties( props );
            evaluator.addExpressionSource( propsSource );

            // Add system properties to the mix. 
            evaluator.addExpressionSource( new SystemPropertyExpressionSource() );

            try
            {
                // Setup some common properties.
                VersionedReference parent = model.getParentProject();
                if ( parent != null )
                {
                    String parentGroupId = StringUtils.defaultString( evaluator.expand( parent.getGroupId() ) );
                    String parentArtifactId = StringUtils.defaultString( evaluator.expand( parent.getArtifactId() ) );
                    String parentVersion = StringUtils.defaultString( evaluator.expand( parent.getVersion() ) );

                    props.setProperty( "parent.groupId", parentGroupId );
                    props.setProperty( "parent.artifactId", parentArtifactId );
                    props.setProperty( "parent.version", parentVersion );
                }

                String groupId = StringUtils.defaultString( evaluator.expand( model.getGroupId() ) );
                String artifactId = StringUtils.defaultString( evaluator.expand( model.getArtifactId() ) );
                String version = StringUtils.defaultString( evaluator.expand( model.getVersion() ) );
                String name = StringUtils.defaultString( evaluator.expand( model.getName() ) );
                

                /* Archiva doesn't need to handle a full expression language with object tree walking
                 * as the requirements within Archiva are much smaller, a quick replacement of the
                 * important fields (groupId, artifactId, version, name) are handled specifically. 
                 */
                props.setProperty( "pom.groupId",  groupId );
                props.setProperty( "pom.artifactId", artifactId );
                props.setProperty( "pom.version", version );
                props.setProperty( "pom.name", name );
                props.setProperty( "project.groupId",  groupId );
                props.setProperty( "project.artifactId", artifactId );
                props.setProperty( "project.version", version );
                props.setProperty( "project.name", name );

                // Evaluate everything.
                ret.setVersion( evaluator.expand( ret.getVersion() ) );
                ret.setGroupId( evaluator.expand( ret.getGroupId() ) );
                ret.setName( evaluator.expand( ret.getName() ) );
                ret.setDescription( evaluator.expand( ret.getDescription() ) );
                ret.setPackaging( evaluator.expand( ret.getPackaging() ) );
                ret.setUrl( evaluator.expand( ret.getUrl() ) );

                evaluateParentProject( evaluator, ret.getParentProject() );

                evaluateBuildExtensions( evaluator, ret.getBuildExtensions() );
                evaluateCiManagement( evaluator, ret.getCiManagement() );
                evaluateDependencyList( evaluator, ret.getDependencies() );
                evaluateDependencyList( evaluator, ret.getDependencyManagement() );
                evaluateIndividuals( evaluator, ret.getIndividuals() );
                evaluateIssueManagement( evaluator, ret.getIssueManagement() );
                evaluateLicenses( evaluator, ret.getLicenses() );
                evaluateMailingLists( evaluator, ret.getMailingLists() );
                evaluateOrganization( evaluator, ret.getOrganization() );
                evaluatePlugins( evaluator, ret.getPlugins() );
                evaluateRelocation( evaluator, ret.getRelocation() );
                evaluateReports( evaluator, ret.getReports() );
                evaluateRepositories( evaluator, ret.getRepositories() );
                evaluateScm( evaluator, ret.getScm() );
            }
            catch ( EvaluatorException e )
            {
                throw new ProjectModelException( "Unable to evaluate expression in model: " + e.getMessage(), e );
            }
        }

        return ret;
    }

    private void evaluateArtifactReferenceList( ExpressionEvaluator eval, List<ArtifactReference> refs )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( refs ) )
        {
            return;
        }

        for ( ArtifactReference ref : refs )
        {
            ref.setGroupId( eval.expand( ref.getGroupId() ) );
            ref.setArtifactId( eval.expand( ref.getArtifactId() ) );
            ref.setVersion( eval.expand( ref.getVersion() ) );
            ref.setClassifier( eval.expand( ref.getClassifier() ) );
            ref.setType( eval.expand( ref.getType() ) );
        }
    }

    private void evaluateBuildExtensions( ExpressionEvaluator eval, List<ArtifactReference> buildExtensions )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( buildExtensions ) )
        {
            return;
        }

        for ( ArtifactReference ref : buildExtensions )
        {
            ref.setGroupId( eval.expand( ref.getGroupId() ) );
            ref.setArtifactId( eval.expand( ref.getArtifactId() ) );
            ref.setVersion( eval.expand( ref.getVersion() ) );
            ref.setClassifier( eval.expand( ref.getClassifier() ) );
            ref.setType( eval.expand( ref.getType() ) );
        }
    }

    private void evaluateCiManagement( ExpressionEvaluator eval, CiManagement ciManagement )
        throws EvaluatorException
    {
        if ( ciManagement == null )
        {
            return;
        }

        ciManagement.setSystem( eval.expand( ciManagement.getSystem() ) );
        ciManagement.setUrl( eval.expand( ciManagement.getUrl() ) );
    }

    private void evaluateDependencyList( ExpressionEvaluator eval, List<Dependency> dependencies )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( dependencies ) )
        {
            return;
        }

        for ( Dependency dependency : dependencies )
        {
            dependency.setGroupId( eval.expand( dependency.getGroupId() ) );
            dependency.setArtifactId( eval.expand( dependency.getArtifactId() ) );
            dependency.setVersion( eval.expand( dependency.getVersion() ) );
            dependency.setScope( eval.expand( dependency.getScope() ) );
            dependency.setType( eval.expand( dependency.getType() ) );
            dependency.setUrl( eval.expand( dependency.getUrl() ) );

            evaluateExclusions( eval, dependency.getExclusions() );
        }
    }

    private void evaluateExclusions( ExpressionEvaluator eval, List<Exclusion> exclusions )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( exclusions ) )
        {
            return;
        }

        for ( Exclusion exclusion : exclusions )
        {
            exclusion.setGroupId( eval.expand( exclusion.getGroupId() ) );
            exclusion.setArtifactId( eval.expand( exclusion.getArtifactId() ) );
        }
    }

    private void evaluateIndividuals( ExpressionEvaluator eval, List<Individual> individuals )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( individuals ) )
        {
            return;
        }

        for ( Individual individual : individuals )
        {
            individual.setPrincipal( eval.expand( individual.getPrincipal() ) );
            individual.setName( eval.expand( individual.getName() ) );
            individual.setEmail( eval.expand( individual.getEmail() ) );
            individual.setTimezone( eval.expand( individual.getTimezone() ) );
            individual.setOrganization( eval.expand( individual.getOrganization() ) );
            individual.setOrganizationUrl( eval.expand( individual.getOrganizationUrl() ) );
            individual.setUrl( eval.expand( individual.getUrl() ) );

            evaluateProperties( eval, individual.getProperties() );
            evaluateStringList( eval, individual.getRoles() );
        }
    }

    private void evaluateIssueManagement( ExpressionEvaluator eval, IssueManagement issueManagement )
        throws EvaluatorException
    {
        if ( issueManagement == null )
        {
            return;
        }

        issueManagement.setSystem( eval.expand( issueManagement.getSystem() ) );
        issueManagement.setUrl( eval.expand( issueManagement.getUrl() ) );
    }

    private void evaluateLicenses( ExpressionEvaluator eval, List<License> licenses )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( licenses ) )
        {
            return;
        }

        for ( License license : licenses )
        {
            license.setName( eval.expand( license.getName() ) );
            license.setUrl( eval.expand( license.getUrl() ) );
            license.setComments( eval.expand( license.getComments() ) );
        }
    }

    private void evaluateMailingLists( ExpressionEvaluator eval, List<MailingList> mailingLists )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( mailingLists ) )
        {
            return;
        }

        for ( MailingList mlist : mailingLists )
        {
            mlist.setName( eval.expand( mlist.getName() ) );
            mlist.setSubscribeAddress( eval.expand( mlist.getSubscribeAddress() ) );
            mlist.setUnsubscribeAddress( eval.expand( mlist.getUnsubscribeAddress() ) );
            mlist.setPostAddress( eval.expand( mlist.getPostAddress() ) );
            mlist.setMainArchiveUrl( eval.expand( mlist.getMainArchiveUrl() ) );

            evaluateStringList( eval, mlist.getOtherArchives() );
        }
    }

    private void evaluateOrganization( ExpressionEvaluator eval, Organization organization )
        throws EvaluatorException
    {
        if ( organization == null )
        {
            return;
        }

        organization.setName( eval.expand( organization.getName() ) );
        organization.setUrl( eval.expand( organization.getUrl() ) );
        organization.setFavicon( eval.expand( organization.getFavicon() ) );
    }

    private void evaluateParentProject( ExpressionEvaluator eval, VersionedReference parentProject )
        throws EvaluatorException
    {
        if ( parentProject == null )
        {
            return;
        }

        parentProject.setGroupId( eval.expand( parentProject.getGroupId() ) );
        parentProject.setArtifactId( eval.expand( parentProject.getArtifactId() ) );
        parentProject.setVersion( eval.expand( parentProject.getVersion() ) );
    }

    private void evaluatePlugins( ExpressionEvaluator eval, List<ArtifactReference> plugins )
        throws EvaluatorException
    {
        evaluateArtifactReferenceList( eval, plugins );
    }

    private void evaluateProperties( ExpressionEvaluator eval, Properties props )
        throws EvaluatorException
    {
        if ( props == null )
        {
            return;
        }

        // Only evaluate the values, not the keys.

        // Collect the key names. (Done ahead of time to prevent iteration / concurrent modification exceptions)
        Set<String> keys = new HashSet<String>();
        for ( Object obj : props.keySet() )
        {
            keys.add( (String) obj );
        }

        // Evaluate all of the values.
        for ( String key : keys )
        {
            String value = props.getProperty( key );
            props.setProperty( key, eval.expand( value ) );
        }
    }

    private void evaluateRelocation( ExpressionEvaluator eval, VersionedReference relocation )
        throws EvaluatorException
    {
        if ( relocation == null )
        {
            return;
        }

        relocation.setGroupId( eval.expand( relocation.getGroupId() ) );
        relocation.setArtifactId( eval.expand( relocation.getArtifactId() ) );
        relocation.setVersion( eval.expand( relocation.getVersion() ) );
    }

    private void evaluateReports( ExpressionEvaluator eval, List<ArtifactReference> reports )
        throws EvaluatorException
    {
        evaluateArtifactReferenceList( eval, reports );
    }

    private void evaluateRepositories( ExpressionEvaluator eval, List<ProjectRepository> repositories )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( repositories ) )
        {
            return;
        }

        for ( ProjectRepository repository : repositories )
        {
            repository.setId( eval.expand( repository.getId() ) );
            repository.setLayout( eval.expand( repository.getLayout() ) );
            repository.setName( eval.expand( repository.getName() ) );
            repository.setUrl( eval.expand( repository.getUrl() ) );
        }
    }

    private void evaluateScm( ExpressionEvaluator eval, Scm scm )
        throws EvaluatorException
    {
        if ( scm == null )
        {
            return;
        }

        scm.setConnection( eval.expand( scm.getConnection() ) );
        scm.setDeveloperConnection( eval.expand( scm.getDeveloperConnection() ) );
        scm.setUrl( eval.expand( scm.getUrl() ) );
    }

    private void evaluateStringList( ExpressionEvaluator eval, List<String> strings )
        throws EvaluatorException
    {
        if ( CollectionUtils.isEmpty( strings ) )
        {
            return;
        }

        // Create new list to hold post-evaluated strings.
        List<String> evaluated = new ArrayList<String>();

        // Evaluate them all
        for ( String str : strings )
        {
            evaluated.add( eval.expand( str ) );
        }

        // Populate the original list with the post-evaluated list.
        strings.clear();
        strings.addAll( evaluated );
    }
}
