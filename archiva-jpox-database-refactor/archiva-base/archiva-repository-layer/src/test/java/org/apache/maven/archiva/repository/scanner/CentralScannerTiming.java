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

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.common.utils.DateUtil;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.ArchivaRepository;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.consumer.Consumer;
import org.apache.maven.archiva.repository.consumer.ConsumerException;

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

        RepositoryScanner scanner = new RepositoryScanner();

        List consumerList = new ArrayList();
        BasicConsumer consumer = new BasicConsumer();
        consumerList.add( consumer );

        try
        {
            RepositoryContentStatistics stats = scanner.scan( centralRepo, consumerList, true );

            SimpleDateFormat df = new SimpleDateFormat();
            System.out.println( "-------" );
            System.out.println( "  Repository ID   : " + stats.getRepositoryId() );
            System.out.println( "  Duration        : " + DateUtil.getDuration( stats.getDuration() ) );
            System.out.println( "  When Gathered   : " + df.format( stats.getWhenGathered() ) );
            System.out.println( "  Total File Count: " + stats.getTotalFileCount() );
            System.out.println( "  New File Count  : " + stats.getNewFileCount() );
        }
        catch ( RepositoryException e )
        {
            e.printStackTrace( System.err );
        }
    }

    class BasicConsumer implements Consumer
    {
        int count = 0;

        public List getExcludePatterns()
        {
            return Collections.EMPTY_LIST;
        }

        public List getIncludePatterns()
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

        public String getName()
        {
            return "Basic No-op Consumer";
        }

        public boolean init( ArchivaRepository repository )
        {
            return true;
        }

        public void processFile( BaseFile file ) throws ConsumerException
        {
            count++;
            if ( ( count % 1000 ) == 0 )
            {
                System.out.println( "Files Processed: " + count );
            }
        }

        public void processFileProblem( BaseFile file, String message )
        {
            /* no-op */
        }
    }
}
