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

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Map alternate URLs to specific actions. Used for the repository browser and the proxy.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RepositoryActionMapper
    extends DefaultActionMapper
{
    private static final String ACTION_BROWSE = "browse";

    private static final String ACTION_BROWSE_ARTIFACT = "browseArtifact";

    private static final String ACTION_BROWSE_GROUP = "browseGroup";

    private static final String ACTION_SHOW_ARTIFACT = "showArtifact";

    private static final String ACTION_SHOW_ARTIFACT_DEPENDEES = "showArtifactDependees";

    private static final String ACTION_SHOW_ARTIFACT_DEPENDENCIES = "showArtifactDependencies";

    private static final String ACTION_SHOW_ARTIFACT_DEPENDENCY_TREE = "showArtifactDependencyTree";

    private static final String ACTION_SHOW_ARTIFACT_MAILING_LISTS = "showArtifactMailingLists";

    private static final String BROWSE_PREFIX = "/browse";

    private static final String METHOD_DEPENDENCIES = "dependencies";

    private static final String METHOD_DEPENDENCY_TREE = "dependencyTree";

    private static final String METHOD_MAILING_LISTS = "mailingLists";

    private static final String METHOD_USEDBY = "usedby";

    private static final String PARAM_ARTIFACT_ID = "artifactId";

    private static final String PARAM_GROUP_ID = "groupId";

    private static final String PARAM_VERSION = "version";

    public ActionMapping getMapping( HttpServletRequest httpServletRequest )
    {
        String path = httpServletRequest.getServletPath();

        if ("".equals(path)){
        	// if JEE 5 spec is correctly implemented, the "/*" pattern implies an empty string in servletpath
        	path = httpServletRequest.getPathInfo();
        }
        
        if ( path.startsWith( BROWSE_PREFIX ) )
        {
            path = path.substring( BROWSE_PREFIX.length() );
            if ( StringUtils.isBlank( path ) ||
                 StringUtils.equals( path, "/" ) || 
                 StringUtils.equals( path, ".action" ) )
            {
                // Return "root" browse.
                return new ActionMapping( ACTION_BROWSE, "/", "", null );
            }
            else
            {
                Map params = new HashMap();

                if ( path.charAt( 0 ) == '/' )
                {
                    path = path.substring( 1 );
                }

                String[] parts = path.split( "/" );
                switch ( parts.length )
                {
                    case 1:
                        params.put( PARAM_GROUP_ID, parts[0] );
                        return new ActionMapping( ACTION_BROWSE_GROUP, "/", "", params );

                    case 2:
                        params.put( PARAM_GROUP_ID, parts[0] );
                        params.put( PARAM_ARTIFACT_ID, parts[1] );
                        return new ActionMapping( ACTION_BROWSE_ARTIFACT, "/", "", params );

                    case 3:
                        params.put( PARAM_GROUP_ID, parts[0] );
                        params.put( PARAM_ARTIFACT_ID, parts[1] );
                        params.put( PARAM_VERSION, parts[2] );
                        return new ActionMapping( ACTION_SHOW_ARTIFACT, "/", "", params );

                    case 4:
                        params.put( PARAM_GROUP_ID, parts[0] );
                        params.put( PARAM_ARTIFACT_ID, parts[1] );
                        params.put( PARAM_VERSION, parts[2] );

                        if ( METHOD_DEPENDENCIES.equals( parts[3] ) )
                        {
                            return new ActionMapping( ACTION_SHOW_ARTIFACT_DEPENDENCIES, "/", "", params );
                        }
                        else if ( METHOD_MAILING_LISTS.equals( parts[3] ) )
                        {
                            return new ActionMapping( ACTION_SHOW_ARTIFACT_MAILING_LISTS, "/", "", params );
                        }
                        else if ( METHOD_USEDBY.equals( parts[3] ) )
                        {
                            return new ActionMapping( ACTION_SHOW_ARTIFACT_DEPENDEES, "/", "", params );
                        }
                        else if ( METHOD_DEPENDENCY_TREE.equals( parts[3] ) )
                        {
                            return new ActionMapping( ACTION_SHOW_ARTIFACT_DEPENDENCY_TREE, "/", "", params );
                        }
                        break;
                }
            }
        }

        return super.getMapping( httpServletRequest );
    }

    public String getUriFromActionMapping( ActionMapping actionMapping )
    {
        Map params = actionMapping.getParams();
        if ( ACTION_BROWSE.equals( actionMapping.getName() ) )
        {
            return BROWSE_PREFIX;
        }
        else if ( ACTION_BROWSE_GROUP.equals( actionMapping.getName() ) )
        {
            return toUri( params, false, false, null );
        }
        else if ( ACTION_BROWSE_ARTIFACT.equals( actionMapping.getName() ) )
        {
            return toUri( params, true, false, null );
        }
        else if ( ACTION_SHOW_ARTIFACT.equals( actionMapping.getName() ) )
        {
            return toUri( params, true, true, null );
        }
        else if ( ACTION_SHOW_ARTIFACT_DEPENDENCIES.equals( actionMapping.getName() ) )
        {
            return toUri( params, true, true, METHOD_DEPENDENCIES );
        }
        else if ( ACTION_SHOW_ARTIFACT_MAILING_LISTS.equals( actionMapping.getName() ) )
        {
            return toUri( params, true, true, METHOD_MAILING_LISTS );
        }
        else if ( ACTION_SHOW_ARTIFACT_DEPENDEES.equals( actionMapping.getName() ) )
        {
            return toUri( params, true, true, METHOD_USEDBY );
        }
        else if ( ACTION_SHOW_ARTIFACT_DEPENDENCY_TREE.equals( actionMapping.getName() ) )
        {
            return toUri( params, true, true, METHOD_DEPENDENCY_TREE );
        }

        return super.getUriFromActionMapping( actionMapping );
    }

    private String toUri( Map params, boolean artifactId, boolean version, String method )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( BROWSE_PREFIX );
        buf.append( '/' );
        buf.append( params.remove( PARAM_GROUP_ID ) );

        if ( artifactId )
        {
            buf.append( '/' );
            buf.append( params.remove( PARAM_ARTIFACT_ID ) );

            if ( version )
            {
                buf.append( '/' );
                buf.append( params.remove( PARAM_VERSION ) );

                if ( StringUtils.isNotBlank( method ) )
                {
                    buf.append( '/' );
                    buf.append( method );
                }
            }
        }

        return buf.toString();
    }
}
