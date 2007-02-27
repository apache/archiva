package org.apache.maven.archiva.database;

import org.apache.maven.archiva.database.key.MetadataKey;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.ibatis.PlexusIbatisHelper;

public class RepositoryMetadataDatabaseTest
    extends PlexusTestCase
{
    /**
     * @plexus.requirement 
     */
    protected PlexusIbatisHelper ibatisHelper;
    
    protected void setUp()
        throws Exception
    {
        // TODO Auto-generated method stub
        super.setUp();
    }

    public void testRepositoryMetadataCreationAndDeletion() throws Exception
    {
        RepositoryMetadataDatabase db = (RepositoryMetadataDatabase) lookup( "org.apache.maven.archiva.database.RepositoryMetadataDatabase", "default" );
        
        assertNotNull( db );
        assertTrue( db.tableExists( "RepositoryMetadata" ) );
        assertTrue( db.tableExists( "MetadataKeys" ) );
        
        db.dropTable( "RepositoryMetadata" );
        db.dropTable( "MetadataKeys" );
        
        assertFalse( db.tableExists( "RepositoryMetadata" ) );
        assertFalse( db.tableExists( "MetadataKeys" ) );
    }
    
    public void testMetadataKeyRetrieval() throws Exception
    {
        RepositoryMetadataDatabase db = (RepositoryMetadataDatabase) lookup( "org.apache.maven.archiva.database.RepositoryMetadataDatabase", "default" );
        
        Metadata metadata = new Metadata();
        metadata.setArtifactId( "testArtifactId" );
        metadata.setGroupId( "testGroupId" );
        metadata.setVersion( "testVersion" );
           
        MetadataKey metadataKey = db.getMetadataKey( metadata );
        
        assertTrue( metadataKey.getMetadataKey() > 0 );
        assertEquals( metadataKey.getArtifactId(), metadata.getArtifactId() );
        assertEquals( metadataKey.getGroupId(), metadata.getGroupId() );
        assertEquals( metadataKey.getVersion(), metadata.getVersion() );        
        
        db.dropTable( "RepositoryMetadata" );
        db.dropTable( "MetadataKeys" );
        
        assertFalse( db.tableExists( "RepositoryMetadata" ) );
        assertFalse( db.tableExists( "MetadataKeys" ) );
        
    }
    
    
}
