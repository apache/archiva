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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * GroupIdLink 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GroupIdLinkTag
    extends TagSupport
{
    private String var_; // stores EL-based property

    private String var; // stores the evaluated object.

    private boolean includeTop = false;

    public void release()
    {
        var_ = null;
        var = null;
        includeTop = false;

        super.release();
    }

    public int doEndTag()
        throws JspException
    {
        evaluateExpressions();

        GroupIdLink gidlink = new GroupIdLink( TagUtils.getStack( pageContext ), (HttpServletRequest) pageContext
            .getRequest(), (HttpServletResponse) pageContext.getResponse() );

        gidlink.setGroupId( var );
        gidlink.setIncludeTop( includeTop );

        gidlink.end( pageContext.getOut(), "" );

        return super.doEndTag();
    }

    private void evaluateExpressions()
        throws JspException
    {
        ExpressionTool exprTool = new ExpressionTool( pageContext, this, "groupIdLink" );
        
        var = exprTool.optionalString( "var", var_, "" );
    }

    public void setVar( String value )
    {
        this.var_ = value;
    }

    public void setIncludeTop( boolean includeTop )
    {
        this.includeTop = includeTop;
    }
}
