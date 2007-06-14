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
import org.apache.maven.archiva.repository.project.resolvers.ProjectModelResolverStack;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builder for the Effective Project Model.  
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.project.ProjectModelFilter" 
 *                   role-hint="effective" 
 */
public class EffectiveProjectModelFilter
    extends AbstractLogEnabled
    implements ProjectModelFilter
{
    private ProjectModelFilter expressionFilter = new ProjectModelExpressionFilter();

    private ProjectModelResolverStack projectModelResolverStack;

    public EffectiveProjectModelFilter()
    {
        projectModelResolverStack = new ProjectModelResolverStack();
    }

    public void setProjectModelResolverStack( ProjectModelResolverStack resolverStack )
    {
        this.projectModelResolverStack = resolverStack;
    }

    public ProjectModelResolverStack getProjectModelResolverStack()
    {
        return this.projectModelResolverStack;
    }

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

        if ( this.projectModelResolverStack.isEmpty() )
        {
            throw new IllegalStateException( "Unable to build effective pom with no project model resolvers defined." );
        }

        // Clone submitted project (so that we don't mess with it) 
        ArchivaProjectModel effectiveProject = ArchivaModelCloner.clone( project );

        // Setup Expression Evaluation pieces.
        effectiveProject = expressionFilter.filter( effectiveProject );

        getLogger().debug( "Starting build of effective with: " + effectiveProject );

        // Merge in all the parent poms.
        effectiveProject = mergeParent( effectiveProject );

        // Resolve dependency versions from dependency management.
        applyDependencyManagement( effectiveProject );

        // Return what we got.
        return effectiveProject;
    }

    private Logger logger;

    protected Logger getLogger()
    {
        if ( logger == null )
        {
            logger = super.getLogger();
            if ( logger == null )
            {
                logger = new ConsoleLogger( ConsoleLogger.LEVEL_INFO, this.getClass().getName() );
            }
        }

        return logger;
    }

    private void applyDependencyManagement( ArchivaProjectModel pom )
    {
        if ( CollectionUtils.isEmpty( pom.getDependencyManagement() )
            || CollectionUtils.isEmpty( pom.getDependencies() ) )
        {
            // Nothing to do. All done!
            return;
        }

        Map managedDependencies = createDependencyMap( pom.getDependencyManagement() );
        Iterator it = pom.getDependencies().iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
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

        getLogger().debug( "Parent: " + pom.getParentProject() );

        if ( pom.getParentProject() != null )
        {
            // Use parent reference.
            VersionedReference parentRef = pom.getParentProject();

            getLogger().debug( "Has parent: " + parentRef );

            // Find parent using resolvers.
            ArchivaProjectModel parentProject = this.projectModelResolverStack.findProject( parentRef );

            if ( parentProject != null )
            {
                parentProject = expressionFilter.filter( parentProject );
                parentProject = mergeParent( parentProject );
                mixedProject = ProjectModelMerge.merge( pom, parentProject );
            }
            else
            {
                // Shortcircuit due to missing parent pom.
                // TODO: Document this via monitor.
                mixedProject = mixinSuperPom( pom );
            }
        }
        else
        {
            getLogger().debug( "No parent found" );

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
        getLogger().debug( "Mix in Super POM: " + pom );

        return pom;
    }

    private static Map createDependencyMap( List dependencies )
    {
        Map ret = new HashMap();

        Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
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
}
