package org.apache.maven.archiva.repository.consumer;

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
import org.apache.maven.archiva.repository.ArchivaRepository;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

/**
 * DiscovererConsumer 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Consumer
{
    public static final String ROLE = Consumer.class.getName();

    /**
     * This is the human readable name for the discoverer.
     * 
     * @return the human readable discoverer name.
     */
    public String getName();
    
    /**
     * This is used to initialize any internals in the consumer before it is used.
     * 
     * This method is called by the internals of archiva and is not meant to be used by other developers.
     * This method is called once per repository.
     * 
     * @param repository the repository to initialize the consumer against.
     * @return true if the repository is valid for this consumer. false will result in consumer being disabled 
     *      for the provided repository.
     */
    public boolean init( ArchivaRepository repository );
    
    /**
     * Get the List of excluded file patterns for this consumer.
     * 
     * @return the list of excluded file patterns for this consumer.
     */
    public List getExcludePatterns();
    
    /**
     * Get the List of included file patterns for this consumer.
     * 
     * @return the list of included file patterns for this consumer.
     */
    public List getIncludePatterns();

    /**
     * Called by archiva framework to indicate that there is a file suitable for consuming, 
     * This method will only be called if the {@link #init(ArtifactRepository)} and {@link #getExcludePatterns()}
     * and {@link #getIncludePatterns()} all pass for this consumer.
     * 
     * @param file the file to process.
     * @throws ConsumerException if there was a problem processing this file.
     */
    public void processFile( BaseFile file ) throws ConsumerException;
    
    /**
     * Called by archiva framework to indicate that there has been a problem detected
     * on a specific file.
     * 
     * NOTE: It is very possible for 1 file to have more than 1 problem associated with it.
     * 
     * @param file the file to process.
     * @param message the message describing the problem.
     */
    public void processFileProblem( BaseFile file, String message );
}
