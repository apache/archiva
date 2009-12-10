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

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.util.ValueStack;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.struts2.StrutsException;
import org.apache.struts2.components.Component;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class DownloadArtifact
    extends Component
{
    private static final String DEFAULT_DOWNLOAD_IMAGE = "download-type-other.png";

    private RepositoryContentFactory repositoryFactory;

    private MetadataRepository metadataRepository;

    private HttpServletRequest req;

    private String groupId;

    private String artifactId;

    private String version;

    private boolean mini = false;

    private DecimalFormat decimalFormat;

    private static final Map<String, String> DOWNLOAD_IMAGES = new HashMap<String, String>();

    private UserRepositories userRepositories;

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
        decimalFormat = new DecimalFormat( "#,#00" );
        this.req = (HttpServletRequest) pageContext.getRequest();
        try
        {
            metadataRepository = (MetadataRepository) PlexusTagUtil.lookup( pageContext, MetadataRepository.class );
            repositoryFactory =
                (RepositoryContentFactory) PlexusTagUtil.lookup( pageContext, RepositoryContentFactory.class );
            userRepositories = (UserRepositories) PlexusTagUtil.lookup( pageContext, UserRepositories.class );
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
            List<ArtifactMetadata> artifacts = new ArrayList<ArtifactMetadata>();
            for ( String repoId : getObservableRepos() )
            {
                artifacts.addAll( metadataRepository.getArtifacts( repoId, groupId, artifactId, version ) );
            }

            if ( !artifacts.isEmpty() )
            {
                String prefix = req.getContextPath() + "/repository/";

                if ( mini )
                {
                    // TODO: write 1 line download link for main artifact.
                }
                else
                {
                    appendNormal( sb, prefix, artifacts );
                }
            }
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

    private void appendNormal( StringBuffer sb, String prefix, List<ArtifactMetadata> relatedArtifacts )
        throws RepositoryException
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
        for ( ArtifactMetadata artifact : relatedArtifacts )
        {
            String repoId = artifact.getRepositoryId();
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
                                  ArtifactMetadata artifact )
    {
        String path = getPath( repo, artifact );
        String type = getType( repo, path );
        String linkText = "<img src=\"" + req.getContextPath() + "/images/" + getDownloadImage( type ) + "\" />";
        appendLink( sb, prefix, artifact, linkText, path );
    }

    private String getType( ManagedRepositoryContent repo, String path )
    {
        String type = null;
        try
        {
            type = repo.toArtifactReference( path ).getType();
        }
        catch ( LayoutException e )
        {
            e.printStackTrace();  //TODO
        }
        return type;
    }

    private String getDownloadImage( String type )
    {
        String name = DOWNLOAD_IMAGES.get( type );
        return name != null ? name : DEFAULT_DOWNLOAD_IMAGE;
    }

    private static void appendLink( StringBuffer sb, String prefix, ArtifactMetadata artifact, String linkText,
                                    String path )
    {

        StringBuffer url = new StringBuffer();
        url.append( prefix );
        url.append( "/" ).append( path );

        sb.append( "<a href=\"" ).append( StringEscapeUtils.escapeXml( url.toString() ) ).append( "\"" );
        sb.append( " title=\"" ).append( "Download " ).append( StringEscapeUtils.escapeXml( artifact.getId() ) ).append(
            "\"" );
        sb.append( ">" );

        sb.append( linkText );

        sb.append( "</a>" );
    }

    private static String getPath( ManagedRepositoryContent repo, ArtifactMetadata artifact )
    {
        // TODO: use metadata resolver capability instead
        ArtifactReference ref = new ArtifactReference();
        ref.setArtifactId( artifact.getProject() );
        ref.setGroupId( artifact.getNamespace() );
        ref.setVersion( artifact.getVersion() );
        String path = repo.toPath( ref );
        path = path.substring( 0, path.lastIndexOf( "/" ) + 1 ) + artifact.getId();
        return path;
    }

    private void appendLink( StringBuffer sb, String prefix, ManagedRepositoryContent repo, ArtifactMetadata artifact )
    {
        String path = getPath( repo, artifact );
        String type = getType( repo, path );
        String linkText = StringUtils.capitalize( type );

        appendLink( sb, prefix, artifact, linkText, path );
    }

    private void appendFilesize( StringBuffer sb, ArtifactMetadata artifact )
    {
        sb.append( decimalFormat.format( artifact.getSize() ) );
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

    public Collection<String> getObservableRepos()
    {
        try
        {
            ActionContext context = ActionContext.getContext();
            Map session = context.getSession();
            return userRepositories.getObservableRepositoryIds( ArchivaXworkUser.getActivePrincipal( session ) );
        }
        catch ( ArchivaSecurityException e )
        {
            e.printStackTrace();  //TODO
            return Collections.emptyList();
        }
    }
}
