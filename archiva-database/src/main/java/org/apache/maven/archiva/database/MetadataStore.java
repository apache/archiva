package org.apache.maven.archiva.database;

import org.apache.maven.archiva.database.key.MetadataKey;
import org.apache.maven.artifact.repository.metadata.Metadata;

public interface MetadataStore 
{
    public static final String ROLE = MetadataStore.class.getName();
    
    public void addMetadataKey( MetadataKey metadataKey ) throws MetadataStoreException;
    
}
