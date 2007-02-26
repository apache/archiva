package org.apache.maven.archiva.database;

import org.apache.maven.archiva.database.key.MetadataKey;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.plexus.PlexusTestCase;

public class IbatisMetadataStoreTest
    extends PlexusTestCase
{

    protected void setUp()
        throws Exception
    {
        // TODO Auto-generated method stub
        super.setUp();
    }

    public void testMetadataKeysInitialization() throws Exception
    {
        MetadataStore store = (MetadataStore) lookup( MetadataStore.ROLE, "ibatis" );
        
        assertNotNull( store );
    }
    
    public void testMetadataKeyRetrieval() throws Exception
    {
        MetadataStore store = (MetadataStore) lookup( MetadataStore.ROLE, "ibatis" );
        
        Metadata metadata = new Metadata();
        metadata.setArtifactId( "testArtifactId" );
        metadata.setGroupId( "testGroupId" );
        metadata.setVersion( "testVersion" );
        
        store.addMetadata( metadata );                
        
        MetadataKey metadataKey = store.getMetadataKey( metadata );
        
        assertTrue( metadataKey.getMetadataKey() > 0 );
        assertEquals( metadataKey.getArtifactId(), metadata.getArtifactId() );
        assertEquals( metadataKey.getGroupId(), metadata.getGroupId() );
        assertEquals( metadataKey.getVersion(), metadata.getVersion() );
        
    }
    
    
}
