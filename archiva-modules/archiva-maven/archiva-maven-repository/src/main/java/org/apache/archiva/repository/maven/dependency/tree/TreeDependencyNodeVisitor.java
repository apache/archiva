package org.apache.archiva.repository.maven.dependency.tree;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.maven2.model.TreeEntry;
import org.eclipse.aether.graph.DependencyVisitor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public class TreeDependencyNodeVisitor
    implements DependencyVisitor
{

    final List<TreeEntry> treeEntries;

    private TreeEntry currentEntry;

    private org.eclipse.aether.graph.DependencyNode firstDependencyNode;

    public TreeDependencyNodeVisitor( List<TreeEntry> treeEntries )
    {
        this.treeEntries = treeEntries;
    }


    @Override
    public boolean visitEnter( DependencyNode dependencyNode )
    {
        TreeEntry entry =
            new TreeEntry( getModelMapper().map( dependencyNode.getDependency().getArtifact(), Artifact.class ) );
        entry.getArtifact().setFileExtension( dependencyNode.getDependency().getArtifact().getExtension() );
        entry.getArtifact().setScope( dependencyNode.getDependency().getScope() );
        entry.setParent( currentEntry );
        currentEntry = entry;

        if ( firstDependencyNode == null )
        {
            firstDependencyNode = dependencyNode;
            treeEntries.add( currentEntry );
        }
        else
        {
            currentEntry.getParent().getChilds().add( currentEntry );
        }
        return true;
    }

    @Override
    public boolean visitLeave( DependencyNode dependencyNode )
    {
        currentEntry = currentEntry.getParent();
        return true;
    }

    private static class ModelMapperHolder
    {
        private static ModelMapper MODEL_MAPPER = new ModelMapper();

        static
        {
            MODEL_MAPPER.getConfiguration().setMatchingStrategy( MatchingStrategies.STRICT );
        }
    }

    protected ModelMapper getModelMapper()
    {
        return ModelMapperHolder.MODEL_MAPPER;
    }
}
