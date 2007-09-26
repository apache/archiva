package org.apache.maven.archiva.consumers.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.codehaus.plexus.digest.ChecksumFile;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ArtifactMissingChecksumsConsumer - Create missing checksums for the artifact.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 * role-hint="create-missing-checksums"
 * instantiation-strategy="per-lookup"
 */
public class ArtifactMissingChecksumsConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener, Initializable
{
    /**
     * @plexus.configuration default-value="create-missing-checksums"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Create Missing Checksums (.sha1 & .md5)"
     */
    private String description;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout"
     */
    private Map bidirectionalLayoutMap; // TODO: replace with new bidir-repo-layout-factory

    /**
     * @plexus.requirement role-hint="sha1"
     */
    private Digester digestSha1;

    /**
     * @plexus.requirement role-hint="md5";
     */
    private Digester digestMd5;

    /**
     * @plexus.requirement
     */
    private ChecksumFile checksum;

    private static final String TYPE_CHECKSUM_NOT_FILE = "checksum-bad-not-file";

    private static final String TYPE_CHECKSUM_CANNOT_CALC = "checksum-calc-failure";

    private static final String TYPE_CHECKSUM_CANNOT_CREATE = "checksum-create-failure";

    private ArchivaRepository repository;

    private File repositoryDir;

    private BidirectionalRepositoryLayout layout;

    private List<String> propertyNameTriggers = new ArrayList<String>();

    private List<String> includes = new ArrayList<String>();

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan( ArchivaRepository repository )
        throws ConsumerException
    {
        this.repository = repository;
        this.repositoryDir = new File( repository.getUrl().getPath() );

        String layoutName = repository.getModel().getLayoutName();
        if ( !bidirectionalLayoutMap.containsKey( layoutName ) )
        {
            throw new ConsumerException( "Unable to process repository with layout [" + layoutName +
                "] as there is no corresponding " + BidirectionalRepositoryLayout.class.getName() +
                " implementation available." );
        }

        this.layout = (BidirectionalRepositoryLayout) bidirectionalLayoutMap.get( layoutName );
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        createIfMissing( path, digestSha1 );
        createIfMissing( path, digestMd5 );
    }

    private void createIfMissing( String path, Digester digester )
    {
        File checksumFile = new File( this.repositoryDir, path + digester.getFilenameExtension() );
        if ( !checksumFile.exists() )
        {
            try
            {
                checksum.createChecksum( new File( this.repositoryDir, path ), digester );
                triggerConsumerInfo( "Created missing checksum file " + checksumFile.getAbsolutePath() );
            }
            catch ( DigesterException e )
            {
                triggerConsumerError( TYPE_CHECKSUM_CANNOT_CALC,
                                      "Cannot calculate checksum for file " + checksumFile + ": " + e.getMessage() );
            }
            catch ( IOException e )
            {
                triggerConsumerError( TYPE_CHECKSUM_CANNOT_CREATE,
                                      "Cannot create checksum for file " + checksumFile + ": " + e.getMessage() );
            }
        }
        else if ( !checksumFile.isFile() )
        {
            triggerConsumerWarning( TYPE_CHECKSUM_NOT_FILE,
                                    "Checksum file " + checksumFile.getAbsolutePath() + " is not a file." );
        }

    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyNameTriggers.contains( propertyName ) )
        {
            initIncludes();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes.clear();

        includes.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
    }

    public void initialize()
        throws InitializationException
    {
        propertyNameTriggers = new ArrayList<String>();
        propertyNameTriggers.add( "repositoryScanning" );
        propertyNameTriggers.add( "fileTypes" );
        propertyNameTriggers.add( "fileType" );
        propertyNameTriggers.add( "patterns" );
        propertyNameTriggers.add( "pattern" );

        configuration.addChangeListener( this );

        initIncludes();
    }
}
