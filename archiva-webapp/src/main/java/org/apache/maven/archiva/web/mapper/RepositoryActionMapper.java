package org.apache.maven.archiva.web.mapper;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.webwork.dispatcher.mapper.ActionMapping;
import com.opensymphony.webwork.dispatcher.mapper.DefaultActionMapper;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Map alternate URLs to specific actions. Used for the repository browser and the proxy.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RepositoryActionMapper
    extends DefaultActionMapper
{
    private static final String BROWSE_PREFIX = "/browse/";

    private static final String PROXY_PREFIX = "/proxy/";

    public String getUriFromActionMapping( ActionMapping actionMapping )
    {
        Map params = actionMapping.getParams();
        if ( "browseGroup".equals( actionMapping.getName() ) )
        {
            return BROWSE_PREFIX + params.remove( "groupId" );
        }
        else if ( "browseArtifact".equals( actionMapping.getName() ) )
        {
            return BROWSE_PREFIX + params.remove( "groupId" ) + "/" + params.remove( "artifactId" );
        }
        else if ( "showArtifact".equals( actionMapping.getName() ) )
        {
            return BROWSE_PREFIX + params.remove( "groupId" ) + "/" + params.remove( "artifactId" ) + "/" +
                params.remove( "version" );
        }
        else if ( "showArtifactDependencies".equals( actionMapping.getName() ) )
        {
            return BROWSE_PREFIX + params.remove( "groupId" ) + "/" + params.remove( "artifactId" ) + "/" +
                params.remove( "version" ) + "/dependencies";
        }
        else if ( "showArtifactDependees".equals( actionMapping.getName() ) )
        {
            return BROWSE_PREFIX + params.remove( "groupId" ) + "/" + params.remove( "artifactId" ) + "/" +
                params.remove( "version" ) + "/usedby";
        }
        else if ( "showArtifactDependencyTree".equals( actionMapping.getName() ) )
        {
            return BROWSE_PREFIX + params.remove( "groupId" ) + "/" + params.remove( "artifactId" ) + "/" +
                params.remove( "version" ) + "/dependencyTree";
        }
        else if ( "proxy".equals( actionMapping.getName() ) )
        {
            return PROXY_PREFIX + params.remove( "path" );
        }

        return super.getUriFromActionMapping( actionMapping );
    }

    public ActionMapping getMapping( HttpServletRequest httpServletRequest )
    {
        String path = httpServletRequest.getServletPath();
        if ( path.startsWith( BROWSE_PREFIX ) )
        {
            path = path.substring( BROWSE_PREFIX.length() );
            if ( path.length() == 0 )
            {
                return new ActionMapping( "browse", "/", "", null );
            }
            else
            {
                String[] parts = path.split( "/" );
                if ( parts.length == 1 )
                {
                    Map params = new HashMap();
                    params.put( "groupId", parts[0] );
                    return new ActionMapping( "browseGroup", "/", "", params );
                }
                else if ( parts.length == 2 )
                {
                    Map params = new HashMap();
                    params.put( "groupId", parts[0] );
                    params.put( "artifactId", parts[1] );
                    return new ActionMapping( "browseArtifact", "/", "", params );
                }
                else if ( parts.length == 3 )
                {
                    Map params = new HashMap();
                    params.put( "groupId", parts[0] );
                    params.put( "artifactId", parts[1] );
                    params.put( "version", parts[2] );
                    return new ActionMapping( "showArtifact", "/", "", params );
                }
                else if ( parts.length == 4 )
                {
                    Map params = new HashMap();
                    params.put( "groupId", parts[0] );
                    params.put( "artifactId", parts[1] );
                    params.put( "version", parts[2] );

                    if ( "dependencies".equals( parts[3] ) )
                    {
                        return new ActionMapping( "showArtifactDependencies", "/", "", params );
                    }
                    else if ( "usedby".equals( parts[3] ) )
                    {
                        return new ActionMapping( "showArtifactDependees", "/", "", params );
                    }
                    else if ( "dependencyTree".equals( parts[3] ) )
                    {
                        return new ActionMapping( "showArtifactDependencyTree", "/", "", params );
                    }
                }
            }
        }
        else if ( path.startsWith( PROXY_PREFIX ) )
        {
            // retain the leading /
            path = path.substring( PROXY_PREFIX.length() - 1 );

            Map params = new HashMap();
            params.put( "path", path );
            return new ActionMapping( "proxy", "/", "", params );
        }

        return super.getMapping( httpServletRequest );
    }
}
