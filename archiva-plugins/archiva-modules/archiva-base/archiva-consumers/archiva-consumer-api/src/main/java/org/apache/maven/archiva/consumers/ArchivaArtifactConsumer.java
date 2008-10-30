package org.apache.maven.archiva.consumers;

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

import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.List;

/**
 * ArchivaArtifactConsumer - consumer for ArchivaArtifact objects. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ArchivaArtifactConsumer extends Consumer
{
    /**
     * Get the list of included file patterns for this consumer.
     * 
     * @return the list of ({@link String}) artifact types to process.
     */
    public List<String> getIncludedTypes();

    /**
     * <p>
     * Event that triggers at the beginning of a scan.
     * </p>
     * 
     * <p>
     * NOTE: This would be a good place to initialize the consumer, to lock any resources, and to
     * generally start tracking the scan as a whole.
     * </p>
     */
    public void beginScan();

    /**
     * <p>
     * Event indicating an {@link ArchivaArtifact} is to be processed by this consumer.
     * </p> 
     * 
     * <p>
     * NOTE: The consumer does not need to process the artifact immediately, can can opt to queue and/or track
     * the artifact to be processed in batch.  Just be sure to complete the processing by the {@link #completeScan()} 
     * event. 
     * </p>
     * 
     * @param file the file to process.
     * @throws ConsumerException if there was a problem processing this file.
     */
    public void processArchivaArtifact( ArchivaArtifact artifact ) throws ConsumerException;

    /**
     * <p>
     * Event that triggers on the completion of a scan.
     * </p>
     * 
     * <p>
     * NOTE: If the consumer opted to batch up processing requests in the 
     * {@link #processArchivaArtifact(ArchivaArtifact)} event this would be the last opportunity to drain 
     * any processing queue's.
     * </p>
     */
    public void completeScan();
}
