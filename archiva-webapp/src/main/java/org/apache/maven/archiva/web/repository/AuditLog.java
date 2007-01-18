package org.apache.maven.archiva.web.repository;

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
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * AuditLog - Audit Log. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.web.repository.AuditLog"
 */
public class AuditLog
    implements DavServerListener, Initializable
{
    public static final String ROLE = AuditLog.class.getName();

    /**
     * @plexus.configuration default-value="${appserver.base}/logs/audit.log"
     */
    private File logFile;

    /**
     * @plexus.configuration default-value="yyyy-MM-dd HH:mm:ss"
     */
    private String timestampFormat;

    private PrintWriter writer;

    private SimpleDateFormat timestamp;

    private String getServerId( DavServerComponent server )
    {
        return "[" + server.getPrefix() + "]";
    }

    public void serverCollectionCreated( DavServerComponent server, String resource )
    {
        log( getServerId( server ) + " Created Directory \"" + resource + "\"" );
    }

    public void serverCollectionRemoved( DavServerComponent server, String resource )
    {
        log( getServerId( server ) + " Removed Directory \"" + resource + "\"" );
    }

    public void serverResourceCreated( DavServerComponent server, String resource )
    {
        log( getServerId( server ) + " Created File \"" + resource + "\"" );
    }

    public void serverResourceModified( DavServerComponent server, String resource )
    {
        log( getServerId( server ) + " Modified Existing File \"" + resource + "\"" );
    }

    public void serverResourceRemoved( DavServerComponent server, String resource )
    {
        log( getServerId( server ) + " Removed File \"" + resource + "\"" );
    }

    /**
     * Log the message to the file.
     * 
     * @param msg the message.
     */
    public void log( String msg )
    {
        // Synchronize to prevent threading issues.
        synchronized ( writer )
        {
            writer.println( timestamp.format( new Date() ) + " - " + msg );
            // Manually flush buffer to ensure data is written to disk.
            writer.flush();
        }
    }

    public void initialize()
        throws InitializationException
    {
        File parentDir = logFile.getParentFile();
        if ( parentDir != null )
        {
            if ( !parentDir.exists() )
            {
                parentDir.mkdirs();
            }
        }

        if ( StringUtils.isBlank( timestampFormat ) )
        {
            timestampFormat = "yyyy-MM-dd HH:mm:ss";
        }

        timestamp = new SimpleDateFormat( timestampFormat );

        try
        {
            writer = new PrintWriter( new FileWriter( logFile ) );
            log( "Logging Initialized." );
        }
        catch ( IOException e )
        {
            throw new InitializationException( "Unable to initialize log file writer: " + logFile.getAbsolutePath(), e );
        }
    }
}
