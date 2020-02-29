package org.apache.archiva.repository.storage.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.storage.StorageAsset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class VisitStatus
{

    LinkedHashMap<String, LinkedList<StorageAsset>> applied = new LinkedHashMap<>( );
    LinkedList<StorageAsset> visited = new LinkedList<>( );

    public VisitStatus( )
    {

    }

    public void add( StorageAsset asset )
    {
        // System.out.println( "Adding " + asset.getPath( ) );
        visited.addLast( asset );
    }

    public void add(String type, StorageAsset asset) {
        if (!applied.containsKey( type )) {
            applied.put( type, new LinkedList<>( ) );
        }
        applied.get( type ).add( asset );
    }

    public StorageAsset getLast( )
    {
        return visited.getLast( );
    }

    public StorageAsset getFirst()  {
        return visited.getFirst( );
    }

    public List<StorageAsset> getVisited( )
    {
        return visited;
    }

    public int size( )
    {
        return visited.size( );
    }

    public int size(String type) {
        return applied.get( type ).size( );
    }


}
