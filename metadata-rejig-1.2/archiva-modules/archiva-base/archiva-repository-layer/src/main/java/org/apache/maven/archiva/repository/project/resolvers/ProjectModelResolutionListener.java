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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;

import java.util.List;

/**
 * ProjectModelResolutionListener 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ProjectModelResolutionListener
{
    /**
     * Indicates that the resolution process has started for a specific project.
     * 
     * @param projectRef the project reference.
     * @param resolverList the {@link List} of {@link ProjectModelResolver}'s that will be searched.
     * @see #resolutionSuccess(VersionedReference, ProjectModelResolver, ArchivaProjectModel)
     * @see #resolutionNotFound(VersionedReference, List)
     */
    public void resolutionStart( VersionedReference projectRef, List resolverList );

    /**
     * Indicates that a resolution against a specific resolver is about 
     * to occur.
     * 
     * @param projectRef the project reference.
     * @param resolver the resolver to attempt resolution on.
     */
    public void resolutionAttempting( VersionedReference projectRef, ProjectModelResolver resolver );
    
    /**
     * Indicates that a resolution against a specific resolver resulted
     * in in a missed resolution.
     * 
     * "Miss" in this case refers to an attempt against a resolver, and that
     * resolver essentially responds with a "not found here" response.
     * 
     * @param projectRef the project reference.
     * @param resolver the resolver the attempt was made on.
     */
    public void resolutionMiss( VersionedReference projectRef, ProjectModelResolver resolver );
    
    /**
     * Indicates that a resolution against the specific resolver has
     * caused an error.
     * 
     * @param projectRef the project reference.
     * @param resolver the (optional) resolver on which the error occured.
     * @param cause the cause of the error.
     */
    public void resolutionError( VersionedReference projectRef, ProjectModelResolver resolver, Exception cause );
    
    /**
     * Indicates that a resolution process has finished, and the requested
     * projectRef has been found. 
     * 
     * @param projectRef the project reference.
     * @param resolver the resolver on which success occured.
     * @param model the resolved model. 
     * @see #resolutionStart(VersionedReference, List)
     */
    public void resolutionSuccess( VersionedReference projectRef, ProjectModelResolver resolver, ArchivaProjectModel model );
    
    /**
     * Indicates that the resolution process has finished, and the requested
     * projectRef could not be found.
     * 
     * @param projectRef the project reference.
     * @param resolverList the {@link List} of {@link ProjectModelResolver}'s that was be searched.
     * @see #resolutionStart(VersionedReference, List)
     */
    public void resolutionNotFound( VersionedReference projectRef, List resolverList );
}
