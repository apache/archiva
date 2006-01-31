package org.apache.maven.repository.proxy;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;

/**
 * @author Edwin Punzalan
 */
public interface ProxyManager
{
    File getArtifactFile( Artifact artifact )
        throws TransferFailedException, ResourceDoesNotExistException, IOException;
    InputStream getArtifactAsStream( Artifact artifact )
        throws TransferFailedException, ResourceDoesNotExistException, IOException;
}
