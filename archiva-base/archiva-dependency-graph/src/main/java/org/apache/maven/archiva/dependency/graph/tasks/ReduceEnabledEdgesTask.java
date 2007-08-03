package org.apache.maven.archiva.dependency.graph.tasks;

import org.apache.commons.collections.functors.TruePredicate;
import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;

/**
 * ReduceEnabledEdgesTask 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.dependency.graph.GraphTask"
 *      role-hint="reduce-enabled-edges"
 *      instantiation-strategy="per-lookup"
 */
public class ReduceEnabledEdgesTask
    implements GraphTask
{
    public void executeTask( DependencyGraph graph )
    {
        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        walker.setEdgePredicate( TruePredicate.getInstance() );
        ReduceEnabledEdgesVisitor reduceEnabledEdgesResolver = new ReduceEnabledEdgesVisitor();
        walker.visit( graph, reduceEnabledEdgesResolver );
    }

    public String getTaskId()
    {
        return "reduce-enabled-edges";
    }
}
