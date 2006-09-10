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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The report set for finding old artifacts (both snapshot and release)
 *
 * @plexus.component role="org.apache.maven.archiva.reporting.ReportGroup" role-hint="old-artifact"
 */
public class OldArtifactReportGroup
    extends AbstractReportGroup
{
    /**
     * Role hints of the reports to include in this set.
     *
     * @todo implement these report processors!
     */
    private static final Set reports =
        new LinkedHashSet( Arrays.asList( new String[]{"old-artifact", "old-snapshot-artifact"} ) );

    public boolean includeReport( String key )
    {
        return reports.contains( key );
    }

    public Collection getReportIds()
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
