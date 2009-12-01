package org.apache.archiva.dependency.tree.maven2;

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

import java.util.List;

import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * Builds a tree of dependencies for a given Maven project. Customized wrapper for maven-dependency-tree to use
 * maven-model-builder instead of maven-project.
 */
public interface DependencyTreeBuilder
{
    /**
     * Builds a tree of dependencies for the specified Maven project.
     *
     * @param repositoryIds the list of repositories to search for metadata
     * @param groupId       the project groupId to build the tree for
     * @param artifactId    the project artifactId to build the tree for
     * @param version       the project version to build the tree for
     * @param nodeVisitor   visitor to apply to all nodes discovered
     * @throws DependencyTreeBuilderException if the dependency tree cannot be resolved
     */
    public void buildDependencyTree( List<String> repositoryIds, String groupId, String artifactId, String version,
                                     DependencyNodeVisitor nodeVisitor )
        throws DependencyTreeBuilderException;
}
