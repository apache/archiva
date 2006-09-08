package org.apache.maven.archiva.converter;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archiva.reporting.ReportingDatabase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

/**
 * Copy a set of artifacts from one repository to the other, converting if necessary.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface RepositoryConverter
{
    String ROLE = RepositoryConverter.class.getName();

    /**
     * Convert a single artifact, writing it into the target repository.
     *
     * @param artifact         the artifact to convert
     * @param targetRepository the target repository
     * @param reporter         reporter to track the results of the conversion
     */
    void convert( Artifact artifact, ArtifactRepository targetRepository, ReportingDatabase reporter )
        throws RepositoryConversionException;

    /**
     * Convert a set of artifacts, writing them into the target repository.
     *
     * @param artifacts        the set of artifacts to convert
     * @param targetRepository the target repository
     * @param reporter         reporter to track the results of the conversions
     */
    void convert( List artifacts, ArtifactRepository targetRepository, ReportingDatabase reporter )
        throws RepositoryConversionException;
}
