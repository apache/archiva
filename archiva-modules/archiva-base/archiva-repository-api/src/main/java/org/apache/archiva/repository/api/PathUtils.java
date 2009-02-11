package org.apache.archiva.repository.api;

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

import java.util.Arrays;
import org.apache.commons.lang.StringUtils;

public final class PathUtils
{
    public static String getRepositoryId(String uri)
    {
        if (uri == null)
            return null;

        final String[] parts = StringUtils.split(uri, '/');
        if (parts != null && parts.length >= 1)
        {
            return parts[0];
        }
        return null;
    }

    public static String getLogicalPath(String uri)
    {
        String result = "/";

        if (uri == null)
            return result;

        final String[] parts = StringUtils.split(uri, '/');
        if (parts.length > 0)
        {
            result = "/" + StringUtils.join(Arrays.asList(parts).subList(1, parts.length), "/");
        }
        if (StringUtils.isEmpty(result))
        {
            result = "/";
        }
        return result;
    }
}
