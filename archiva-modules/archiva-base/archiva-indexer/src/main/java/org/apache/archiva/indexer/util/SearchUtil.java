package org.apache.archiva.indexer.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;

/**
 * SearchUtil - utility class for search.
 * 
 * @version
 */
public class SearchUtil
{
    public static final String BYTECODE_KEYWORD = "bytecode:";

    /**
     * Determines whether the queryString has the bytecode keyword.
     * 
     * @param queryString
     * @return
     */
    public static boolean isBytecodeSearch( String queryString )
    {
        if ( queryString.startsWith( BYTECODE_KEYWORD ) )
        {
            return true;
        }

        return false;
    }

    /**
     * Removes the bytecode keyword from the query string.
     * 
     * @param queryString
     * @return
     */
    public static String removeBytecodeKeyword( String queryString )
    {
        String qString = StringUtils.uncapitalize( queryString );
        qString = StringUtils.remove( queryString, BYTECODE_KEYWORD );

        return qString;
    }
    
    public static String getHitId( String groupId, String artifactId )
    {
        return groupId + ":" + artifactId;
    }
}
