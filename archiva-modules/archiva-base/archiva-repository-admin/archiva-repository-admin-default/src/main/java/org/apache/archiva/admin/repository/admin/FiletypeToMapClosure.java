package org.apache.archiva.admin.repository.admin;

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

import org.apache.archiva.admin.model.beans.FileType;
import org.apache.commons.collections.Closure;

import java.util.HashMap;
import java.util.Map;

/**
 * FiletypeToMapClosure 
 *
 * @since 1.4-M1
 */
public class FiletypeToMapClosure
    implements Closure
{
    private Map<String, FileType> map = new HashMap<>();

    @Override
    public void execute( Object input )
    {
        if ( input instanceof FileType )
        {
            FileType filetype = (FileType) input;
            map.put( filetype.getId(), filetype );
        }
    }

    public Map<String, FileType> getMap()
    {
        return map;
    }
}
