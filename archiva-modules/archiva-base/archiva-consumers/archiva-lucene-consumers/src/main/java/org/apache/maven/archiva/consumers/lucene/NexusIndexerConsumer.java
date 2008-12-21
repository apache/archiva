package org.apache.maven.archiva.consumers.lucene;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexingContext;

public class NexusIndexerConsumer 
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    private static final Logger log = LoggerFactory.getLogger(NexusIndexerConsumer.class);

    private final NexusIndexer indexer;

    private final ArtifactContextProducer artifactContextProducer;

    private ManagedRepositoryConfiguration repository;

    private ManagedDefaultRepositoryContent repositoryContent;

    private IndexingContext context;

    public NexusIndexerConsumer(NexusIndexer indexer)
    {
        this.indexer = indexer;
        this.artifactContextProducer = new DefaultArtifactContextProducer();
    }

    public String getDescription()
    {
        return "Indexes the repository to provide search and IDE integration features";
    }

    public String getId()
    {
        return "index-content";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan(ManagedRepositoryConfiguration repository, Date whenGathered)
        throws ConsumerException
    {
        this.repository = repository;
        File managedRepository = new File(repository.getLocation());
        File indexDirectory = new File(managedRepository, ".indexer");

        repositoryContent = new ManagedDefaultRepositoryContent();
        repositoryContent.setRepository(repository);

        synchronized (indexer)
        {
            try
            {
                context = indexer.addIndexingContextForced(repository.getId(), repository.getId(), managedRepository, indexDirectory, "repourl", "updateurl", NexusIndexer.FULL_INDEX);
                context.setSearchable(repository.isScanned());
            }
            catch (IOException e)
            {
                log.error("Could not create FSDirectory for index at " + indexDirectory.getAbsoluteFile(), e);
            }
        }
    }

    public void completeScan()
    {
        /** do nothing **/
    }

    public List<String> getExcludes()
    {
        return new ArrayList<String>();
    }

    public List<String> getIncludes()
    {
        return Arrays.asList("**/*");
    }

    public void processFile(String path) 
        throws ConsumerException
    {
        File artifactFile = new File(path);

        try
        {
            repositoryContent.toArtifactReference(path);
        }
        catch (LayoutException e)
        {
            /** do nothing **/
            return;
        }

        ArtifactContext artifactContext = artifactContextProducer.getArtifactContext(context, artifactFile);
        try
        {
            indexer.artifactDiscovered(artifactContext, context);
        }
        catch (IOException e)
        {
            throw new ConsumerException(e.getMessage(), e);
        }
    }
}
