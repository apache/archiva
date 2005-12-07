package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * 
 */
public class MockArtifactReportProcessor
    implements ArtifactReportProcessor
{
    private List reportConditions;

    private Iterator iterator;

    public MockArtifactReportProcessor()
    {
        reportConditions = new ArrayList();
    }

    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter, ArtifactRepository artifactRepository )
    {
        if ( iterator == null || !iterator.hasNext() ) // not initialized or reached end of the list. start again
        {
            iterator = reportConditions.iterator();
        }
        if ( !reportConditions.isEmpty() )
        {
            while(iterator.hasNext())
            {
                ReportCondition reportCondition = (ReportCondition) iterator.next();
                switch ( reportCondition.getResult() )
                {
                    case ReportCondition.SUCCESS:
                        {
                            reporter.addSuccess( reportCondition.getArtifact() );
                            break;
                        }
                    case ReportCondition.WARNING:
                        {
                            reporter.addWarning( reportCondition.getArtifact(), reportCondition.getReason() );
                            break;
                        }
                    case ReportCondition.FAILURE:
                        {
                            reporter.addFailure( reportCondition.getArtifact(), reportCondition.getReason() );
                            break;
                        }
                }
            }
        }
    }

    public void addReturnValue( int result, Artifact artifact, String reason )
    {
        reportConditions.add( new ReportCondition( result, artifact, reason ) );
    }

    public void clearList()
    {
        reportConditions.clear();
    }
}
