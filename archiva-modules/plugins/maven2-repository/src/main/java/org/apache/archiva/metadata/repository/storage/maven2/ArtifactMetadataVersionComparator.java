package org.apache.archiva.metadata.repository.storage.maven2;
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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.Comparator;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
public class ArtifactMetadataVersionComparator
    implements Comparator<ArtifactMetadata>
{
    public static ArtifactMetadataVersionComparator INSTANCE = new ArtifactMetadataVersionComparator();

    @Override
    public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
    {
        // sort by version (reverse), then ID
        int result =
            new DefaultArtifactVersion( o2.getVersion() ).compareTo( new DefaultArtifactVersion( o1.getVersion() ) );
        return result != 0 ? result : o1.getId().compareTo( o2.getId() );
    }
}
