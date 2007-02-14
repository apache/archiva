package org.apache.maven.archiva.discoverer.builders;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.PlexusTestCase;

public class AbstractLayoutArtifactBuilderTestCase
extends PlexusTestCase
{

    protected void assertArtifact( String groupId, String artifactId, String version, String type, String classifier, Artifact artifact )
    {
        assertNotNull( "Artifact cannot be null.", artifact );
    
        assertEquals( "Artifact groupId", groupId, artifact.getGroupId() );
        assertEquals( "Artifact artifactId", artifactId, artifact.getArtifactId() );
        assertEquals( "Artifact version", version, artifact.getVersion() );
        assertEquals( "Artifact type", type, artifact.getType() );
    
        if ( StringUtils.isNotBlank( classifier ) )
        {
            assertEquals( "Artifact classifier", classifier, artifact.getClassifier() );
        }
    }
    
}
