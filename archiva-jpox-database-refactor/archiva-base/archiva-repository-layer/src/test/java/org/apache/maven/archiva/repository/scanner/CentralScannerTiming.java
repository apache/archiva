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

import org.apache.maven.archiva.common.utils.DateUtil;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.project.ProjectModel400Reader;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CentralScannerTiming 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CentralScannerTiming
{
    public static void main( String[] args )
    {
        String pathToCentral = "/home/repo1/ibiblio";

        ( new CentralScannerTiming() ).scanIt( pathToCentral );
    }

    public void scanIt( String path )
    {
        ArchivaRepository centralRepo = new ArchivaRepository( "central", "Central Mirror", "file://" + path );

        List consumerList = new ArrayList();

        // Basic - find the artifacts (no real processing)

        consumerList.add( new BasicConsumer() );
        timeIt( "Basic Scan", centralRepo, consumerList );

        // POM - find the poms and read them.

        consumerList.clear();
        consumerList.add( new POMConsumer() );
        timeIt( "POM Read", centralRepo, consumerList );
    }

    private void timeIt( String type, ArchivaRepository repo, List consumerList )
    {
        RepositoryScanner scanner = new RepositoryScanner();

        try
        {
            RepositoryContentStatistics stats = scanner.scan( repo, consumerList, true );

            SimpleDateFormat df = new SimpleDateFormat();
            System.out.println( ".\\ " + type + " \\.__________________________________________" );
            System.out.println( "  Repository ID   : " + stats.getRepositoryId() );
            System.out.println( "  Duration        : " + DateUtil.getDuration( stats.getDuration() ) );
            System.out.println( "  When Gathered   : " + df.format( stats.getWhenGathered() ) );
            System.out.println( "  Total File Count: " + stats.getTotalFileCount() );
            System.out.println( "  New File Count  : " + stats.getNewFileCount() );
            System.out.println( "______________________________________________________________" );
        }
        catch ( RepositoryException e )
        {
            e.printStackTrace( System.err );
        }
    }

    class POMConsumer extends AbstractMonitoredConsumer implements RepositoryContentConsumer
    {
        private int count = 0;

        private ProjectModelReader reader;

        private ArchivaRepository repo;

        public POMConsumer()
        {
            reader = new ProjectModel400Reader();
        }

        public List getExcludes()
        {
            return Collections.EMPTY_LIST;
        }

        public List getIncludes()
        {
            List includes = new ArrayList();
            includes.add( "**/*.pom" );
            return includes;
        }

        public String getId()
        {
            return "pom-consumer";
        }

        public String getDescription()
        {
            return "Basic POM Consumer";
        }

        public boolean isPermanent()
        {
            return false;
        }

        public void beginScan( ArchivaRepository repository ) throws ConsumerException
        {
            repo = repository;
        }

        public void processFile( String path ) throws ConsumerException
        {
            count++;
            if ( ( count % 1000 ) == 0 )
            {
                System.out.println( "Files Processed: " + count );
            }

            File pomFile = new File( repo.getUrl().getPath(), path );
            try
            {
                ArchivaProjectModel model = reader.read( pomFile );
            }
            catch ( ProjectModelException e )
            {
                System.err.println( "Unable to process: " + pomFile );
                e.printStackTrace( System.out );
            }
        }

        public void completeScan()
        {
            /* do nothing */
        }
    }

    class BasicConsumer extends AbstractMonitoredConsumer implements RepositoryContentConsumer
    {
        int count = 0;

        public List getExcludes()
        {
            return Collections.EMPTY_LIST;
        }

        public List getIncludes()
        {
            List includes = new ArrayList();
            includes.add( "**/*.pom" );
            includes.add( "**/*.jar" );
            includes.add( "**/*.war" );
            includes.add( "**/*.ear" );
            includes.add( "**/*.sar" );
            includes.add( "**/*.car" );
            includes.add( "**/*.mar" );
            //            includes.add( "**/*.sha1" );
            //            includes.add( "**/*.md5" );
            //            includes.add( "**/*.asc" );
            includes.add( "**/*.dtd" );
            includes.add( "**/*.tld" );
            includes.add( "**/*.gz" );
            includes.add( "**/*.bz2" );
            includes.add( "**/*.zip" );
            return includes;
        }

        public String getId()
        {
            return "test-scan-timing";
        }

        public String getDescription()
        {
            return "Basic No-op Consumer";
        }

        public boolean isPermanent()
        {
            return false;
        }

        public void beginScan( ArchivaRepository repository ) throws ConsumerException
        {
            /* do nothing */
        }

        public void processFile( String path ) throws ConsumerException
        {
            count++;
            if ( ( count % 1000 ) == 0 )
            {
                System.out.println( "Files Processed: " + count );
            }
        }

        public void completeScan()
        {
            /* do nothing */
        }
    }
}
