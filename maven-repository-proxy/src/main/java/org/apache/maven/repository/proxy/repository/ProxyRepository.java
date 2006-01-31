package org.apache.maven.repository.proxy.repository;

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.repository.proxy.files.Checksum;

import java.security.NoSuchAlgorithmException;

/**
 * @author Edwin Punzalan
 */
public class ProxyRepository
    extends DefaultArtifactRepository
{
    private Checksum checksum;

    public ProxyRepository( String id, String url, ArtifactRepositoryLayout layout )
    {
        super( id, url, layout );
    }

    public void setChecksum( String algorithm )
    {
        this.checksum = new Checksum( algorithm );
    }

    public Checksum getChecksum()
    {
        return checksum;
    }

    public ChecksumObserver getChecksumObserver()
        throws NoSuchAlgorithmException
    {
        return new ChecksumObserver( checksum.getAlgorithm() );
    }
}
