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
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.apache.maven.archiva.repository.project.ProjectModelMerge;
import org.apache.maven.archiva.repository.project.ProjectModelResolverFactory;
import org.codehaus.plexus.cache.Cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builder for the Effective Project Model.  
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.project.ProjectModelFilter" 
 *                   role-hint="effective" 
 */
public class EffectiveProjectModelFilter
    implements ProjectModelFilter
{
    private ProjectModelFilter expressionFilter = new ProjectModelExpressionFilter();

    /**
     * @plexus.requirement
     */
    private ProjectModelResolverFactory resolverFactory;

    /**
     * @plexus.requirement role-hint="effective-project-cache"
     */
    private Cache effectiveProjectCache;

    /**
     * Take the provided {@link ArchivaProjectModel} and build the effective {@link ArchivaProjectModel}.
     * 
     * Steps:
     * 1) Expand any expressions / properties.
     * 2) Walk the parent project references and merge.
     * 3) Apply dependency management settings.
     * 
     * @param project the project to create the effective {@link ArchivaProjectModel} from.
     * @return a the effective {@link ArchivaProjectModel}.
     * @throws ProjectModelException if there was a problem building the effective pom.
     */
    public ArchivaProjectModel filter( final ArchivaProjectModel project )
        throws ProjectModelException
    {
        if ( project == null )
        {
            return null;
        }

        if ( resolverFactory.getCurrentResolverStack().isEmpty() )
        {
            throw new IllegalStateException( "Unable to build effective pom with no project model resolvers defined." );
        }

        ArchivaProjectModel effectiveProject;
        String projectKey = toProjectKey( project );

        synchronized ( effectiveProjectCache )
        {
            if ( effectiveProjectCache.hasKey( projectKey ) )
            {
                DEBUG( "Fetching (from cache/projectKey): " + projectKey );
                effectiveProject = (ArchivaProjectModel) effectiveProjectCache.get( projectKey );
                return effectiveProject;
            }
        }

        // Clone submitted project (so that we don't mess with it) 
        effectiveProject = ArchivaModelCloner.clone( project );

        // Setup Expression Evaluation pieces.
        effectiveProject = expressionFilter.filter( effectiveProject );

        DEBUG( "Starting build of effective with: " + effectiveProject );

        // Merge in all the parent poms.
        effectiveProject = mergeParent( effectiveProject );

        // Resolve dependency versions from dependency management.
        applyDependencyManagement( effectiveProject );

        synchronized ( effectiveProjectCache )
        {
            DEBUG( "Putting (to cache/projectKey): " + projectKey );
            effectiveProjectCache.put( projectKey, effectiveProject );
        }

        // Return what we got.
        return effectiveProject;
    }

    private void applyDependencyManagement( ArchivaProjectModel pom )
    {
        if ( CollectionUtils.isEmpty( pom.getDependencyManagement() )
            || CollectionUtils.isEmpty( pom.getDependencies() ) )
        {
            // Nothing to do. All done!
            return;
        }

        Map<String, Dependency> managedDependencies = createDependencyMap( pom.getDependencyManagement() );
        Iterator<Dependency> it = pom.getDependencies().iterator();
        while ( it.hasNext() )
        {
            Dependency dep = it.next();
            String key = toVersionlessDependencyKey( dep );

            // Do we need to do anything?
            if ( managedDependencies.containsKey( key ) )
            {
                Dependency mgmtDep = (Dependency) managedDependencies.get( key );

                dep.setVersion( mgmtDep.getVersion() );
                dep.setScope( mgmtDep.getScope() );
                dep.setExclusions( ProjectModelMerge.mergeExclusions( dep.getExclusions(), mgmtDep.getExclusions() ) );
            }
        }
    }

    private ArchivaProjectModel mergeParent( ArchivaProjectModel pom )
        throws ProjectModelException
    {
        ArchivaProjectModel mixedProject;

        DEBUG( "Project: " + toProjectKey( pom ) );

        if ( pom.getParentProject() != null )
        {
            // Use parent reference.
            VersionedReference parentRef = pom.getParentProject();

            String parentKey = VersionedReference.toKey( parentRef );

            DEBUG( "Has parent: " + parentKey );

            ArchivaProjectModel parentProject;

            synchronized ( effectiveProjectCache )
            {
                // is the pre-merged parent in the cache? 
                if ( effectiveProjectCache.hasKey( parentKey ) )
                {
                    DEBUG( "Fetching (from cache/parentKey): " + parentKey );
                    // Use the one from the cache.
                    parentProject = (ArchivaProjectModel) effectiveProjectCache.get( parentKey );
                }
                else
                {
                    // Look it up, using resolvers.
                    parentProject = this.resolverFactory.getCurrentResolverStack().findProject( parentRef );
                }
            }

            if ( parentProject != null )
            {
                // Merge the pom with the parent pom.
                parentProject = expressionFilter.filter( parentProject );
                parentProject = mergeParent( parentProject );

                // Cache the pre-merged parent.
                synchronized ( effectiveProjectCache )
                {
                    DEBUG( "Putting (to cache/parentKey/merged): " + parentKey );
                    // Add the merged parent pom to the cache.
                    effectiveProjectCache.put( parentKey, parentProject );
                }

                // Now merge the parent with the current
                mixedProject = ProjectModelMerge.merge( pom, parentProject );
            }
            else
            {
                // Shortcircuit due to missing parent pom.
                // TODO: Document this via a monitor.
                mixedProject = mixinSuperPom( pom );

                // Cache the non-existant parent.
                synchronized ( effectiveProjectCache )
                {
                    DEBUG( "Putting (to cache/parentKey/basicPom): " + parentKey );
                    // Add the basic pom to cache.
                    effectiveProjectCache.put( parentKey, createBasicPom( parentRef ) );
                }
            }
        }
        else
        {
            DEBUG( "No parent found" );

            /* Mix in the super-pom.
             * 
             * Super POM from maven/components contains many things.
             * However, for purposes of archiva, only the <repositories>
             * and <pluginRepositories> sections are of any value.
             */

            mixedProject = mixinSuperPom( pom );
        }

        return mixedProject;
    }

    private ArchivaProjectModel createBasicPom( VersionedReference ref )
    {
        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( ref.getGroupId() );
        model.setArtifactId( ref.getArtifactId() );
        model.setVersion( ref.getVersion() );
        model.setPackaging( "jar" );

        return model;
    }

    /**
     * Super POM from maven/components contains many things.
     * However, for purposes of archiva, only the <repositories>
     * and <pluginRepositories> sections are of any value.
     * 
     * @param pom
     * @return
     */
    private ArchivaProjectModel mixinSuperPom( ArchivaProjectModel pom )
    {
        // TODO: add super pom repositories.
        DEBUG( "Mix in Super POM: " + pom );

        return pom;
    }

    private static Map<String, Dependency> createDependencyMap( List<Dependency> dependencies )
    {
        Map<String, Dependency> ret = new HashMap<String, Dependency>();

        Iterator<Dependency> it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = it.next();
            String key = toVersionlessDependencyKey( dep );
            ret.put( key, dep );
        }

        return ret;
    }

    private static String toVersionlessDependencyKey( Dependency dep )
    {
        StringBuffer key = new StringBuffer();

        key.append( dep.getGroupId() ).append( ":" ).append( dep.getArtifactId() );
        key.append( StringUtils.defaultString( dep.getClassifier() ) ).append( ":" );
        key.append( dep.getType() );

        return key.toString();
    }

    private String toProjectKey( ArchivaProjectModel project )
    {
        StringBuffer key = new StringBuffer();

        key.append( project.getGroupId() ).append( ":" );
        key.append( project.getArtifactId() ).append( ":" );
        key.append( project.getVersion() );

        return key.toString();
    }

    private void DEBUG( String msg )
    {
        // Used in debugging of this object.
        // System.out.println( "[EffectiveProjectModelFilter] " + msg );
    }
}
