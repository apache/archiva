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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
    {
        if ( iterator == null || !iterator.hasNext() ) // not initialized or reached end of the list. start again
        {
            iterator = reportConditions.iterator();
        }
        if ( !reportConditions.isEmpty() )
        {
            while ( iterator.hasNext() )
            {
                ReportCondition reportCondition = (ReportCondition) iterator.next();
                int i = reportCondition.getResult();
                if ( i == ReportCondition.SUCCESS )
                {
                    reporter.addSuccess( reportCondition.getArtifact() );
                }
                else if ( i == ReportCondition.WARNING )
                {
                    reporter.addWarning( reportCondition.getArtifact(), reportCondition.getReason() );
                }
                else if ( i == ReportCondition.FAILURE )
                {
                    reporter.addFailure( reportCondition.getArtifact(), reportCondition.getReason() );
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
