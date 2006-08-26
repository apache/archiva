package org.apache.maven.archiva.reporting;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;

/**
 * This interface will be called by the main system for each artifact as it is discovered. This is how each of the
 * different types of reports are implemented.
 */
public interface ArtifactReportProcessor
{
    String ROLE = ArtifactReportProcessor.class.getName();

    void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter, ArtifactRepository repository )
        throws ReportProcessorException;

}
