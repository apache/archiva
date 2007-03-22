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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.ArchivaRepository;
import org.apache.maven.archiva.repository.RepositoryException;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * RepositoryScannerTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryScannerTest extends PlexusTestCase
{
    private ArchivaRepository createDefaultRepository()
    {
        File repoDir = new File( getBasedir(), "src/test/repositories/default-repository" );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        String repoUri = "file://" + StringUtils.replace( repoDir.getAbsolutePath(), "\\", "/" );

        ArchivaRepository repo = new ArchivaRepository( "testDefaultRepo", "Test Default Repository", repoUri );

        return repo;
    }

    public void testDefaultRepositoryScanner() throws RepositoryException
    {
        ArchivaRepository repository = createDefaultRepository();

        List consumers = new ArrayList();
        ScanConsumer consumer = new ScanConsumer();
        consumers.add( consumer );

        RepositoryScanner scanner = new RepositoryScanner();
        boolean includeSnapshots = true;
        RepositoryContentStatistics stats = scanner.scan( repository, consumers, includeSnapshots );

        assertNotNull( "Stats should not be null.", stats );
        assertEquals( "Stats.totalFileCount", 17, stats.getTotalFileCount() );
        assertEquals( "Processed Count", 17, consumer.getProcessCount() );
    }

}
