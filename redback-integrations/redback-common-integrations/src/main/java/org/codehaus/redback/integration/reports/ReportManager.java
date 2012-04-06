package org.codehaus.redback.integration.reports;

/*
 * Copyright 2005-2006 The Codehaus.
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

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReportManager
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service( "reportManager" )
public class ReportManager
{

    @Inject
    private List<Report> availableReports;

    @Inject
    private ApplicationContext applicationContext;

    private Map<String, Map<String, Report>> reportMap;

    public Report findReport( String id, String type )
        throws ReportException
    {
        if ( StringUtils.isBlank( id ) )
        {
            throw new ReportException( "Unable to generate report from empty report id." );
        }

        if ( StringUtils.isBlank( type ) )
        {
            throw new ReportException( "Unable to generate report from empty report type." );
        }

        Map<String, Report> typeMap = reportMap.get( id );
        if ( typeMap == null )
        {
            throw new ReportException( "Unable to find report id [" + id + "]" );
        }

        Report requestedReport = typeMap.get( type );

        if ( requestedReport == null )
        {
            throw new ReportException( "Unable to find report id [" + id + "] type [" + type + "]" );
        }

        return requestedReport;
    }

    public Map<String, Map<String, Report>> getReportMap()
    {
        return Collections.unmodifiableMap( reportMap );
    }

    @SuppressWarnings( "unchecked" )
    @PostConstruct
    public void initialize()
    {
        reportMap = new HashMap<String, Map<String, Report>>();

        for ( Report report : availableReports )
        {
            Map<String, Report> typeMap = reportMap.get( report.getId() );
            if ( typeMap == null )
            {
                typeMap = new HashMap<String, Report>();
            }

            typeMap.put( report.getType(), report );
            reportMap.put( report.getId(), typeMap );
        }
    }
}
