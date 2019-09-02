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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 */
public class RepositoryPathUtil
{

    public static String getRepositoryName( final String href )
    {
        String requestPathInfo = StringUtils.defaultString( href );

        //remove prefix ie /repository/blah becomes /blah
        requestPathInfo = removePrefix( requestPathInfo );

        // Remove prefixing slash as the repository id doesn't contain it;
        if ( requestPathInfo.startsWith( "/" ) )
        {
            requestPathInfo = requestPathInfo.substring( 1 );
        }

        // Find first element, if slash exists.
        int slash = requestPathInfo.indexOf( '/' );
        if ( slash > 0 )
        {
            // Filtered: "central/org/apache/maven/" -> "central"
            return requestPathInfo.substring( 0, slash );
        }
        return requestPathInfo;
    }

    private static String removePrefix( final String href )
    {
        String[] parts = StringUtils.split( href, '/' );
        parts = (String[]) ArrayUtils.subarray( parts, 1, parts.length );
        if ( parts == null || parts.length == 0 )
        {
            return "/";
        }

        String joinedString = StringUtils.join( parts, '/' );
        if ( href.endsWith( "/" ) )
        {
            joinedString = joinedString + "/";
        }

        return joinedString;
    }
}
