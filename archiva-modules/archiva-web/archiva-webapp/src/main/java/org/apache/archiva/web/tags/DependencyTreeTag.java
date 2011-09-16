package org.apache.archiva.web.tags;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.common.ArchivaException;
import org.apache.archiva.web.tags.DependencyTree.TreeEntry;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

/**
 * DependencyTreeTag - just here to output the dependency tree to the browser.
 * It was easier to do it this way, vs accessing the dependency graph via a JSP.
 * 
 * <pre>
 *   <archiva:dependency-tree groupId="org.apache.maven.archiva" 
 *                            artifactId="archiva-common" 
 *                            version="1.0"
 *                            nodevar="node">
 *     <b>${node.groupId}</b>:<b>${node.artifactId}</b>:<b>${node.version}</b> (${node.scope})
 *   </archiva:dependency-tree>
 * </pre>
 *
 * @version $Id$
 */
public class DependencyTreeTag
    extends TagSupport
    implements IterationTag, TryCatchFinally
{
    private String groupId;

    private String artifactId;

    @SuppressWarnings("unused")
    private String version;

    private String nodevar;

    private Iterator<TreeEntry> treeIterator;

    private List<TreeEntry> tree;

    private TreeEntry currentTreeEntry;

    private String modelVersion;

    public int doAfterBody()
        throws JspException
    {
        if ( currentTreeEntry != null )
        {
            out( currentTreeEntry.getPost() );
        }

        if ( treeIterator.hasNext() )
        {
            currentTreeEntry = treeIterator.next();
            out( currentTreeEntry.getPre() );
            exposeVariables();
            return EVAL_BODY_AGAIN;
        }

        out( "\n</div><!-- end of dependency-graph -->" );

        return SKIP_BODY;
    }

    public void doCatch( Throwable t )
        throws Throwable
    {
        throw t;
    }

    public void doFinally()
    {
        unExposeVariables();
    }

    @SuppressWarnings("unchecked")
    public int doStartTag()
        throws JspException
    {
        DependencyTree deptree;
        try
        {
            WebApplicationContext webApplicationContext =
                WebApplicationContextUtils.getRequiredWebApplicationContext( pageContext.getServletContext() );

            deptree = webApplicationContext.getBean( "dependencyTree", DependencyTree.class );
        }
        catch ( BeansException e )
        {
            throw new JspException( "Unable to lookup DependencyTree: " + e.getMessage(), e );
        }

        if ( deptree == null )
        {
            throw new JspException( "Unable to process dependency tree.  Component not found." );
        }

        if ( StringUtils.isBlank( nodevar ) )
        {
            nodevar = "node";
        }

        out( "<div class=\"dependency-graph\">" );
        try
        {
            this.tree = deptree.gatherTreeList( groupId, artifactId, modelVersion );
    
            if ( CollectionUtils.isEmpty( this.tree ) )
            {
                return SKIP_BODY;
            }
    
            treeIterator = tree.iterator();
    
            currentTreeEntry = treeIterator.next();
            out( currentTreeEntry.getPre() );
            exposeVariables();
        }
        catch ( ArchivaException e )
        {
            treeIterator = IteratorUtils.EMPTY_LIST_ITERATOR;
            
            out("<pre>");
            e.printStackTrace( new PrintWriter( pageContext.getOut() ) );
            out("</pre>");
        }

        return EVAL_BODY_INCLUDE;
    }

    public void release()
    {
        groupId = "";
        artifactId = "";
        version = "";
        nodevar = "";
        tree = null;
        treeIterator = null;
        super.release();
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setNodevar( String nodevar )
    {
        this.nodevar = nodevar;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public void setModelVersion( String modelVersion )
    {
        this.modelVersion = modelVersion;
    }

    private void exposeVariables()
        throws JspException
    {
        if ( currentTreeEntry == null )
        {
            pageContext.removeAttribute( nodevar, PageContext.PAGE_SCOPE );
        }
        else
        {
            pageContext.setAttribute( nodevar, currentTreeEntry.getArtifact() );
        }
    }

    private void out( String msg )
        throws JspException
    {
        try
        {
            pageContext.getOut().print( msg );
        }
        catch ( IOException e )
        {
            throw new JspException( "Unable to output to jsp page context." );
        }
    }

    private void unExposeVariables()
    {
        pageContext.removeAttribute( nodevar, PageContext.PAGE_SCOPE );
    }
}
