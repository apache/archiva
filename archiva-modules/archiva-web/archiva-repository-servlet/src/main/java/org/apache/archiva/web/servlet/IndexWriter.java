package org.apache.archiva.web.servlet;

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

import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.archiva.repository.api.ResourceType;
import org.apache.commons.io.output.ByteArrayOutputStream;

public final class IndexWriter
{
    public static void write(List<Status> resources, ResourceContext context, HttpServletResponse resp, boolean writeContent) throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);

        resp.setDateHeader("last-modified", new Date().getTime());
        resp.setContentType("text/html");
        writeDocumentStart(context, writer);
        writeHyperlinks(resources, context, writer);
        writeDocumentEnd(writer);

        resp.setContentLength(outputStream.toByteArray().length);
        if (writeContent)
        {
            resp.getOutputStream().write(outputStream.toByteArray());
        }
    }

    private static void writeDocumentStart(ResourceContext context, PrintWriter writer)
    {
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Collection: " + context.getLogicalPath() + "</title>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("<h3>Collection: " + context.getLogicalPath() + "</h3>");

        //Check if not root
        if (!"/".equals(context.getLogicalPath()))
        {
            File file = new File(context.getLogicalPath());
            String parentName = file.getParent().equals("") ? "/" : file.getParent();

            //convert to unix path in case archiva is hosted on windows
            parentName = StringUtils.replace(parentName, "\\", "/" );

            writer.println("<ul>");
            writer.println("<li><a href=\"../\">" + parentName + "</a> <i><small>(Parent)</small></i></li>");
            writer.println("</ul>");
        }

        writer.println("<ul>");
    }

    private static void writeDocumentEnd(PrintWriter writer)
    {
        writer.println("</ul>");
        writer.println("</body>");
        writer.println("</html>");
    }

    private static void writeHyperlinks(List<Status> resources, ResourceContext context, PrintWriter writer)
    {
        ArrayList<Status> sortedResources = new ArrayList<Status>(resources);
        sortedResources.remove(0); //First entry is the stat for the collection itself
        Collections.sort(sortedResources, new Status.StatusNameComparator());

        for( Status status : sortedResources)
        {
            //Ignore hidden directories
            if (!status.getName().startsWith(".") || status.getName().startsWith(".index"))
            {
                writeHyperlink(writer, status);
            }
        }
    }

    private static void writeHyperlink(PrintWriter writer, Status status )
    {
        if (ResourceType.Collection.equals(status.getResourceType()))
        {
            writer.println("<li><a href=\"" + status.getName() + "/\">" + status.getName() + "</a></li>");
        }
        else
        {
            writer.println("<li><a href=\"" + status.getName() + "\">" + status.getName() + "</a></li>");
        }
    }
}