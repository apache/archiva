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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.web.util.ContextUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;

/**
 * CopyPasteSnippet
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.web.tags.CopyPasteSnippet"
 */
public class CopyPasteSnippet
    extends AbstractLogEnabled
{
    public void write( Object o, PageContext pageContext )
        throws JspException
    {
        StringBuffer buf = new StringBuffer();

        if ( o == null )
        {
            buf.append( "Error generating snippet." );
            getLogger().error( "Unable to generate snippet for null object." );
        }
        else if ( o instanceof ManagedRepositoryConfiguration )
        {
            createSnippet( buf, (ManagedRepositoryConfiguration) o, pageContext );
        }
        else
        {
            buf.append( "Unable to generate snippet for object " ).append( o.getClass().getName() );
        }

        try
        {
            JspWriter out = pageContext.getOut();
            out.write( StringEscapeUtils.escapeXml( buf.toString() ) );
            out.flush();
        }
        catch ( IOException e )
        {
            throw new JspException( "Unable to write snippet to output: " + e.getMessage(), e );
        }
    }

    private void createSnippet( StringBuffer snippet, ManagedRepositoryConfiguration repo, PageContext pageContext )
    {
        snippet.append( "<project>\n" );
        snippet.append( "  ...\n" );
        snippet.append( "  <distributionManagement>\n" );

        String distRepoName = "repository";
        if ( repo.isSnapshots() )
        {
            distRepoName = "snapshotRepository";
        }

        snippet.append( "    <" ).append( distRepoName ).append( ">\n" );
        snippet.append( "      <id>" ).append( repo.getId() ).append( "</id>\n" );

        if ( !"default".equals( repo.getLayout() ) )
        {
            snippet.append( "      <layout>" ).append( repo.getLayout() ).append( "</layout>" );
        }

        snippet.append( "    </" ).append( distRepoName ).append( ">\n" );
        snippet.append( "  </distributionManagement>\n" );
        snippet.append( "\n" );

        snippet.append( "  <repositories>\n" );
        snippet.append( "    <repository>\n" );
        snippet.append( "      <id>" ).append( repo.getId() ).append( "</id>\n" );
        snippet.append( "      <name>" ).append( repo.getName() ).append( "</name>\n" );

        snippet.append( "      <url>" );
        snippet.append( ContextUtils.getBaseURL( pageContext, "repository" ) );
        snippet.append( "/" ).append( repo.getId() ).append( "/" );

        snippet.append( "</url>\n" );

        if ( !"default".equals( repo.getLayout() ) )
        {
            snippet.append( "      <layout>" ).append( repo.getLayout() ).append( "</layout>\n" );
        }

        snippet.append( "      <releases>\n" );
        snippet.append( "        <enabled>" ).append( Boolean.valueOf( repo.isReleases() ) ).append( "</enabled>\n" );
        snippet.append( "      </releases>\n" );
        snippet.append( "      <snapshots>\n" );
        snippet.append( "        <enabled>" ).append( Boolean.valueOf( repo.isSnapshots() ) ).append( "</enabled>\n" );
        snippet.append( "      </snapshots>\n" );
        snippet.append( "    </repository>\n" );
        snippet.append( "  </repositories>\n" );

        snippet.append( "  ...\n" );
        snippet.append( "</project>\n" );
    }
}
