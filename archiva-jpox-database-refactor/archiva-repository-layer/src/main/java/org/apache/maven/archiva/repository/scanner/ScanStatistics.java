package org.apache.maven.archiva.repository.scanner;

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

import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * ScanStatistics 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ScanStatistics
{
    private static final String PROP_FILES_CONSUMED = "scan.consumed.files";

    private static final String PROP_FILES_INCLUDED = "scan.included.files";

    private static final String PROP_FILES_SKIPPED = "scan.skipped.files";

    private static final String PROP_TIMESTAMP_STARTED = "scan.started.timestamp";

    private static final String PROP_TIMESTAMP_FINISHED = "scan.finished.timestamp";

    protected long timestampStarted = 0;

    protected long timestampFinished = 0;

    protected long filesIncluded = 0;

    protected long filesConsumed = 0;

    protected long filesSkipped = 0;

    private ArtifactRepository repository;

    public ScanStatistics( ArtifactRepository repository )
    {
        this.repository = repository;
    }

    public void load( String filename )
        throws IOException
    {
        File repositoryBase = new File( this.repository.getBasedir() );

        File scanProperties = new File( repositoryBase, filename );
        FileInputStream fis = null;
        try
        {
            Properties props = new Properties();
            fis = new FileInputStream( scanProperties );
            props.load( fis );

            timestampFinished = NumberUtils.toLong( props.getProperty( PROP_TIMESTAMP_FINISHED ), 0 );
            timestampStarted = NumberUtils.toLong( props.getProperty( PROP_TIMESTAMP_STARTED ), 0 );
            filesIncluded = NumberUtils.toLong( props.getProperty( PROP_FILES_INCLUDED ), 0 );
            filesConsumed = NumberUtils.toLong( props.getProperty( PROP_FILES_CONSUMED ), 0 );
            filesSkipped = NumberUtils.toLong( props.getProperty( PROP_FILES_SKIPPED ), 0 );
        }
        catch ( IOException e )
        {
            reset();
            throw e;
        }
        finally
        {
            IOUtil.close( fis );
        }
    }

    public void save( String filename )
        throws IOException
    {
        Properties props = new Properties();
        props.setProperty( PROP_TIMESTAMP_FINISHED, String.valueOf( timestampFinished ) );
        props.setProperty( PROP_TIMESTAMP_STARTED, String.valueOf( timestampStarted ) );
        props.setProperty( PROP_FILES_INCLUDED, String.valueOf( filesIncluded ) );
        props.setProperty( PROP_FILES_CONSUMED, String.valueOf( filesConsumed ) );
        props.setProperty( PROP_FILES_SKIPPED, String.valueOf( filesSkipped ) );

        File repositoryBase = new File( this.repository.getBasedir() );
        File statsFile = new File( repositoryBase, filename );

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( statsFile );
            props.store( fos, "Last Scan Information, managed by Archiva. DO NOT EDIT" );
            fos.flush();
        }
        finally
        {
            IOUtil.close( fos );
        }
    }

    public void reset()
    {
        timestampStarted = 0;
        timestampFinished = 0;
        filesIncluded = 0;
        filesConsumed = 0;
        filesSkipped = 0;
    }

    public long getElapsedMilliseconds()
    {
        return timestampFinished - timestampStarted;
    }

    public long getFilesConsumed()
    {
        return filesConsumed;
    }

    public long getFilesIncluded()
    {
        return filesIncluded;
    }

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    public long getTimestampFinished()
    {
        return timestampFinished;
    }

    public long getTimestampStarted()
    {
        return timestampStarted;
    }

    public long getFilesSkipped()
    {
        return filesSkipped;
    }

    public void setTimestampFinished( long timestampFinished )
    {
        this.timestampFinished = timestampFinished;
    }

    public void setTimestampStarted( long timestampStarted )
    {
        this.timestampStarted = timestampStarted;
    }

    public void dump( Logger logger )
    {
        logger.info( "----------------------------------------------------" );
        logger.info( "Scan of Repository: " + repository.getId() );
        logger.info( "   Started : " + toHumanTimestamp( this.getTimestampStarted() ) );
        logger.info( "   Finished: " + toHumanTimestamp( this.getTimestampFinished() ) );
        // TODO: pretty print ellapsed time.
        logger.info( "   Duration: " + this.getElapsedMilliseconds() + "ms" );
        logger.info( "   Files   : " + this.getFilesIncluded() );
        logger.info( "   Consumed: " + this.getFilesConsumed() );
        logger.info( "   Skipped : " + this.getFilesSkipped() );
    }
    
    private String toHumanTimestamp( long timestamp )
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        return dateFormat.format( new Date( timestamp ) );
    }
}
