package org.apache.maven.archiva.web.tags;

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
import org.apache.maven.archiva.dependency.DependencyGraphFactory;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.GraphTaskException;
import org.apache.maven.archiva.dependency.graph.walk.BaseVisitor;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.VersionedReference;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * DependencyTree 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.web.tags.DependencyTree" 
 */
public class DependencyTree
    extends AbstractLogEnabled
    implements Initializable
{
    /**
     * @plexus.requirement
     *              role="org.apache.maven.archiva.dependency.graph.DependencyGraphBuilder"
     *              role-hint="project-model"
     */
    private DependencyGraphBuilder graphBuilder;

    private DependencyGraphFactory graphFactory = new DependencyGraphFactory();

    public class TreeEntry
    {
        private String pre = "";

        private String post = "";

        private ArtifactReference artifact;

        public void setArtifact( ArtifactReference artifact )
        {
            this.artifact = artifact;
        }

        public ArtifactReference getArtifact()
        {
            return artifact;
        }

        public String getPost()
        {
            return post;
        }

        public void setPost( String post )
        {
            this.post = post;
        }

        public String getPre()
        {
            return pre;
        }

        public void setPre( String pre )
        {
            this.pre = pre;
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

    public List gatherTreeList( String groupId, String artifactId, String modelVersion, String nodevar,
                                PageContext pageContext )
        throws JspException
    {
        if ( StringUtils.isBlank( groupId ) )
        {
            String emsg = "Error generating dependency tree: groupId is blank.";
            getLogger().error( emsg );
            throw new JspException( emsg );
        }

        DependencyGraph graph = fetchGraph( groupId, artifactId, modelVersion );

        if ( graph == null )
        {
            throw new JspException( "Graph is null." );
        }

        TreeListVisitor treeListVisitor = new TreeListVisitor();
        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        walker.visit( graph, treeListVisitor );

        return treeListVisitor.getList();
    }

    class TreeListVisitor
        extends BaseVisitor
    {
        private List list;

        private int walkDepth;

        private int outputDepth;

        private Stack entryStack = new Stack();

        private TreeEntry currentEntry;

        public TreeListVisitor()
        {
            this.list = new ArrayList();
        }

        public List getList()
        {
            return this.list;
        }

        public void discoverGraph( DependencyGraph graph )
        {
            super.discoverGraph( graph );
            this.list.clear();
            this.entryStack.clear();
            walkDepth = 0;
            outputDepth = -1;
        }

        public void discoverNode( DependencyGraphNode node )
        {
            super.discoverNode( node );
            currentEntry = new TreeEntry();

            while ( walkDepth > outputDepth )
            {
                currentEntry.appendPre( "<ul>" );
                outputDepth++;
            }
            currentEntry.appendPre( "<li>" );
            currentEntry.setArtifact( node.getArtifact() );
            currentEntry.appendPost( "</li>" );
            this.list.add( currentEntry );
            this.entryStack.push( currentEntry );
        }

        public void finishNode( DependencyGraphNode node )
        {
            super.finishNode( node );

            while ( walkDepth < outputDepth )
            {
                currentEntry.appendPost( "</ul>" );
                outputDepth--;
            }

            this.entryStack.pop();
        }

        public void discoverEdge( DependencyGraphEdge edge )
        {
            super.discoverEdge( edge );
            walkDepth++;
        }

        public void finishEdge( DependencyGraphEdge edge )
        {
            super.finishEdge( edge );
            walkDepth--;
        }
    }

    private DependencyGraph fetchGraph( String groupId, String artifactId, String modelVersion )
    {
        // TODO Cache the results to disk, in XML format, in the same place as the artifact is located.

        VersionedReference projectRef = new VersionedReference();
        projectRef.setGroupId( groupId );
        projectRef.setArtifactId( artifactId );
        projectRef.setVersion( modelVersion );

        try
        {
            DependencyGraph depGraph = graphFactory.getGraph( projectRef );
 
            return depGraph;
        }
        catch ( GraphTaskException e )
        {
            getLogger().warn( "Unable to get Graph: " + e.getMessage(), e );
            return null;
        }
    }

    public void initialize()
        throws InitializationException
    {
        this.graphFactory.setGraphBuilder( graphBuilder );
        this.graphFactory.setDesiredScope( DependencyScope.TEST );
    }
}
