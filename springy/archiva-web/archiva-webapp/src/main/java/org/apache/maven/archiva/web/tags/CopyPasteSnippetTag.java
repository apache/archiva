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

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * CopyPasteSnippetTag 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class CopyPasteSnippetTag
    extends TagSupport
{
    private Object object;
    
    private String wrapper = CopyPasteSnippet.PRE;

    public void release()
    {
        object = null;
        super.release();
    }

    public int doEndTag()
        throws JspException
    {
        CopyPasteSnippet snippet;
        try
        {
            snippet = (CopyPasteSnippet) PlexusTagUtil.lookup( pageContext, CopyPasteSnippet.class.getName() );
        }
        catch ( ComponentLookupException e )
        {
            throw new JspException( "Unable to lookup CopyPasteSnippet: " + e.getMessage(), e );
        }

        if ( snippet == null )
        {
            throw new JspException( "Unable to process snippet.  Component not found." );
        }

        snippet.write( wrapper, object, pageContext );

        return super.doEndTag();
    }

    public void setObject( Object object )
    {
        this.object = object;
    }

    public void setWrapper( String wrapper )
    {
        this.wrapper = wrapper;
    }
}
