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

import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.io.IOException;
import java.net.URL;

/**
 * Custom Archiva MimeTypes loader for plexus-webdav's {@link MimeTypes} 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @todo Support custom mime types from archiva-configuration.
 * 
 * @plexus.component role="org.apache.maven.archiva.web.repository.ArchivaMimeTypeLoader"
 */
public class ArchivaMimeTypeLoader
    implements Initializable
{
    /**
     * @plexus.requirement
     */
    private MimeTypes mimeTypes;

    public void initialize()
        throws InitializationException
    {
        // TODO: Make mime types loading configurable.
        // Load the mime types from archiva location.
        if ( mimeTypes.getMimeType( "sha1" ) == null )
        {
            URL url = this.getClass().getClassLoader().getResource( "/archiva-mime-types.txt" );
            if ( url == null )
            {
                url = this.getClass().getClassLoader().getResource( "archiva-mime-types.txt" );
            }

            if ( url != null )
            {
                try
                {
                    mimeTypes.load( url.openStream() );
                }
                catch ( IOException e )
                {
                    throw new InitializationException( "Unable to load archiva-mime-types.txt : " + e.getMessage(), e );
                }
            }
        }
    }
}
