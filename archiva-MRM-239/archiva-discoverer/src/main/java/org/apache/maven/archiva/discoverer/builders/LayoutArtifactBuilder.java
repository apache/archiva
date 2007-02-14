package org.apache.maven.archiva.discoverer.builders;

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

import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * LayoutArtifactBuilder 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @todo this concept should really exist inside of the {@link ArtifactRepositoryLayout}
 */
public interface LayoutArtifactBuilder
{
    public Artifact build( String pathToArtifact ) throws BuilderException, DiscovererException;
}
