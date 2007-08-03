package org.apache.maven.archiva.scheduled.tasks;

/**
 * DataRefreshTask - task for discovering changes in the repository 
 * and updating all associated data. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id: DataRefreshTask.java 525176 2007-04-03 15:21:33Z joakime $
 */
public class DatabaseTask
    implements ArchivaTask
{
   
    String name;
    
    String queuePolicy;

    long maxExecutionTime;

    public long getMaxExecutionTime()
    {
        return maxExecutionTime;
    }

    public void setMaxExecutionTime( long maxExecutionTime )
    {
        this.maxExecutionTime = maxExecutionTime;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getQueuePolicy()
    {
        return queuePolicy;
    }

    public void setQueuePolicy( String queuePolicy )
    {
        this.queuePolicy = queuePolicy;
    }
}
