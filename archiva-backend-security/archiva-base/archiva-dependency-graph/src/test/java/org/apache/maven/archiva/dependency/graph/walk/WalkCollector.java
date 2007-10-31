/**
 * 
 */
package org.apache.maven.archiva.dependency.graph.walk;

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphVisitor;
import org.apache.maven.archiva.model.ArtifactReference;

import java.util.ArrayList;
import java.util.List;

class WalkCollector
    implements DependencyGraphVisitor
{
    private List walkPath = new ArrayList();

    private int countDiscoverGraph = 0;

    private int countFinishGraph = 0;

    private int countDiscoverNode = 0;

    private int countFinishNode = 0;

    private int countDiscoverEdge = 0;

    private int countFinishEdge = 0;

    public void discoverEdge( DependencyGraphEdge edge )
    {
        countDiscoverEdge++;
    }

    public void discoverGraph( DependencyGraph graph )
    {
        countDiscoverGraph++;
    }

    public void discoverNode( DependencyGraphNode node )
    {
        countDiscoverNode++;
        walkPath.add( ArtifactReference.toKey( node.getArtifact() ) );
    }

    public void finishEdge( DependencyGraphEdge edge )
    {
        countFinishEdge++;
    }

    public void finishGraph( DependencyGraph graph )
    {
        countFinishGraph++;
    }

    public void finishNode( DependencyGraphNode node )
    {
        countFinishNode++;
    }

    public List getCollectedPath()
    {
        return walkPath;
    }

    public int getCountDiscoverEdge()
    {
        return countDiscoverEdge;
    }

    public int getCountDiscoverGraph()
    {
        return countDiscoverGraph;
    }

    public int getCountDiscoverNode()
    {
        return countDiscoverNode;
    }

    public int getCountFinishEdge()
    {
        return countFinishEdge;
    }

    public int getCountFinishGraph()
    {
        return countFinishGraph;
    }

    public int getCountFinishNode()
    {
        return countFinishNode;
    }

}