package org.apache.archiva.repository.maven;

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

import org.apache.archiva.repository.maven.dependency.tree.ArchivaRepositoryConnectorFactory;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Some static utility methods that are used by different classes.
 */
@Service("mavenSystemManager")
public class MavenSystemManager {

    static Logger log = LoggerFactory.getLogger(MavenSystemManager.class);

    private DefaultServiceLocator locator;
    private RepositorySystem system;

    @PostConstruct
    private synchronized void init() {
        locator = newLocator();
        system = newRepositorySystem(locator);

    }

    /**
     * Creates a new aether repository system session for the given directory and assigns the
     * repository to this session.
     *
     * @param localRepoDir The repository directory
     * @return The newly created session object.
     */
    public static RepositorySystemSession newRepositorySystemSession(String localRepoDir) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository repo = new LocalRepository(localRepoDir);

        DependencySelector depFilter = new AndDependencySelector(new ExclusionDependencySelector());
        session.setDependencySelector(depFilter);
        SimpleLocalRepositoryManagerFactory repFactory = new SimpleLocalRepositoryManagerFactory();
        try {
            LocalRepositoryManager manager = repFactory.newInstance(session, repo);
            session.setLocalRepositoryManager(manager);
        } catch (NoLocalRepositoryManagerException e) {
            log.error("Could not assign the repository manager to the session: {}", e.getMessage(), e);
        }

        return session;
    }

    public RepositorySystem getRepositorySystem() {
        return system;
    }

    public DefaultServiceLocator getLocator() {
        return locator;
    }

    /**
     * Finds the
     *
     * @return
     */
    public static RepositorySystem newRepositorySystem(DefaultServiceLocator locator) {
        return locator.getService(RepositorySystem.class);
    }

    public static DefaultServiceLocator newLocator() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        locator.addService(RepositoryConnectorFactory.class,
                ArchivaRepositoryConnectorFactory.class);// FileRepositoryConnectorFactory.class );
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);

        return locator;
    }
}
