package org.apache.maven.archiva.repository.scanner;

import java.util.Date;

public class RepositoryContentConsumersStub
    extends RepositoryContentConsumers
{
    private Date startTimeForTest;
    
    public void setStartTime( Date startTimeForTest )
    {
        this.startTimeForTest = startTimeForTest;
    }
    
    public Date getStartTime()
    {
        return startTimeForTest;
    }
}
