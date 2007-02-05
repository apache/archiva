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

import com.opensymphony.webwork.views.jsp.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * DownloadArtifactTag 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DownloadArtifactTag
    extends TagSupport
{
    private String groupId_; // stores EL-based groupId property

    private String groupId; // stores the evaluated groupId object.

    private String artifactId_; // stores EL-based artifactId property

    private String artifactId; // stores the evaluated artifactId object.

    private String version_; // stores EL-based version property

    private String version; // stores the evaluated version object.

    private String mini_; // stores EL-based mini property

    private boolean mini; // stores the evaluated mini object.

    public int doEndTag()
        throws JspException
    {
        evaluateExpressions();

        DownloadArtifact download = new DownloadArtifact( TagUtils.getStack( pageContext ), pageContext );
        download.setGroupId( groupId );
        download.setArtifactId( artifactId );
        download.setVersion( version );
        download.setMini( mini );

        download.end( pageContext.getOut(), "" );

        return super.doEndTag();
    }

    private void evaluateExpressions()
        throws JspException
    {
        ExpressionTool exprTool = new ExpressionTool( pageContext, this, "download" );

        // Handle required properties.
        groupId = exprTool.requiredString( "groupId", groupId_ );
        artifactId = exprTool.requiredString( "artifactId", artifactId_ );
        version = exprTool.requiredString( "version", version_ );

        // Handle optional properties
        mini = exprTool.optionalBoolean( "mini", mini_, false );
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId_ = artifactId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId_ = groupId;
    }

    public void setVersion( String version )
    {
        this.version_ = version;
    }
}
