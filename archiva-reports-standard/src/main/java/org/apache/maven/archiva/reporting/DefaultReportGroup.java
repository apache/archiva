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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The default report set, for repository health.
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.ReportGroup" role-hint="health"
 * @todo could these report groups be assembled dynamically by configuration rather than as explicit components? eg, reportGroup.addReport( ARP ), reportGroup.addReport( MRP )
 */
public class DefaultReportGroup
    extends AbstractReportGroup
{
    /**
     * Role hints of the reports to include in this set.
     */
    private static final Map reports = new LinkedHashMap();

    static
    {
        reports.put( "checksum", "Checksum Problems" );
        reports.put( "dependency", "Dependency Problems" );
        // TODO re-enable duplicate, once a way to populate the index is determined!
//        reports.put( "duplicate", "Duplicate Artifact Problems" );
        reports.put( "invalid-pom", "POM Problems" );
        reports.put( "bad-metadata", "Metadata Problems" );
        reports.put( "checksum-metadata", "Metadata Checksum Problems" );
        reports.put( "artifact-location", "Artifact Location Problems" );
    }

    public boolean includeReport( String key )
    {
        return reports.containsKey( key );
    }

    public Map getReports()
    {
        return reports;
    }

    public String getName()
    {
        return "Repository Health";
    }

    public String getFilename()
    {
        return "health-report.xml";
    }
}
