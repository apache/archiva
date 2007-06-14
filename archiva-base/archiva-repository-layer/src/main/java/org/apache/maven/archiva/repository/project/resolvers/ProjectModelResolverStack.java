package org.apache.maven.archiva.repository.project.resolvers;

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
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a stack of {@link ProjectModelResolver} resolvers for
 * finding/resolving an ArchivaProjectModel from multiple sources. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModelResolverStack
{
    private List resolvers;

    private List listeners;

    public ProjectModelResolverStack()
    {
        this.resolvers = new ArrayList();
        this.listeners = new ArrayList();
    }

    public void addListener( ProjectModelResolutionListener listener )
    {
        if ( listener == null )
        {
            return;
        }

        this.listeners.add( listener );
    }

    public void addProjectModelResolver( ProjectModelResolver resolver )
    {
        if ( resolver == null )
        {
            return;
        }

        this.resolvers.add( resolver );
    }

    public void clearResolvers()
    {
        this.resolvers.clear();
    }

    public ArchivaProjectModel findProject( VersionedReference projectRef )
    {
        if ( CollectionUtils.isEmpty( this.resolvers ) )
        {
            throw new IllegalStateException( "No resolvers have been defined." );
        }

        triggerResolutionStart( projectRef, this.resolvers );

        Iterator it = this.resolvers.iterator();

        while ( it.hasNext() )
        {
            ProjectModelResolver resolver = (ProjectModelResolver) it.next();

            try
            {
                triggerResolutionAttempting( projectRef, resolver );
                ArchivaProjectModel model = resolver.resolveProjectModel( projectRef );

                if ( model != null )
                {
                    // Project was found.
                    triggerResolutionSuccess( projectRef, resolver, model );
                    return model;
                }
                triggerResolutionMiss( projectRef, resolver );
            }
            catch ( ProjectModelException e )
            {
                triggerResolutionError( projectRef, resolver, e );
            }
        }

        // Project was not found in entire resolver list.
        triggerResolutionNotFound( projectRef, this.resolvers );

        return null;
    }

    public boolean isEmpty()
    {
        return this.resolvers.isEmpty();
    }

    public void removeListener( ProjectModelResolutionListener listener )
    {
        if ( listener == null )
        {
            return;
        }

        this.listeners.add( listener );
    }

    public void removeResolver( ProjectModelResolver resolver )
    {
        this.resolvers.remove( resolver );
    }

    private void triggerResolutionAttempting( VersionedReference projectRef, ProjectModelResolver resolver )
    {
        Iterator it = this.listeners.iterator();
        while ( it.hasNext() )
        {
            ProjectModelResolutionListener listener = (ProjectModelResolutionListener) it.next();

            try
            {
                listener.resolutionAttempting( projectRef, resolver );
            }
            catch ( Exception e )
            {
                // do nothing with exception.
            }
        }
    }

    private void triggerResolutionError( VersionedReference projectRef, ProjectModelResolver resolver, Exception cause )
    {
        Iterator it = this.listeners.iterator();
        while ( it.hasNext() )
        {
            ProjectModelResolutionListener listener = (ProjectModelResolutionListener) it.next();

            try
            {
                listener.resolutionError( projectRef, resolver, cause );
            }
            catch ( Exception e )
            {
                // do nothing with exception.
            }
        }
    }

    private void triggerResolutionMiss( VersionedReference projectRef, ProjectModelResolver resolver )
    {
        Iterator it = this.listeners.iterator();
        while ( it.hasNext() )
        {
            ProjectModelResolutionListener listener = (ProjectModelResolutionListener) it.next();

            try
            {
                listener.resolutionMiss( projectRef, resolver );
            }
            catch ( Exception e )
            {
                // do nothing with exception.
            }
        }
    }

    private void triggerResolutionNotFound( VersionedReference projectRef, List resolvers )
    {
        Iterator it = this.listeners.iterator();
        while ( it.hasNext() )
        {
            ProjectModelResolutionListener listener = (ProjectModelResolutionListener) it.next();

            try
            {
                listener.resolutionNotFound( projectRef, resolvers );
            }
            catch ( Exception e )
            {
                // do nothing with exception.
            }
        }
    }

    private void triggerResolutionStart( VersionedReference projectRef, List resolvers )
    {
        Iterator it = this.listeners.iterator();
        while ( it.hasNext() )
        {
            ProjectModelResolutionListener listener = (ProjectModelResolutionListener) it.next();

            try
            {
                listener.resolutionStart( projectRef, resolvers );
            }
            catch ( Exception e )
            {
                // do nothing with exception.
            }
        }
    }

    private void triggerResolutionSuccess( VersionedReference projectRef, ProjectModelResolver resolver,
                                           ArchivaProjectModel model )
    {
        Iterator it = this.listeners.iterator();
        while ( it.hasNext() )
        {
            ProjectModelResolutionListener listener = (ProjectModelResolutionListener) it.next();

            try
            {
                listener.resolutionSuccess( projectRef, resolver, model );
            }
            catch ( Exception e )
            {
                // do nothing with exception.
            }
        }
    }
}
