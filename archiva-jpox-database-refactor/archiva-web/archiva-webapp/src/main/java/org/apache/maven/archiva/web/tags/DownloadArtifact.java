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

import com.opensymphony.webwork.WebWorkException;
import com.opensymphony.webwork.components.Component;
import com.opensymphony.xwork.util.OgnlValueStack;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.artifact.managed.ManagedArtifact;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.repositories.ActiveManagedRepositories;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * DownloadArtifact 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.webwork.components.Component" role-hint="download-artifact" 
 *  instantiation-strategy="per-lookup"
 */
public class DownloadArtifact
    extends Component
{
    /**
     * @plexus.requirement 
     */
    private ActiveManagedRepositories managedRepositories;

    private HttpServletRequest req;

    private HttpServletResponse res;

    private String groupId;

    private String artifactId;

    private String version;

    private boolean mini = false;

    public DownloadArtifact( OgnlValueStack stack, PageContext pageContext )
    {
        super( stack );
        this.req = (HttpServletRequest) pageContext.getRequest();
        this.res = (HttpServletResponse) pageContext.getResponse();
        try
        {
            managedRepositories = (ActiveManagedRepositories) PlexusTagUtil.lookup( pageContext,
                                                                                    ActiveManagedRepositories.ROLE );
        }
        catch ( ComponentLookupException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public boolean end( Writer writer, String body )
    {
        StringBuffer sb = new StringBuffer();

        try
        {
            ManagedArtifact managedArtifact = managedRepositories.findArtifact( groupId, artifactId, version );

            if ( managedArtifact != null )
            {
                RepositoryConfiguration repoConfig = managedRepositories.getRepositoryConfiguration( managedArtifact
                    .getRepositoryId() );
                String prefix = req.getContextPath() + "/repository/" + repoConfig.getUrlName();

                if ( mini )
                {
                    appendMini( sb, prefix, managedArtifact );
                }
                else
                {
                    appendNormal( sb, prefix, managedArtifact );
                }
            }
        }
        catch ( ProjectBuildingException e )
        {
            appendError( sb, e );
        }

        try
        {
            writer.write( sb.toString() );
        }
        catch ( IOException e )
        {
            throw new WebWorkException( "IOError: " + e.getMessage(), e );
        }

        return super.end( writer, body );
    }

    private void appendError( StringBuffer sb, ProjectBuildingException e )
    {
        /* do nothing */
    }

    private void appendMini( StringBuffer sb, String prefix, ManagedArtifact managedArtifact )
    {
        /* do nothing */
    }

    private void appendNormal( StringBuffer sb, String prefix, ManagedArtifact managedArtifact )
    {
        /*
         * <div class="download">
         *   <div class="hd"> 
         *     <div class="c"></div>
         *   </div>
         *   <div class="bd">
         *     <div class="c">
         *       <-- main content goes here -->
         *     </div>
         *   </div>
         *   <div class="ft">
         *     <div class="c"></div>
         *   </div>
         * </div>
         */

        sb.append( "<div class=\"download\">" );
        sb.append( "<div class=\"hd\"><div class=\"c\"></div></div>" );
        sb.append( "<div class=\"bd\"><div class=\"c\">" );

        // Heading
        sb.append( "<h2>" );
        if ( managedArtifact.getAttached().isEmpty() )
        {
            sb.append( "Download" );
        }
        else
        {
            sb.append( "Downloads" );
        }
        sb.append( "</h2>" );

        // Body
        sb.append( "<p class=\"body\">" );

        appendLink( sb, prefix, managedArtifact.getPath(), "main" );

        Iterator it = managedArtifact.getAttached().entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String type = (String) entry.getKey();
            String path = (String) entry.getValue();

            if ( StringUtils.isNotBlank( path ) )
            {
                sb.append( "<br/>" );
                appendLink( sb, prefix, path, type );
            }
        }

        sb.append( "</div>" ); // close "downloadbox.bd.c"
        sb.append( "</div>" ); // close "downloadbox.bd"

        sb.append( "<div class=\"ft\"><div class=\"c\"></div></div>" );
        sb.append( "</div>" ); // close "download"
    }

    private void appendLink( StringBuffer sb, String prefix, String path, String type )
    {
        StringBuffer url = new StringBuffer();

        url.append( prefix );
        url.append( "/" ).append( path );

        String filename = path.substring( path.lastIndexOf( "/" ) + 1 );

        sb.append( "<a href=\"" ).append( StringEscapeUtils.escapeXml( url.toString() ) ).append( "\"" );
        sb.append( " title=\"" ).append( "Download " ).append( StringEscapeUtils.escapeXml( filename ) ).append( "\"" );
        sb.append( ">" );

        sb.append( "<img src=\"" ).append( req.getContextPath() );
        sb.append( "/images/download-type-" ).append( type ).append( ".png\" />" );

        // TODO: Include file size / date in output ?
        sb.append( StringUtils.capitalize( type ) );
        sb.append( "</a>" );
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setMini( boolean mini )
    {
        this.mini = mini;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }
}
