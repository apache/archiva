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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * DownloadArtifact
 *
 * @version $Id$
 * @plexus.component role="org.apache.struts2.components.Component" role-hint="download-artifact"
 * instantiation-strategy="per-lookup"
 */
public class DownloadArtifact
    extends Component
{
    private static final String DEFAULT_DOWNLOAD_IMAGE = "download-type-other.png";
    
    private static final double KILO_BYTE = 1024.0;
    private static final double MEGA_BYTE = 1048576.0;
    private static final double GIGA_BYTE = 1073741824.0;

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

    private static final Map<String, String> DOWNLOAD_IMAGES = new HashMap<String,String>();

    static
    {
        DOWNLOAD_IMAGES.put( "jar", "download-type-jar.png" );
        DOWNLOAD_IMAGES.put( "java-source", "download-type-jar.png" );
        DOWNLOAD_IMAGES.put( "pom", "download-type-pom.png" );
        DOWNLOAD_IMAGES.put( "maven-plugin", "download-type-maven-plugin.png" );
        DOWNLOAD_IMAGES.put( "maven-archetype", "download-type-archetype.png" );
        DOWNLOAD_IMAGES.put( "maven-skin", "download-type-skin.png" );
    }
    
    public DownloadArtifact( ValueStack stack, PageContext pageContext )
    {
        super( stack );
        decimalFormat = new DecimalFormat( "#,##0.00" );
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

    @Override
    public boolean end( Writer writer, String body )
    {
        StringBuffer sb = new StringBuffer();

        try
        {
            Constraint constraint = new ArtifactsRelatedConstraint( groupId, artifactId, version );
            List<ArchivaArtifact> relatedArtifacts = dao.getArtifactDAO().queryArtifacts( constraint );

            if ( relatedArtifacts != null && relatedArtifacts.size() > 0 )
            {
                String prefix = req.getContextPath() + "/repository/";

                if ( mini )
                {
                    appendMini( sb, prefix, relatedArtifacts );
                }
                else
                {
                    appendNormal( sb, prefix, relatedArtifacts );
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
            throw new StrutsException( "IOError: " + e.getMessage(), e );
        }

        return super.end( writer, body );
    }

    private void appendError( StringBuffer sb, Exception e )
    {
        /* do nothing */
    }

    private void appendMini( StringBuffer sb, String prefix, List<ArchivaArtifact> relatedArtifacts )
    {
        // TODO: write 1 line download link for main artifact.
    }

    private void appendNormal( StringBuffer sb, String prefix, List<ArchivaArtifact> relatedArtifacts )
        throws RepositoryNotFoundException, RepositoryException
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
            String repoId = artifact.getModel().getRepositoryId();
            ManagedRepositoryContent repo = repositoryFactory.getManagedRepositoryContent( repoId );

            sb.append( "\n<tr>" );

            sb.append( "<td class=\"icon\">" );
            appendImageLink( sb, prefix + repoId, repo, artifact );
            sb.append( "</td>" );

            sb.append( "<td class=\"type\">" );
            appendLink( sb, prefix + repoId, repo, artifact );
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
        String linkText = "<img src=\"" + req.getContextPath() + "/images/" + getDownloadImage( type ) + "\" />";
        appendLink( sb, prefix, repo, artifact, linkText );
    }

    private String getDownloadImage( String type )
    {
        String name = DOWNLOAD_IMAGES.get( type );
        return name != null ? name : DEFAULT_DOWNLOAD_IMAGE;
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
        
        if( artifact.getModel().getClassifier() != null && !artifact.getModel().getClassifier().trim().equals( "" ) ) 
        {
            linkText = new StringBuilder(linkText).append(" (").append(artifact.getModel().getClassifier()).append(")").toString();
        }

        appendLink( sb, prefix, repo, artifact, linkText );
    }

    private void appendFilesize( StringBuffer sb, ArchivaArtifact artifact )
    {
        long size = artifact.getModel().getSize();
        if( size > GIGA_BYTE )
        {
            sb.append( decimalFormat.format( artifact.getModel().getSize() / GIGA_BYTE ) ).append(" GB");        
        }
        else if( size > MEGA_BYTE )
        {
            sb.append( decimalFormat.format( artifact.getModel().getSize() / MEGA_BYTE ) ).append(" MB");        
        }
        else if( size > KILO_BYTE )
        {
            sb.append( decimalFormat.format( artifact.getModel().getSize() / KILO_BYTE ) ).append(" KB");        
        }
        else
        {
            sb.append( decimalFormat.format( artifact.getModel().getSize() ) ).append(" B");        
        }
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
