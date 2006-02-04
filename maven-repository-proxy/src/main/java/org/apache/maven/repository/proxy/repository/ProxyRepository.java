package org.apache.maven.repository.proxy.repository;

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

import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

/**
 * Class to represent the Proxy repository.  Currently does not provide additional methods from
 * DefaultArtifactRepository but is expected to do so like enabled/disabled when a UI is present.
 *
 * @author Edwin Punzalan
 */
public class ProxyRepository
    extends DefaultArtifactRepository
{
    public ProxyRepository( String id, String url, ArtifactRepositoryLayout layout )
    {
        super( id, url, layout );
    }
}
