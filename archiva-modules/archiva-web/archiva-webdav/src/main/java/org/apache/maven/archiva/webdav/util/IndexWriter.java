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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;
import java.io.File;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class IndexWriter
{
    private final DavResource resource;

    private final String logicalResource;
    
    private final List<File> localResources;
    
    private final boolean isVirtual;
    
    public IndexWriter(DavResource resource, File localResource, String logicalResource)
    {
        this.resource = resource;
        this.localResources = new ArrayList<File>();
        this.localResources.add( localResource );
        this.logicalResource = logicalResource;
        this.isVirtual = false;
    }
    
    public IndexWriter( DavResource resource, List<File> localResources, String logicalResource )
    {
        this.resource = resource;
        this.logicalResource = logicalResource;
        this.localResources = localResources;
        this.isVirtual = true;
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
        writer.println("<title>Collection: " + logicalResource + "</title>");
        writer.println("</head>");
        writer.println("<body>");
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
        if( !isVirtual )
        {
            for( File localResource : localResources )
            {
                List<File> files = new ArrayList<File>( Arrays.asList( localResource.listFiles() ) ); 
                Collections.sort( files );
                
                for ( File file : files )
                {
                    writeHyperlink( writer, file.getName(), file.isDirectory() );
                }
            }
        }
        else 
        {            
            // virtual repository - filter unique directories
            Map<String, File> uniqueChildFiles = new HashMap<String, File>();
            List<String> sortedList = new ArrayList<String>();
            for( File resource : localResources )
            {
                List<File> files = new ArrayList<File>( Arrays.asList( resource.listFiles() ) ); 
                                                
                for ( File file : files )
                {   
                    if( uniqueChildFiles.get( file.getName() ) == null )
                    {
                        uniqueChildFiles.put( file.getName(), file );
                        sortedList.add( file.getName() );
                    }                    
                }
            }
             
            Collections.sort( sortedList );
            for ( String fileName : sortedList )
            {
                writeHyperlink( writer, fileName, ( (File) uniqueChildFiles.get( fileName ) ).isDirectory());
            }
        }
    }

    private void writeHyperlink(PrintWriter writer, String resourceName, boolean directory )
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
