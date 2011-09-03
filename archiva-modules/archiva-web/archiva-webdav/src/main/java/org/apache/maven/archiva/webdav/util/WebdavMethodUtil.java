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

import org.apache.commons.lang.StringUtils;
import org.apache.archiva.security.ArchivaRoleConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WebdavMethodUtil
 * 
 * @version $Id: WebdavMethodUtil.java 5412 2007-01-13 01:18:47Z joakime $
 */
public class WebdavMethodUtil
{
    private static final List<String> READ_METHODS;

    static
    {
        READ_METHODS = new ArrayList<String>();
        READ_METHODS.add( "HEAD" );
        READ_METHODS.add( "GET" );
        READ_METHODS.add( "PROPFIND" );
        READ_METHODS.add( "OPTIONS" );
        READ_METHODS.add( "REPORT" );
    }

    public static String getMethodPermission( String method )
    {
        if ( StringUtils.isBlank( method ) )
        {
            throw new IllegalArgumentException( "WebDAV method is empty" );
        }
        if ( READ_METHODS.contains( method.toUpperCase( Locale.US ) ) )
        {
            return ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS;
        }
        else if ( "DELETE".equals( method.toUpperCase( Locale.US ) ) )
        {
            return ArchivaRoleConstants.OPERATION_REPOSITORY_DELETE;
        }
        else
        {
            return ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD;
        }
    }

    public static boolean isReadMethod( String method )
    {
        if ( StringUtils.isBlank( method ) )
        {
            throw new IllegalArgumentException( "WebDAV method is empty" );
        }
        return READ_METHODS.contains( method.toUpperCase( Locale.US ) );
    }
}
