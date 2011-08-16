package org.apache.archiva.common.plexusbridge;

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

import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.apache.maven.index.creator.OSGIArtifactIndexCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service("mavenIndexerUtils")
public class MavenIndexerUtils
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private List<? extends IndexCreator> allIndexCreators;

    @Inject
    public MavenIndexerUtils( PlexusSisuBridge plexusSisuBridge )
        throws PlexusSisuBridgeException
    {
        allIndexCreators = new ArrayList( plexusSisuBridge.lookupList( IndexCreator.class ) );

        if ( allIndexCreators == null || allIndexCreators.isEmpty() )
        {
            // olamy when the TCL is not a URLClassLoader lookupList fail !
            // when using tomcat maven plugin so adding a simple hack
            log.warn( "using lookList from sisu plexus failed so build indexCreator manually" );

            allIndexCreators =
                Arrays.asList( new OSGIArtifactIndexCreator(), new MavenArchetypeArtifactInfoIndexCreator(),
                               new MinimalArtifactInfoIndexCreator(), new JarFileContentsIndexCreator(),
                               new MavenPluginArtifactInfoIndexCreator() );

        }

        log.debug( "allIndexCreators {}", allIndexCreators );
    }

    public List<? extends IndexCreator> getAllIndexCreators()
    {
        return allIndexCreators;
    }

    public void setAllIndexCreators( List<IndexCreator> allIndexCreators )
    {
        this.allIndexCreators = allIndexCreators;
    }
}
