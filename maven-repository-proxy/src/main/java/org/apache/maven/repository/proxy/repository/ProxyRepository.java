package org.apache.maven.repository.proxy.repository;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.proxy.files.Checksum;
import org.apache.maven.wagon.observers.ChecksumObserver;

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
