package org.apache.maven.archiva.dependency.graph;

/**
 * GraphListener 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface GraphListener
{
    public void graphError( GraphTaskException e, DependencyGraph currentGraph );

    public void graphPhaseEvent( GraphPhaseEvent event );

    public void dependencyResolutionEvent( DependencyResolutionEvent event );
}
