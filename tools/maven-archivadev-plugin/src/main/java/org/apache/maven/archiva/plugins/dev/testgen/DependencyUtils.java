package org.apache.maven.archiva.plugins.dev.testgen;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * DependencyUtils - common utilities for dependencies. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyUtils
{
    public static Map getManagedVersionMap( MavenProject project, ArtifactFactory factory ) throws ProjectBuildingException
    {
        DependencyManagement dependencyManagement = project.getDependencyManagement();
        Map managedVersionMap;

        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            managedVersionMap = new HashMap();

            for ( Iterator iterator = dependencyManagement.getDependencies().iterator(); iterator.hasNext(); )
            {
                Dependency dependency = (Dependency) iterator.next();

                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( dependency.getVersion() );

                    Artifact artifact =
                        factory.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                          versionRange, dependency.getType(),
                                                          dependency.getClassifier(), dependency.getScope() );

                    managedVersionMap.put( dependency.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException exception )
                {
                    throw new ProjectBuildingException( project.getId(), "Unable to parse version '"
                                    + dependency.getVersion() + "' for dependency '" + dependency.getManagementKey()
                                    + "': " + exception.getMessage(), exception );
                }
            }
        }
        else
        {
            managedVersionMap = Collections.EMPTY_MAP;
        }

        return managedVersionMap;
    }
}
