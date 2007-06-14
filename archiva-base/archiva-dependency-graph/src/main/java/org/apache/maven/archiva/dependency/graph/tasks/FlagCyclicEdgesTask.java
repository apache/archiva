package org.apache.maven.archiva.dependency.graph.tasks;

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;

/**
 * FlagCyclicEdgesTask 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.dependency.graph.GraphTask"
 *      role-hint="flag-cyclic-edges"
 *      instantiation-strategy="per-lookup"
 */
public class FlagCyclicEdgesTask
    implements GraphTask
{

    public void executeTask( DependencyGraph graph )
    {
        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        FlagExcludedEdgesVisitor excludedEdgeResolver = new FlagExcludedEdgesVisitor();
        walker.visit( graph, excludedEdgeResolver );
    }

    public String getTaskId()
    {
        return "flag-cyclic-edges";
    }
}
