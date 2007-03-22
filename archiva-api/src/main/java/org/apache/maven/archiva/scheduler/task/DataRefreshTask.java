package org.apache.maven.archiva.scheduler.task;

/**
 * DataRefreshTask - task for discovering changes in the repository 
 * and updating all associated data. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DataRefreshTask
    implements RepositoryTask
{
    private String jobName;

    private String policy;

    public String getJobName()
    {
        return jobName;
    }

    public String getQueuePolicy()
    {
        return policy;
    }

    public void setJobName( String jobName )
    {
        this.jobName = jobName;
    }

    public void setQueuePolicy( String policy )
    {
        this.policy = policy;
    }

    public long getMaxExecutionTime()
    {
        return 0;
    }
}
