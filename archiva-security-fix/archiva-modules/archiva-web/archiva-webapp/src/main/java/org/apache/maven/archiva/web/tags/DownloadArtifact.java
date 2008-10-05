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
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArtifactsRelatedConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * DownloadArtifact
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="com.opensymphony.webwork.components.Component" role-hint="download-artifact"
 * instantiation-strategy="per-lookup"
 */
public class DownloadArtifact
    extends Component
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;
    
    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    private HttpServletRequest req;

    @SuppressWarnings("unused")
    private HttpServletResponse res;

    private String groupId;

    private String artifactId;

    private String version;

    private boolean mini = false;

    private DecimalFormat decimalFormat;

    public DownloadArtifact( OgnlValueStack stack, PageContext pageContext )
    {
        super( stack );
        decimalFormat = new DecimalFormat( "#,#00" );
        this.req = (HttpServletRequest) pageContext.getRequest();
        this.res = (HttpServletResponse) pageContext.getResponse();
        try
        {
            dao = (ArchivaDAO) PlexusTagUtil.lookup( pageContext, ArchivaDAO.ROLE, "jdo" );
            repositoryFactory = (RepositoryContentFactory) PlexusTagUtil.lookup( pageContext,
                                                                                 RepositoryContentFactory.class );
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
            Constraint constraint = new ArtifactsRelatedConstraint( groupId, artifactId, version );
            List<ArchivaArtifact> relatedArtifacts = dao.getArtifactDAO().queryArtifacts( constraint );

            if ( relatedArtifacts != null )
            {
                String repoId = ( (ArchivaArtifact) relatedArtifacts.get( 0 ) ).getModel().getRepositoryId();
                ManagedRepositoryContent repo = repositoryFactory.getManagedRepositoryContent( repoId );

                String prefix = req.getContextPath() + "/repository/" + repoId;

                if ( mini )
                {
                    appendMini( sb, prefix, repo, relatedArtifacts );
                }
                else
                {
                    appendNormal( sb, prefix, repo, relatedArtifacts );
                }
            }
        }
        catch ( ObjectNotFoundException e )
        {
            appendError( sb, e );
        }
        catch ( ArchivaDatabaseException e )
        {
            appendError( sb, e );
        }
        catch ( RepositoryNotFoundException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

    private void appendError( StringBuffer sb, Exception e )
    {
        /* do nothing */
    }

    private void appendMini( StringBuffer sb, String prefix, ManagedRepositoryContent repo,
                             List<ArchivaArtifact> relatedArtifacts )
    {
        // TODO: write 1 line download link for main artifact.
    }

    private void appendNormal( StringBuffer sb, String prefix, ManagedRepositoryContent repo,
                               List<ArchivaArtifact> relatedArtifacts )
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
        if ( relatedArtifacts.size() > 1 )
        {
            sb.append( "Downloads" );
        }
        else
        {
            sb.append( "Download" );
        }
        sb.append( "</h2>" );

        // Body
        sb.append( "<p class=\"body\">" );

        sb.append( "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">" );
        for ( ArchivaArtifact artifact : relatedArtifacts )
        {
            sb.append( "\n<tr>" );

            sb.append( "<td class=\"icon\">" );
            appendImageLink( sb, prefix, repo, artifact );
            sb.append( "</td>" );

            sb.append( "<td class=\"type\">" );
            appendLink( sb, prefix, repo, artifact );
            sb.append( "</td>" );

            sb.append( "<td class=\"size\">" );
            appendFilesize( sb, artifact );
            sb.append( "</td>" );

            sb.append( "</tr>" );
        }
        sb.append( "</table>" );
        sb.append( "</p>" );

        sb.append( "</div>" ); // close "downloadbox.bd.c"
        sb.append( "</div>" ); // close "downloadbox.bd"

        sb.append( "<div class=\"ft\"><div class=\"c\"></div></div>" );
        sb.append( "</div>" ); // close "download"
    }

    private void appendImageLink( StringBuffer sb, String prefix, ManagedRepositoryContent repo,
                                  ArchivaArtifact artifact )
    {
        String type = artifact.getType();
        String linkText = "<img src=\"" + req.getContextPath() + "/images/download-type-" + type + ".png\" />";
        appendLink( sb, prefix, repo, artifact, linkText );
    }

    private static void appendLink( StringBuffer sb, String prefix, ManagedRepositoryContent repo,
                                    ArchivaArtifact artifact, String linkText )
    {
        StringBuffer url = new StringBuffer();
        
        String path = repo.toPath( artifact );

        url.append( prefix );
        url.append( "/" ).append( path );

        String filename = path.substring( path.lastIndexOf( "/" ) + 1 );

        sb.append( "<a href=\"" ).append( StringEscapeUtils.escapeXml( url.toString() ) ).append( "\"" );
        sb.append( " title=\"" ).append( "Download " ).append( StringEscapeUtils.escapeXml( filename ) ).append( "\"" );
        sb.append( ">" );

        sb.append( linkText );

        sb.append( "</a>" );
    }

    private void appendLink( StringBuffer sb, String prefix, ManagedRepositoryContent repo,
                             ArchivaArtifact artifact )
    {
        String type = artifact.getType();
        String linkText = StringUtils.capitalize( type );

        appendLink( sb, prefix, repo, artifact, linkText );
    }

    private void appendFilesize( StringBuffer sb, ArchivaArtifact artifact )
    {
        sb.append( decimalFormat.format( artifact.getModel().getSize() ) );
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
