package org.apache.maven.archiva.reporting.database;

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

import org.apache.maven.archiva.reporting.model.ResultReason;

/**
 * AbstractResultsDatabase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractResultsDatabase
    extends AbstractJdoDatabase
{
    /**
     * <p>
     * Get the number of failures in the database.
     * </p>
     * 
     * <p>
     * <b>WARNING:</b> This is a very resource intensive request. Use sparingly.
     * </p>
     * 
     * @return the number of failures in the database.
     */
    public abstract int getNumFailures();

    /**
     * <p>
     * Get the number of warnings in the database.
     * </p>
     * 
     * <p>
     * <b>WARNING:</b> This is a very resource intensive request. Use sparingly.
     * </p>
     * 
     * @return the number of warnings in the database.
     */
    public abstract int getNumWarnings();
    
    /**
     * <p>
     * Get the number of notices in the database.
     * </p>
     * 
     * <p>
     * <b>WARNING:</b> This is a very resource intensive request. Use sparingly.
     * </p>
     * 
     * @return the number of notices in the database.
     */
    public abstract int getNumNotices();
    
    protected static ResultReason createResultReason( String processor, String problem, String reason )
    {
        ResultReason result = new ResultReason();
        result.setProcessor( processor );
        result.setProblem( problem );
        result.setReason( reason );
        return result;
    }
}
