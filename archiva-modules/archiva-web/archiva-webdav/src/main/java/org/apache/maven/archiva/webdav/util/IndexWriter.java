package org.apache.maven.archiva.webdav.util;

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

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.io.OutputContext;

import java.util.Date;
import java.io.PrintWriter;
import java.io.File;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class IndexWriter
{
    private final DavResource resource;

    private final File localResource;

    private final String logicalResource;

    public IndexWriter(DavResource resource, File localResource, String logicalResource)
    {
        this.resource = resource;
        this.localResource = localResource;
        this.logicalResource = logicalResource;
    }

    public void write(OutputContext outputContext)
    {
        outputContext.setModificationTime(new Date().getTime());
        outputContext.setContentType("text/html");
        outputContext.setETag("");
        if (outputContext.hasStream())
        {
            PrintWriter writer = new PrintWriter(outputContext.getOutputStream());
            writeDocumentStart(writer);
            writeHyperlinks(writer);
            writeDocumentEnd(writer);
            writer.flush();
            writer.close();
        } 
    }

    private void writeDocumentStart(PrintWriter writer)
    {
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Collection: " + logicalResource + "<title>");
        writer.println("</head>");
        writer.println("<h3>Collection: " + logicalResource + "</h3>");

        //Check if not root
        if (!"/".equals(logicalResource))
        {
            File file = new File(logicalResource);
            String parentName = file.getParent().equals("") ? "/" : file.getParent();

            writer.println("<ul>");
            writer.println("<li><a href=\"../\">" + parentName + "</a> <i><small>(Parent)</small></i></li>");
            writer.println("</ul>");
        }

        writer.println("<ul>");
    }

    private void writeDocumentEnd(PrintWriter writer)
    {
        writer.println("</ul>");
        writer.println("</body>");
        writer.println("</html>");
    }

    private void writeHyperlinks(PrintWriter writer)
    {
        for (File file : localResource.listFiles())
        {
            writeHyperlink(writer, file.getName(), file.isDirectory());
        }
    }

    private void writeHyperlink(PrintWriter writer, String resourceName, boolean directory)
    {
        if (directory)
        {
            writer.println("<li><a href=\"./" + resourceName + "/\">" + resourceName + "</a></li>");
        }
        else
        {
            writer.println("<li><a href=\"./" + resourceName + "\">" + resourceName + "</a></li>");
        }
    }
}
