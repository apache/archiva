package org.apache.maven.archiva.dependency.graph.tasks;

import org.apache.maven.archiva.dependency.graph.DependencyGraph;
import org.apache.maven.archiva.dependency.graph.GraphTask;
import org.apache.maven.archiva.dependency.graph.GraphTaskException;
import org.apache.maven.archiva.dependency.graph.walk.DependencyGraphWalker;
import org.apache.maven.archiva.dependency.graph.walk.WalkDepthFirstSearch;

/**
 * Update the scopes of the edges to what their parent node says.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.dependency.graph.GraphTask"
 *      role-hint="update-scopes"
 *      instantiation-strategy="per-lookup"
 */
public class UpdateScopesTask
    implements GraphTask
{
    public void executeTask( DependencyGraph graph )
        throws GraphTaskException
    {
        DependencyGraphWalker walker = new WalkDepthFirstSearch();
        UpdateScopesVisitor updateScopes = new UpdateScopesVisitor();
        walker.visit( graph, updateScopes );
    }

    public String getTaskId()
    {
        return "update-scopes";
    }
}
