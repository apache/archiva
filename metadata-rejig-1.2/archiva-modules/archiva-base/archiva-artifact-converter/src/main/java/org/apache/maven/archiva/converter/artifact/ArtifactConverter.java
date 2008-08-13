package org.apache.maven.archiva.converter.artifact;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Map;

/**
 * ArtifactConverter 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ArtifactConverter
{
    public static final String ROLE = ArtifactConverter.class.getName();
    
    /**
     * Convert an provided artifact, and place it into the destination repository.
     * 
     * @param artifact the artifact to convert.
     * @param destinationRepository the respository to send the artifact to.
     * @throws ArtifactConversionException 
     */
    void convert( Artifact artifact, ArtifactRepository destinationRepository )
        throws ArtifactConversionException;

    /**
     * Get the map of accumulated warnings for the conversion.
     * 
     * @return the {@link Map}&lt;{@link Artifact}, {@link String}&gt; warning messages.
     */
    Map getWarnings();

    /**
     * Clear the list of warning messages.
     */
    void clearWarnings();
}
