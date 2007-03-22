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

import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

/**
 * ExpressionTool 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ExpressionTool
{
    private PageContext pageContext;

    private Tag tag;

    private String tagName;

    public ExpressionTool( PageContext pageContext, Tag tag, String tagName )
    {
        this.pageContext = pageContext;
        this.tag = tag;
        this.tagName = tagName;
    }

    public boolean optionalBoolean( String propertyName, String expression, boolean defaultValue )
        throws JspException
    {
        try
        {
            Boolean ret = (Boolean) ExpressionUtil.evalNotNull( this.tagName, propertyName, expression, Boolean.class,
                                                                this.tag, this.pageContext );

            if ( ret == null )
            {
                return defaultValue;
            }

            return ret.booleanValue();
        }
        catch ( NullAttributeException e )
        {
            return defaultValue;
        }
    }

    public String optionalString( String propertyName, String expression, String defaultValue )
        throws JspException
    {
        try
        {
            String ret = (String) ExpressionUtil.evalNotNull( this.tagName, propertyName, expression, String.class,
                                                              this.tag, this.pageContext );

            if ( ret == null )
            {
                return defaultValue;
            }

            return ret;
        }
        catch ( NullAttributeException e )
        {
            return defaultValue;
        }
    }

    public String requiredString( String propertyName, String expression )
        throws JspException
    {
        try
        {
            String ret = (String) ExpressionUtil.evalNotNull( this.tagName, propertyName, expression, String.class,
                                                              this.tag, this.pageContext );
            return ret;
        }
        catch ( NullAttributeException e )
        {
            String emsg = "Required " + this.tagName + " property [" + propertyName + "] is null!";

            log( emsg, e );
            throw new JspException( emsg );
        }
    }

    private void log( String msg, Throwable t )
    {
        pageContext.getServletContext().log( msg, t );
    }
}
