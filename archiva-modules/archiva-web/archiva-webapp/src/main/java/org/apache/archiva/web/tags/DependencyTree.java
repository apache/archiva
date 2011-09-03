package org.apache.archiva.web.tags;

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

import com.opensymphony.xwork2.ActionContext;
import org.apache.archiva.dependency.tree.maven2.DependencyTreeBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.model.Keys;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * DependencyTree
 *
 * @version $Id$
 *          plexus.component role="org.apache.archiva.web.tags.DependencyTree"
 */
@Service( "dependencyTree" )
public class DependencyTree
{
    private Logger log = LoggerFactory.getLogger( DependencyTree.class );

    /**
     * plexus.requirement role-hint="maven2"
     */
    @Inject
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * plexus.requirement
     */
    @Inject
    private UserRepositories userRepositories;

    public static class TreeEntry
    {
        private String pre = "";

        private String post = "";

        private Artifact artifact;

        public void setArtifact( Artifact artifact )
        {
            this.artifact = artifact;
        }

        public Artifact getArtifact()
        {
            return artifact;
        }

        public String getPost()
        {
            return post;
        }

        public String getPre()
        {
            return pre;
        }

        public void appendPre( String string )
        {
            this.pre += string;
        }

        public void appendPost( String string )
        {
            this.post += string;
        }
    }

    public List<TreeEntry> gatherTreeList( String groupId, String artifactId, String modelVersion )
        throws ArchivaException
    {
        if ( StringUtils.isBlank( groupId ) )
        {
            String emsg = "Error generating dependency tree [" + Keys.toKey( groupId, artifactId, modelVersion )
                + "]: groupId is blank.";
            log.error( emsg );
            throw new ArchivaException( emsg );
        }

        if ( StringUtils.isBlank( artifactId ) )
        {
            String emsg = "Error generating dependency tree [" + Keys.toKey( groupId, artifactId, modelVersion )
                + "]: artifactId is blank.";
            log.error( emsg );
            throw new ArchivaException( emsg );
        }

        if ( StringUtils.isBlank( modelVersion ) )
        {
            String emsg = "Error generating dependency tree [" + Keys.toKey( groupId, artifactId, modelVersion )
                + "]: version is blank.";
            log.error( emsg );
            throw new ArchivaException( emsg );
        }

        // TODO Cache the results to disk, in XML format, in the same place as the artifact is located.

        TreeListVisitor visitor = new TreeListVisitor();
        try
        {
            dependencyTreeBuilder.buildDependencyTree( userRepositories.getObservableRepositoryIds( getPrincipal() ),
                                                       groupId, artifactId, modelVersion, visitor );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new ArchivaException( "Unable to build dependency tree: " + e.getMessage(), e );
        }

        return visitor.getList();
    }

    private String getPrincipal()
    {
        return ArchivaXworkUser.getActivePrincipal( ActionContext.getContext().getSession() );
    }

    private static class TreeListVisitor
        implements DependencyNodeVisitor
    {
        private List<TreeEntry> list;

        private TreeEntry currentEntry;

        boolean firstChild = true;

        private DependencyNode firstNode;

        public TreeListVisitor()
        {
            this.list = new ArrayList<TreeEntry>();
        }

        public List<TreeEntry> getList()
        {
            return this.list;
        }

        public boolean visit( DependencyNode node )
        {
            if ( firstNode == null )
            {
                firstNode = node;
            }

            currentEntry = new TreeEntry();

            if ( firstChild )
            {
                currentEntry.appendPre( "<ul>" );
            }

            currentEntry.appendPre( "<li>" );
            currentEntry.setArtifact( node.getArtifact() );
            currentEntry.appendPost( "</li>" );
            this.list.add( currentEntry );

            if ( !node.getChildren().isEmpty() )
            {
                firstChild = true;
            }

            return true;
        }

        public boolean endVisit( DependencyNode node )
        {
            firstChild = false;

            if ( !node.getChildren().isEmpty() )
            {
                currentEntry.appendPost( "</ul>" );
            }

            if ( node == firstNode )
            {
                currentEntry.appendPost( "</ul>" );
            }

            return true;
        }
    }
}
