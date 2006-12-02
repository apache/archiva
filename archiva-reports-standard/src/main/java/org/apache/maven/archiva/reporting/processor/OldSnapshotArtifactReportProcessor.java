package org.apache.maven.archiva.reporting.processor;

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
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Find snapshot artifacts in the repository that are considered old.
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor" role-hint="old-snapshot-artifact"
 * @todo make this configurable from the web interface
 */
public class OldSnapshotArtifactReportProcessor
    implements ArtifactReportProcessor
{
    private static final String ROLE_HINT = "old-snapshot-artifact";

    /**
     * The maximum age of an artifact before it is reported old, specified in seconds. The default is 1 year.
     *
     * @plexus.configuration default-value="31536000"
     */
    private int maxAge;

    /**
     * The maximum number of snapshots to retain within a given version. The default is 0, which keeps all snapshots
     * that are within the age limits.
     *
     * @plexus.configuration default-value="0"
     */
    private int maxSnapshots;

    public void processArtifact( final Artifact artifact, Model model, ReportingDatabase reporter )
    {
        ArtifactRepository repository = artifact.getRepository();

        if ( !"file".equals( repository.getProtocol() ) )
        {
            // We can't check other types of URLs yet. Need to use Wagon, with an exists() method.
            throw new UnsupportedOperationException(
                "Can't process repository '" + repository.getUrl() + "'. Only file based repositories are supported" );
        }

        adjustDistributionArtifactHandler( artifact );

        String artifactPath = repository.pathOf( artifact );

        //get the location of the artifact itself
        File file = new File( repository.getBasedir(), artifactPath );

        if ( file.exists() )
        {
            if ( artifact.isSnapshot() )
            {
                Matcher m = Artifact.VERSION_FILE_PATTERN.matcher( artifact.getVersion() );
                if ( m.matches() )
                {
                    long timestamp;
                    try
                    {
                        timestamp = new SimpleDateFormat( "yyyyMMdd.HHmmss" ).parse( m.group( 2 ) ).getTime();
                    }
                    catch ( ParseException e )
                    {
                        throw new IllegalStateException(
                            "Shouldn't match timestamp pattern and not be able to parse it: " + m.group( 2 ) );
                    }

                    if ( System.currentTimeMillis() - timestamp > maxAge * 1000 )
                    {
                        addNotice( reporter, artifact, "snapshot-expired-time",
                                   "The artifact is older than the maximum age of " + maxAge + " seconds." );
                    }
                    else if ( maxSnapshots > 0 )
                    {
                        File[] files = file.getParentFile().listFiles( new FilenameFilter()
                        {
                            public boolean accept( File file, String string )
                            {
                                return string.startsWith( artifact.getArtifactId() + "-" ) &&
                                    string.endsWith( "." + artifact.getArtifactHandler().getExtension() );
                            }
                        } );

                        List/*<Integer>*/ buildNumbers = new ArrayList();
                        Integer currentBuild = null;
                        for ( Iterator i = Arrays.asList( files ).iterator(); i.hasNext(); )
                        {
                            File f = (File) i.next();

                            // trim to version
                            int startIndex = artifact.getArtifactId().length() + 1;
                            int extensionLength = artifact.getArtifactHandler().getExtension().length() + 1;
                            int endIndex = f.getName().length() - extensionLength;
                            String name = f.getName().substring( startIndex, endIndex );

                            Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( name );

                            if ( matcher.matches() )
                            {
                                Integer buildNumber = Integer.valueOf( matcher.group( 3 ) );

                                buildNumbers.add( buildNumber );
                                if ( name.equals( artifact.getVersion() ) )
                                {
                                    currentBuild = buildNumber;
                                }
                            }
                        }

                        // Prune back to expired build numbers
                        Collections.sort( buildNumbers );
                        for ( int i = 0; i < maxSnapshots && !buildNumbers.isEmpty(); i++ )
                        {
                            buildNumbers.remove( buildNumbers.size() - 1 );
                        }

                        if ( buildNumbers.contains( currentBuild ) )
                        {
                            addNotice( reporter, artifact, "snapshot-expired-count",
                                       "The artifact is older than the maximum number of retained snapshot builds." );
                        }
                    }
                }
            }
        }
        else
        {
            throw new IllegalStateException( "Couldn't find artifact " + file );
        }
    }

    private static void addNotice( ReportingDatabase reporter, Artifact artifact, String problem, String reason )
    {
        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
        reporter.addNotice( artifact, ROLE_HINT, problem, reason );
    }

    private static void adjustDistributionArtifactHandler( Artifact artifact )
    {
        // need to tweak these as they aren't currently in the known type converters. TODO - add them in Maven
        if ( "distribution-zip".equals( artifact.getType() ) )
        {
            artifact.setArtifactHandler( new DefaultArtifactHandler( "zip" ) );
        }
        else if ( "distribution-tgz".equals( artifact.getType() ) )
        {
            artifact.setArtifactHandler( new DefaultArtifactHandler( "tar.gz" ) );
        }
    }
}
