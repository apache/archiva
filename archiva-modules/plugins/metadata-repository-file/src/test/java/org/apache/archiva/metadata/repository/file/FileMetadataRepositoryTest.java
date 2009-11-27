package org.apache.archiva.metadata.repository.file;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

public class FileMetadataRepositoryTest
    extends PlexusInSpringTestCase
{
    private FileMetadataRepository repository;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_PROJECT = "projectId";

    private static final String TEST_NAMESPACE = "namespace";

    public void setUp()
        throws Exception
    {
        super.setUp();

        repository = (FileMetadataRepository) lookup( MetadataRepository.class );
        File directory = getTestFile( "target/test-repository" );
        FileUtils.deleteDirectory( directory );
        repository.setDirectory( directory );
    }

    public void testRootNamespaceWithNoMetadataRepository()
    {
        Collection<String> namespaces = repository.getRootNamespaces( TEST_REPO_ID );
        assertEquals( Collections.<String>emptyList(), namespaces );
    }

    public void testUpdateProjectVersionMetadataWithNoOtherArchives()
    {
        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setId( TEST_PROJECT );
        MailingList mailingList = new MailingList();
        mailingList.setName( "Foo List" );
        mailingList.setOtherArchives( Collections.<String>emptyList() );
        metadata.setMailingLists( Collections.singletonList( mailingList ) );
        repository.updateProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, metadata );
    }
}
