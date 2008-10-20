package org.apache.maven.archiva.web.tags;

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

import org.apache.struts2.StrutsException;
import org.apache.struts2.components.Component;

import com.opensymphony.xwork2.util.ValueStack;
import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * GroupIdLink 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GroupIdLink
    extends Component
{
    private static final String ACTION = "browseGroup";

    private static final String NAMESPACE = "/";

    private static final boolean includeContext = true;

    private static final boolean encode = true;

    private static final String method = null;

    private HttpServletRequest req;

    private HttpServletResponse res;

    private String groupId;

    private boolean includeTop = false;

    public GroupIdLink( ValueStack stack, HttpServletRequest req, HttpServletResponse res )
    {
        super( stack );
        this.req = req;
        this.res = res;
    }

    @Override
    public boolean end( Writer writer, String body )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "<span class=\"groupId\">" );

        if ( includeTop )
        {
            sb.append( "<a href=\"" );
            sb.append( determineBrowseActionUrl() );
            sb.append( "\">[top]</a> / " ); // TODO: i18n
        }

        StringTokenizer tok = new StringTokenizer( groupId, "." );
        String cumulativeGroup = null;

        while ( tok.hasMoreTokens() )
        {
            String token = tok.nextToken();

            if ( cumulativeGroup == null )
            {
                cumulativeGroup = token;
            }
            else
            {
                cumulativeGroup += "." + token;
            }
            sb.append( "<a href=\"" );
            sb.append( determineBrowseGroupActionUrl( cumulativeGroup ) );
            sb.append( "\">" ).append( token ).append( "</a> / " );
        }
        
        sb.append( "</span>" );

        try
        {
            writer.write( sb.toString() );
        }
        catch ( IOException e )
        {
            throw new StrutsException( "IOError: " + e.getMessage(), e );
        }

        return super.end( writer, body );
    }

    private String determineBrowseActionUrl()
    {
        return determineActionURL( "browse", NAMESPACE, method, req, res, parameters, req.getScheme(), includeContext, encode, false, false );
    }

    private String determineBrowseGroupActionUrl( String gid )
    {
        parameters.put( "groupId", gid );

        return determineActionURL( ACTION, NAMESPACE, method, req, res, parameters, req.getScheme(), includeContext, encode, false, false );
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public boolean isIncludeTop()
    {
        return includeTop;
    }

    public void setIncludeTop( boolean includeTop )
    {
        this.includeTop = includeTop;
    }

}
