package org.apache.maven.archiva.repository.scanner;

import java.util.Date;

public class RepositoryContentConsumersStub
    extends RepositoryContentConsumers
{       
    public Date getStartTime()
    {
        Date startTimeForTest = new Date( System.currentTimeMillis() );
        startTimeForTest.setTime( 12345678 );
        
        return startTimeForTest;
    }
}
