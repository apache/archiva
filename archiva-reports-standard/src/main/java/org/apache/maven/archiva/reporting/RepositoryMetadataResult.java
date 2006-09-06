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

import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

/**
 * A result of the report for a given artifact being processed.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id:RepositoryMetadataResult.java 437105 2006-08-26 17:22:22 +1000 (Sat, 26 Aug 2006) brett $
 */
public class RepositoryMetadataResult
    implements Result
{
    private final RepositoryMetadata metadata;

    private final String reason;

    public RepositoryMetadataResult( RepositoryMetadata metadata )
    {
        this.metadata = metadata;
        this.reason = null;
    }

    public RepositoryMetadataResult( RepositoryMetadata metadata, String reason )
    {
        this.metadata = metadata;
        this.reason = reason;
    }

    public RepositoryMetadata getMetadata()
    {
        return metadata;
    }

    public String getReason()
    {
        return reason;
    }
}
