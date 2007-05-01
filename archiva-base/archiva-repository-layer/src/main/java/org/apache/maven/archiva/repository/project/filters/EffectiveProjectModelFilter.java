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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaModelCloner;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelFilter;
import org.apache.maven.archiva.repository.project.ProjectModelMerge;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;

import java.util.ArrayList;
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
 *                   instantiation-strategy="per-lookup"
 */
public class EffectiveProjectModelFilter implements ProjectModelFilter
{
    /**
     * @plexus.requirement role-hint="expression"
     */
    private ProjectModelFilter expressionFilter;
    
    private List projectModelResolvers;
    
    public EffectiveProjectModelFilter()
    {
        projectModelResolvers = new ArrayList();
    }

    public void addProjectModelResolver( ProjectModelResolver resolver )
    {
        if ( resolver == null )
        {
            return;
        }

        this.projectModelResolvers.add( resolver );
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

        if ( this.projectModelResolvers.isEmpty() )
        {
            throw new IllegalStateException( "Unable to build effective pom with no project model resolvers defined." );
        }

        // Clone submitted project (so that we don't mess with it) 
        ArchivaProjectModel effectiveProject = ArchivaModelCloner.clone( project );

        // Setup Expression Evaluation pieces.
        effectiveProject = expressionFilter.filter( effectiveProject );

        debug( "Starting build of effective with: " + effectiveProject );

        // Merge in all the parent poms.
        effectiveProject = mergeParent( effectiveProject );

        // Resolve dependency versions from dependency management.
        applyDependencyManagement( effectiveProject );

        // Return what we got.
        return effectiveProject;
    }

    public void removeResolver( ProjectModelResolver resolver )
    {
        this.projectModelResolvers.remove( resolver );
    }

    private void applyDependencyManagement( ArchivaProjectModel pom )
    {
        if ( ( pom.getDependencyManagement() == null ) || ( pom.getDependencyTree() == null ) )
        {
            // Nothing to do. All done!
            return;
        }
        
        if ( pom.getDependencyManagement().isEmpty() || pom.getDependencyTree().isEmpty() )
        {
            // Nothing to do. All done!
            return;
        }

        Map managedDependencies = createDependencyMap( pom.getDependencyManagement() );
        Iterator it = pom.getDependencyTree().getDependencyNodes().iterator();
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

    private void debug( String msg )
    {
        System.out.println( "## " + msg );
    }

    private ArchivaProjectModel findProject( VersionedReference projectRef )
    {
        debug( "Trying to find project: " + projectRef );
        Iterator it = this.projectModelResolvers.iterator();

        while ( it.hasNext() )
        {
            ProjectModelResolver resolver = (ProjectModelResolver) it.next();

            try
            {
                debug( "Trying to find in " + resolver.getClass().getName() );
                ArchivaProjectModel model = resolver.resolveProjectModel( projectRef );

                if ( model != null )
                {
                    debug( "Found it!: " + model );
                    return model;
                }
                debug( "Not found." );
            }
            catch ( ProjectModelException e )
            {
                // TODO: trigger notifier of problem?
                e.printStackTrace();
            }
        }

        // TODO: Document that project was not found. (Use monitor?)

        return null;
    }

    private ArchivaProjectModel mergeParent( ArchivaProjectModel pom )
        throws ProjectModelException
    {
        ArchivaProjectModel mixedProject;

        debug( "Parent: " + pom.getParentProject() );

        if ( pom.getParentProject() != null )
        {
            // Use parent reference.
            VersionedReference parentRef = pom.getParentProject();

            debug( "Has parent: " + parentRef );

            // Find parent using resolvers.
            ArchivaProjectModel parentProject = findProject( parentRef );

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
            debug( "No parent found" );

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
        debug( "Mix in Super POM: " + pom );

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
