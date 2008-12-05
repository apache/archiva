package org.apache.maven.archiva.repository.scanner;

import java.util.Date;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;

public class RepositoryContentConsumersStub
    extends RepositoryContentConsumers
{
    public RepositoryContentConsumersStub(ArchivaConfiguration archivaConfiguration)
    {
        super(archivaConfiguration);
    }

    @Override
    public Date getStartTime()
    {
        Date startTimeForTest = new Date( System.currentTimeMillis() );
        startTimeForTest.setTime( 12345678 );
        
        return startTimeForTest;
    }
}
