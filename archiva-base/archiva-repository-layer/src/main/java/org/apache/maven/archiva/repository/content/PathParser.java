/**
 *
 */
package org.apache.maven.archiva.repository.content;

import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.layout.LayoutException;

/**
 * @author ndeloof
 *
 */
public interface PathParser
{

    public ArtifactReference toArtifactReference( String path )
        throws LayoutException;

}
