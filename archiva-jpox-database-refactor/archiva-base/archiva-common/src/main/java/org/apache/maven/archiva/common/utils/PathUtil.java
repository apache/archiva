package org.apache.maven.archiva.common.utils;

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

import java.io.File;
import java.net.MalformedURLException;

/**
 * PathUtil - simple utility methods for path manipulation. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PathUtil
{
    public static String toUrl( String path )
    {
        // Is our work already done for us?
        if ( path.startsWith( "file:/" ) )
        {
            return path;
        }
        
        return toUrl( new File( path ) );
    }

    public static String toUrl( File file )
    {
        try
        {
            return file.toURL().toExternalForm();
        }
        catch ( MalformedURLException e )
        {
            String pathCorrected = StringUtils.replaceChars( file.getAbsolutePath(), '\\', '/' );
            if ( pathCorrected.startsWith( "file:/" ) )
            {
                return pathCorrected;
            }

            return "file://" + pathCorrected;
        }
    }

    public static String getRelative( String basedir, File file )
    {
        return getRelative( basedir, file.getAbsolutePath() );
    }

    public static String getRelative( String basedir, String child )
    {
        if ( child.startsWith( basedir ) )
        {
            // simple solution.
            return child.substring( basedir.length() + 1 );
        }

        String absoluteBasedir = new File( basedir ).getAbsolutePath();
        if ( child.startsWith( absoluteBasedir ) )
        {
            // resolved basedir solution.
            return child.substring( absoluteBasedir.length() + 1 );
        }

        // File is not within basedir.
        throw new IllegalStateException( "Unable to obtain relative path of file " + child
            + ", it is not within basedir " + basedir + "." );
    }
}
