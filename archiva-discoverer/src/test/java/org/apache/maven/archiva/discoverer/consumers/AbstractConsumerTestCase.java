package org.apache.maven.archiva.discoverer.consumers;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.discoverer.AbstractDiscovererTestCase;
import org.apache.maven.archiva.discoverer.Discoverer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;

/**
 * AbstractConsumerTestCase 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractConsumerTestCase
    extends AbstractDiscovererTestCase
{
    protected ArtifactFactory artifactFactory;

    protected Discoverer discoverer;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        discoverer = (Discoverer) lookup( Discoverer.class.getName(), "default" );
    }

    protected void tearDown()
        throws Exception
    {
        if ( discoverer != null )
        {
            release( discoverer );
        }

        if ( artifactFactory != null )
        {
            release( artifactFactory );
        }
        super.tearDown();
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type, String classifier )
    {
        if ( StringUtils.isNotBlank( classifier ) )
        {
            return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
        }
        else
        {
            return artifactFactory.createArtifact( groupId, artifactId, version, "runtime", type );
        }
    }
}
