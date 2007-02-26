package org.apache.maven.archiva.database;

public class MetadataStoreException
    extends Exception
{
    
    public MetadataStoreException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public MetadataStoreException( String message )
    {
        super( message );
    }

}
