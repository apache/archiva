package org.apache.maven.archiva.dependency.graph.tasks;

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;

/**
 * ReduceScopeTask 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.dependency.graph.GraphTask"
 *      role-hint="reduce-scope"
 *      instantiation-strategy="per-lookup"
 */
public class ReduceScopeTask
    implements GraphTask
{
    private String scope;

    public ReduceScopeTask( String scope )
    {
        this.scope = scope;
    }

    public void executeTask( DependencyGraph graph )
    {
        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        ReduceScopeVisitor reduceScopeResolver = new ReduceScopeVisitor( this.scope );
        walker.visit( graph, reduceScopeResolver );
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String getTaskId()
    {
        return "reduce-scope";
    }
}
