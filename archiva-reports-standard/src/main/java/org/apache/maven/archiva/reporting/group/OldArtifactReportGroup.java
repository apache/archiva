package org.apache.maven.archiva.reporting.group;

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

import org.apache.maven.archiva.reporting.group.AbstractReportGroup;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The report set for finding old artifacts (both snapshot and release)
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.group.ReportGroup" role-hint="old-artifact"
 */
public class OldArtifactReportGroup
    extends AbstractReportGroup
{
    /**
     * Role hints of the reports to include in this set.
     *
     * @todo implement these report processors!
     */
    private static final Map reports = new LinkedHashMap();

    static
    {
        reports.put( "old-artifact", "Old Artifacts" );
        reports.put( "old-snapshot-artifact", "Old Snapshot Artifacts" );
    }

    public boolean includeReport( String key )
    {
        return reports.containsKey( key );
    }

    public Map getReports()
    {
        return reports;
    }

    public String getFilename()
    {
        return "old-artifacts-report.xml";
    }

    public String getName()
    {
        return "Old Artifacts";
    }
}
