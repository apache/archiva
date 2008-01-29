package org.apache.maven.archiva.database;

public class ArchivaDatabaseException
    extends Exception
{
    
    public ArchivaDatabaseException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ArchivaDatabaseException( String message )
    {
        super( message );
    }

}
