package org.apache.archiva.common.utils;

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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * PathUtil - simple utility methods for path manipulation.
 *
 *
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

        return toUrl( Paths.get( path ) );
    }

    public static String toUrl( Path file )
    {
        try
        {
            return file.toUri().toURL().toExternalForm();
        }
        catch ( MalformedURLException e )
        {
            String pathCorrected = StringUtils.replaceChars( file.toAbsolutePath().toString(), '\\', '/' );
            if ( pathCorrected.startsWith( "file:/" ) )
            {
                return pathCorrected;
            }

            return "file://" + pathCorrected;
        }
    }

    /**
     * Given a basedir and a child file, return the relative path to the child.
     *
     * @param basedir the basedir.
     * @param file    the file to get the relative path for.
     * @return the relative path to the child. (NOTE: this path will NOT start with a file separator character)
     */
    public static String getRelative( Path basedir, Path file )
    {
        if (basedir.isAbsolute() && !file.isAbsolute()) {
            return basedir.normalize().relativize(file.toAbsolutePath()).toString();
        } else if (!basedir.isAbsolute() && file.isAbsolute()) {
            return basedir.toAbsolutePath().relativize(file.normalize()).toString();
        } else {
            return basedir.normalize().relativize(file.normalize()).toString();
        }
    }

    public static String getRelative(String basedir, Path file) {
        return getRelative(Paths.get(basedir), file);
    }

    /**
     * Given a basedir and a child file, return the relative path to the child.
     *
     * @param basedir the basedir.
     * @param child   the child path (can be a full path)
     * @return the relative path to the child. (NOTE: this path will NOT start with a file separator character)
     */
    public static String getRelative( String basedir, String child )
    {

        return getRelative(basedir, Paths.get(child));
    }
}
