package org.apache.maven.archiva.database.key;

public class MetadataKey {

	private String groupId;
	private String artifactId;
	private String version;
	private int metadataKey;
    
    public MetadataKey( String groupId, String artifactId, String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
   
    public MetadataKey() {}
    
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public int getMetadataKey() {
		return metadataKey;
	}
	public void setMetadataKey(int id) {
		this.metadataKey = id;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	
	
}
