package org.apache.maven.archiva.database;

import org.apache.maven.archiva.database.key.MetadataKey;
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
        
        MetadataKey testMetadataKey = new MetadataKey();
        
        testMetadataKey.setArtifactId( "testArtfiactId" );
        testMetadataKey.setGroupId( "testGroupId" );
        testMetadataKey.setVersion( "testVersion" );
        
        store.addMetadataKey( testMetadataKey );       
    }
    
    
}
