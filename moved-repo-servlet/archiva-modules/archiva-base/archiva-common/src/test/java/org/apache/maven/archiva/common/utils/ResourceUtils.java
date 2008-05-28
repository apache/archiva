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
import java.io.IOException;
import java.net.URL;

/**
 * <code>ResourceUtils</code>
 */
public class ResourceUtils
{
    /**
     * Lookup resource at the given path relative to the root of the classpath and if it exists return a file object
     * that can be used to access it.
     * <p>
     * At test time the contents of both the src/resources and test/resources dirs are available at the root of the
     * classpath.
     * <p>
     * To retrieve the file src/test/resources/sometest/test.properties use getResource("/sometest/test.properties").
     * 
     * @param resourcePath the path to the resource relative to the root of the classpath
     * @return File a file object pointing to the resource on the classpath or null if the resource cannot be found
     */
    public static File getResource( String resourcePath )
        throws IOException
    {
        return getResource( resourcePath, null );
    }

    /**
     * Lookup resource at the given path relative to the root of the classpath and if it exists return a file object
     * that can be used to access it.
     * <p>
     * At test time the contents of both the src/resources and test/resources dirs are available at the root of the
     * classpath.
     * <p>
     * To retrieve the file src/test/resources/sometest/test.properties use getResource("/sometest/test.properties").
     * 
     * @param resourcePath the path to the resource relative to the root of the classpath
     * @param classloader the classloader who's classpath should be searched for the resource
     * @return File a file object pointing to the resource on the classpath or null if the resource cannot be found
     */
    public static File getResource( String resourcePath, ClassLoader classloader )
        throws IOException
    {
        File testResource = null;

        if ( StringUtils.isNotBlank( resourcePath ) )
        {
            // make sure the retrieval is relative to the root of the classpath
            resourcePath = resourcePath.startsWith( "/" ) ? resourcePath : "/" + resourcePath;

            URL resourceUrl = getResourceUrl( resourcePath, classloader );
            if ( resourceUrl == null )
            {
                throw new IOException( "Could not find test resource at path '" + resourcePath + "'" );
            }
            testResource = new File( resourceUrl.getFile() );
        }

        return testResource;
    }

    private static URL getResourceUrl( String resourcePath, ClassLoader classloader )
    {
        return classloader != null ? classloader.getResource( resourcePath )
                        : ResourceUtils.class.getResource( resourcePath );
    }
}
