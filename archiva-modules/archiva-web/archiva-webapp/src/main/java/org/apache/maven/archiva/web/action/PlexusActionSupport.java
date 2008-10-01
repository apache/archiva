package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork2.ActionSupport;
import java.util.Map;
import org.apache.struts2.interceptor.SessionAware;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * LogEnabled and SessionAware ActionSupport
 */
public abstract class PlexusActionSupport
    extends ActionSupport
    implements LogEnabled, SessionAware
{
    protected Map session;

    private Logger logger;

    public void setSession( Map map )
    {
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.session = map;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

    protected Logger getLogger()
    {
        return logger;
    }
}
