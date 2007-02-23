package org.apache.maven.archiva.converter;

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

import org.apache.maven.archiva.converter.legacy.LegacyRepositoryConverter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * ConversionEvent 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ConversionEvent
{
    public static final int STARTED = 0;

    public static final int PROCESSED = 1;

    public static final int WARNING = 2;

    public static final int ERROR = 3;

    public static final int FINISHED = 4;

    private int type;

    private String message;

    private Artifact artifact;

    private ArtifactRepository repository;

    private Exception exception;

    public ConversionEvent( ArtifactRepository repository, int type )
    {
        this.repository = repository;
        this.type = type;
    }
    
    public ConversionEvent( ArtifactRepository repository, int type, Artifact artifact )
    {
        this( repository, type );
        this.artifact = artifact;
    }
    
    public ConversionEvent( ArtifactRepository repository, int type, Artifact artifact, String message )
    {
        this( repository, type );
        this.artifact = artifact;
        this.message = message;
    }
    
    public ConversionEvent( ArtifactRepository repository, int type, Artifact artifact, Exception exception )
    {
        this( repository, type );
        this.artifact = artifact;
        this.exception = exception;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public Exception getException()
    {
        return exception;
    }

    public String getMessage()
    {
        return message;
    }

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    /**
     * <p>
     * The type of event.
     * </p>
     * 
     * <p>
     * Can be one of the following ...
     * </p>
     * 
     * <ul>
     * <li>{@link #STARTED} - the whole repository conversion process has started.
     *      only seen when using the whole repository conversion technique with the
     *      {@link LegacyRepositoryConverter#convertLegacyRepository(java.io.File, java.io.File, java.util.List, boolean)} 
     *      method.</li>
     * <li>{@link #PROCESSED} - a specific artifact has been processed.</li>
     * <li>{@link #WARNING} - a warning has been detected for a specific artifact during the conversion process.</li>
     * <li>{@link #ERROR} - an error in the processing of an artifact has been detected.</li>
     * <li>{@link #FINISHED} - the whole repository conversion process has finished.
     *      only seen when using the whole repository conversion technique with the
     *      {@link LegacyRepositoryConverter#convertLegacyRepository(java.io.File, java.io.File, java.util.List, boolean)} 
     *      method.</li>
     * </ul>
     * @return
     */
    public int getType()
    {
        return type;
    }
}
