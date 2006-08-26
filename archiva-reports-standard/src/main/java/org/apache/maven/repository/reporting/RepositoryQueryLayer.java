package org.apache.maven.repository.reporting;

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
import org.apache.maven.artifact.repository.metadata.Snapshot;

import java.util.List;

/**
 * The transitive and metadata validation reports will need to query the repository for artifacts.
 */
public interface RepositoryQueryLayer
{
    String ROLE = RepositoryQueryLayer.class.getName();

    boolean containsArtifact( Artifact artifact );

    /** @todo I believe we can remove this [BP] - artifact should contain all the necessary version info */
    boolean containsArtifact( Artifact artifact, Snapshot snapshot );

    List getVersions( Artifact artifact )
        throws RepositoryQueryLayerException;
}
