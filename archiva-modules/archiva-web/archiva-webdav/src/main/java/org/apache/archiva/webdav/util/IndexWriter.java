package org.apache.archiva.webdav.util;

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

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.io.OutputContext;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 */
public class IndexWriter
{
    private final String logicalResource;

    private final List<File> localResources;

    private final boolean isVirtual;

    public IndexWriter( DavResource resource, File localResource, String logicalResource )
    {
        this.localResources = new ArrayList<>();
        this.localResources.add( localResource );
        this.logicalResource = logicalResource;
        this.isVirtual = false;
    }

    public IndexWriter( DavResource resource, List<File> localResources, String logicalResource )
    {
        this.logicalResource = logicalResource;
        this.localResources = localResources;
        this.isVirtual = true;
    }

    public void write( OutputContext outputContext )
    {
        outputContext.setModificationTime( new Date().getTime() );
        outputContext.setContentType( "text/html" );
        outputContext.setETag( "" ); // skygo ETag MRM-1127 seems to be fixed
        if ( outputContext.hasStream() )
        {
            PrintWriter writer = new PrintWriter( outputContext.getOutputStream() );
            writeDocumentStart( writer );
            writeHyperlinks( writer );
            writeDocumentEnd( writer );
            writer.flush();
            writer.close();
        }
    }

    private void writeDocumentStart( PrintWriter writer )
    {
        writer.println("<!DOCTYPE html>");
        writer.println( "<html>" );
        writer.println( "<head>" );
        writer.println( "<title>Collection: /" + logicalResource + "</title>" );
        writer.println( "<style type=\"text/css\">" );
        writer.println( "ul{list-style:none;}" ); 
        
        StringBuilder relative = new StringBuilder("../../");
        if ( logicalResource != null && logicalResource.length() > 0 )
        {
            String tmpRelative = StringUtils.replace( logicalResource, "\\", "/" );
            for (int i=0;i<tmpRelative.split("/").length;i++) 
            {
                relative.append("../");
            }
        }
        writer.println( ".file{background:url(" + relative.toString() + "images/package-x-generic.png) no-repeat scroll 0 0 transparent;}" );
        writer.println( ".folder{background:url(" + relative.toString() + "images/folder.png) no-repeat scroll 0 0 transparent;}" );
        writer.println( "a{color:#0088CC;text-decoration: none;padding-left:20px;}" );
        writer.println( ".collection tr:nth-child(odd){background-color:#fafafa;}" );
        writer.println( "tr td:nth-child(2){width:150px;color:#cc8800;text-align:right;}" );
        writer.println( "tr td:nth-child(3){width:150px;color:#0000cc;text-align:center;}" );
        writer.println( "th td:nth-child(2){width:150px;}" );
        writer.println( "th td:nth-child(3){width:150px;}" );
        writer.println( "</style>" );
        writer.println( "<link rel=\"shortcut icon\" href=\"../../favicon.ico\"/>" );
        writer.println( "</head>" );
        writer.println( "<body>" );
        writer.println( "<h3>Collection: /" + logicalResource + "</h3>" );

        //Check if not root
        if ( logicalResource != null && logicalResource.length() > 0 )
        {
            File file = new File( logicalResource );
            String parentName = file.getParent() == null ? "/" : file.getParent();

            //convert to unix path in case archiva is hosted on windows
            parentName = StringUtils.replace( parentName, "\\", "/" );

            writer.println( "<ul>" );
            writer.println( "<li><a class=\"folder\" href=\"../\">" + parentName + "</a> <i><small>(Parent)</small></i></li>" );
            writer.println( "</ul>" );
        }

        writer.println( "<table class=\"collection\">" );
        writer.println( "<tr><th>Name</th><th>Size (Bytes)</th><th>Last Modified</th></tr>" );
    }

    private void writeDocumentEnd( PrintWriter writer )
    {
        writer.println( "</table>" );
        writer.println( "</body>" );
        writer.println( "</html>" );
    }

    private void writeHyperlinks( PrintWriter writer )
    {
        if ( !isVirtual )
        {
            for ( File localResource : localResources )
            {
                List<File> files = new ArrayList<>( Arrays.asList( localResource.listFiles() ) );
                Collections.sort( files );

                for ( File file : files )
                {
                    writeHyperlink( writer, file.getName(), file.lastModified(), file.length(), file.isDirectory() );
                }
            }
        }
        else
        {
            // virtual repository - filter unique directories
            Map<String, List<String>> uniqueChildFiles = new HashMap<>();
            List<String> sortedList = new ArrayList<>();
            for ( File resource : localResources )
            {
                List<File> files = new ArrayList<>( Arrays.asList( resource.listFiles() ) );
                for ( File file : files )
                {
                    List<String> mergedChildFiles = new ArrayList<>();
                    if ( uniqueChildFiles.get( file.getName() ) == null )
                    {
                        mergedChildFiles.add( file.getAbsolutePath() );
                    }
                    else
                    {
                        mergedChildFiles = uniqueChildFiles.get( file.getName() );
                        if ( !mergedChildFiles.contains( file.getAbsolutePath() ) )
                        {
                            mergedChildFiles.add( file.getAbsolutePath() );
                        }
                    }
                    uniqueChildFiles.put( file.getName(), mergedChildFiles );
                    sortedList.add( file.getName() );
                }
            }

            Collections.sort( sortedList );
            List<String> written = new ArrayList<>();
            for ( String fileName : sortedList )
            {
                List<String> childFilesFromMap = uniqueChildFiles.get( fileName );
                for ( String childFilePath : childFilesFromMap )
                {
                    File childFile = new File( childFilePath );
                    if ( !written.contains( childFile.getName() ) )
                    {
                        written.add( childFile.getName() );
                        writeHyperlink( writer, fileName, childFile.lastModified(), childFile.length(), childFile.isDirectory() );
                    }
                }
            }
        }
    }

    private static String fileDateFormat( long date ) 
    {
        DateFormat dateFormatter = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault() );
        Date aDate = new Date( date );
        return dateFormatter.format( aDate );
    }
    
    private void writeHyperlink( PrintWriter writer, String resourceName, long lastModified, long fileSize, boolean directory )
    {
        if ( directory )
        {
            writer.println( "<tr><td><a class=\"folder\" href=\"" + resourceName + "/\">" + resourceName + "</a></td><td>&nbsp;</td><td>&nbsp;</td></tr>" );
        }
        else
        {
            writer.println( "<tr><td><a class=\"file\" href=\"" + resourceName + "\">" + resourceName + "</a></td><td class=\"size\">" + fileSize + "&nbsp;&nbsp;</td><td class=\"date\">" + fileDateFormat( lastModified ) + "</td></tr>" );
        }
    }
}
