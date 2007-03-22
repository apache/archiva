package org.apache.maven.archiva.common.artifact.builder;

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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * DefaultLayoutArtifactBuilder - artifact builder for default layout repositories. 
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.common.artifact.builder.LayoutArtifactBuilder"
 *     role-hint="default"
 */
public class DefaultLayoutArtifactBuilder
    extends AbstractLayoutArtifactBuilder
    implements LayoutArtifactBuilder
{
    public DefaultLayoutArtifactBuilder()
    {
        super();
    }

    public DefaultLayoutArtifactBuilder( ArtifactFactory artifactFactory )
    {
        super( artifactFactory );
    }

    public Artifact build( String pathToArtifact )
        throws BuilderException
    {
        if( artifactFactory == null )
        {
            throw new IllegalStateException( "Unable to build artifact with a null artifactFactory." );
        }
        
        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( pathToArtifact, "/\\" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );

        Artifact artifact;
        if ( pathParts.size() >= 4 )
        {
            // maven 2.x path

            // the actual artifact filename.
            String filename = (String) pathParts.remove( 0 );

            // the next one is the version.
            String version = (String) pathParts.remove( 0 );

            // the next one is the artifactId.
            String artifactId = (String) pathParts.remove( 0 );

            // the remaining are the groupId.
            Collections.reverse( pathParts );
            String groupId = StringUtils.join( pathParts.iterator(), "." );

            String remainingFilename = filename;
            if ( remainingFilename.startsWith( artifactId + "-" ) )
            {
                remainingFilename = remainingFilename.substring( artifactId.length() + 1 );

                String classifier = null;

                // TODO: use artifact handler, share with legacy discoverer
                String type;
                if ( remainingFilename.endsWith( ".tar.gz" ) )
                {
                    type = "distribution-tgz";
                    remainingFilename = remainingFilename
                        .substring( 0, remainingFilename.length() - ".tar.gz".length() );
                }
                else if ( remainingFilename.endsWith( ".zip" ) )
                {
                    type = "distribution-zip";
                    remainingFilename = remainingFilename.substring( 0, remainingFilename.length() - ".zip".length() );
                }
                else if ( remainingFilename.endsWith( "-test-sources.jar" ) )
                {
                    type = "java-source";
                    classifier = "test-sources";
                    remainingFilename = remainingFilename.substring( 0, remainingFilename.length()
                        - "-test-sources.jar".length() );
                }
                else if ( remainingFilename.endsWith( "-sources.jar" ) )
                {
                    type = "java-source";
                    classifier = "sources";
                    remainingFilename = remainingFilename.substring( 0, remainingFilename.length()
                        - "-sources.jar".length() );
                }
                else
                {
                    int index = remainingFilename.lastIndexOf( "." );
                    if ( index >= 0 )
                    {
                        type = remainingFilename.substring( index + 1 );
                        remainingFilename = remainingFilename.substring( 0, index );
                    }
                    else
                    {
                        throw new BuilderException( "Path filename does not have an extension." );
                    }
                }

                Artifact result;
                if ( classifier == null )
                {
                    result = artifactFactory
                        .createArtifact( groupId, artifactId, version, Artifact.SCOPE_RUNTIME, type );
                }
                else
                {
                    result = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                           classifier );
                }

                if ( result.isSnapshot() )
                {
                    // version is *-SNAPSHOT, filename is *-yyyyMMdd.hhmmss-b
                    int classifierIndex = remainingFilename.indexOf( '-', version.length() + 8 );
                    if ( classifierIndex >= 0 )
                    {
                        classifier = remainingFilename.substring( classifierIndex + 1 );
                        remainingFilename = remainingFilename.substring( 0, classifierIndex );
                        result = artifactFactory.createArtifactWithClassifier( groupId, artifactId, remainingFilename,
                                                                               type, classifier );
                    }
                    else
                    {
                        result = artifactFactory.createArtifact( groupId, artifactId, remainingFilename,
                                                                 Artifact.SCOPE_RUNTIME, type );
                    }

                    // poor encapsulation requires we do this to populate base version
                    if ( !result.isSnapshot() )
                    {
                        throw new BuilderException( "Failed to create a snapshot artifact: " + result );
                    }
                    else if ( !result.getBaseVersion().equals( version ) )
                    {
                        throw new BuilderException(
                                                    "Built snapshot artifact base version does not match path version: "
                                                        + result.getBaseVersion() + "; should have been version: "
                                                        + version );
                    }
                    else
                    {
                        artifact = result;
                    }
                }
                else if ( !remainingFilename.startsWith( version ) )
                {
                    throw new BuilderException( "Built artifact version does not match path version" );
                }
                else if ( !remainingFilename.equals( version ) )
                {
                    if ( remainingFilename.charAt( version.length() ) == '-' )
                    {
                        classifier = remainingFilename.substring( version.length() + 1 );
                        artifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                                 classifier );
                    }
                    else
                    {
                        throw new BuilderException( "Path version does not corresspond to an artifact version" );
                    }
                }
                else
                {
                    artifact = result;
                }
            }
            else
            {
                throw new BuilderException( "Path filename does not correspond to an artifact." );
            }
        }
        else
        {
            throw new BuilderException( "Path is too short to build an artifact from." );
        }

        return artifact;
    }
}
