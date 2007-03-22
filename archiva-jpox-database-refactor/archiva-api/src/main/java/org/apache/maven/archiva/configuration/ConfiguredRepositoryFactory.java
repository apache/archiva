package org.apache.maven.archiva.configuration;

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

import org.apache.maven.archiva.proxy.ProxiedArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

/**
 * Create an artifact repository from the given configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ConfiguredRepositoryFactory
{
    String ROLE = ConfiguredRepositoryFactory.class.getName();

    /**
     * Create an artifact repository from the given configuration.
     *
     * @param configuration the configuration
     * @return the artifact repository
     */
    ArtifactRepository createRepository( RepositoryConfiguration configuration );

    /**
     * Create artifact repositories from the given configuration.
     *
     * @param configuration the configuration containing the repositories
     * @return the artifact repositories
     */
    List createRepositories( Configuration configuration );

    /**
     * Create a local repository from the given configuration.
     *
     * @param configuration the configuration
     * @return the local artifact repository
     */
    ArtifactRepository createLocalRepository( Configuration configuration );

    /**
     * Create an artifact repository from the given proxy repository configuration.
     *
     * @param configuration the configuration
     * @return the artifact repository
     */
    ProxiedArtifactRepository createProxiedRepository( ProxiedRepositoryConfiguration configuration );

    /**
     * Create artifact repositories from the given proxy repository configurations.
     *
     * @param configuration the configuration containing the repositories
     * @return the artifact repositories
     */
    List createProxiedRepositories( Configuration configuration );
}
