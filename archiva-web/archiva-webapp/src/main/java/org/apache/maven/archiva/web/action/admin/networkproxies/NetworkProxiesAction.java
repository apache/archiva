package org.apache.maven.archiva.web.action.admin.networkproxies;

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

import com.opensymphony.webwork.interceptor.ServletRequestAware;
import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import com.opensymphony.xwork.Validateable;

import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import javax.servlet.http.HttpServletRequest;

/**
 * NetworkProxiesAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="networkProxiesAction"
 */
public class NetworkProxiesAction
extends PlexusActionSupport
implements ModelDriven, Preparable, Validateable, SecureAction, ServletRequestAware
{

    public Object getModel()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void prepare()
        throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setServletRequest( HttpServletRequest request )
    {
        // TODO Auto-generated method stub
        
    }

}
