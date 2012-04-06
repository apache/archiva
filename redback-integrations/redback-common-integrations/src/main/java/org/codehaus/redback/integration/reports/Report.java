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

import java.io.OutputStream;

/**
 * Report
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Report
{
    /**
     * The Name of the Report (for display to the user)
     *
     * @return the name of the report.
     */
    String getName();

    /**
     * The type of report (example: 'csv', 'xls', 'pdf')
     * Used in the display of the report links to the user.
     *
     * @return the type of report.
     */
    String getType();

    /**
     * The mimetype of the report. (used to set download content type correctly)
     *
     * @return the mimetype.
     */
    String getMimeType();

    /**
     * The ID for this report.
     *
     * @return the ID for this report.
     */
    String getId();

    /**
     * Write Report to provided outputstream.
     *
     * @param os the outputstream to write to.
     * @throws ReportException if there was a problem in generating the report.
     */
    void writeReport( OutputStream os )
        throws ReportException;
}
