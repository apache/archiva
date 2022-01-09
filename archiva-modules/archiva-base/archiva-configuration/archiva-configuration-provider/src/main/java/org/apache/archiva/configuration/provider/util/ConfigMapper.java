package org.apache.archiva.configuration.provider.util;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class that can be used for mapping configuration keys (e.g. user configuration keys) to
 * archiva configuration objects.
 *
 * @param <T> The class used to retrieve the attribute data
 * @param <K> The class used to retrieve the data that is for prefix matching
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class ConfigMapper<T, K>
{
    private final Map<String, Function<T,  String>> stringFunctionMap = new HashMap<>( );
    private final Map<String, Function<T,  Integer>> intFunctionMap = new HashMap<>( );
    private final Map<String, Function<T,  Boolean>> booleanFunctionMap = new HashMap<>( );
    private final Map<String, BiFunction<String, K, String>> prefixStringFunctionMap = new HashMap<>( );

    public void addStringMapping( String attributeName, Function<T, String> mapping) {
        this.stringFunctionMap.put( attributeName, mapping );
    }

    public void  addPrefixStringMapping(String prefix, BiFunction<String, K, String> mapping) {
        prefixStringFunctionMap.put( prefix, mapping );
    }

    public String getString( String attributeName, T instance) {
        return stringFunctionMap.get( attributeName ).apply( instance );
    }

    public String getPrefixString(String attributeName, K instance) {
        BiFunction<String, K, String> function = prefixStringFunctionMap.entrySet( ).stream( ).filter( entry -> attributeName.startsWith( entry.getKey( ) ) ).findFirst( )
            .map( entry -> entry.getValue( ) )
            .get( );
        return function.apply( attributeName, instance );
    }

    public boolean isStringMapping(String attributeName) {
        return stringFunctionMap.containsKey( attributeName );
    }

    public boolean isIntMapping(String attributeName) {
        return intFunctionMap.containsKey( attributeName );
    }

    public boolean isBooleanMapping(String attributeName) {
        return booleanFunctionMap.containsKey( attributeName );
    }

    public boolean isPrefixMapping(String attributeName) {
        return prefixStringFunctionMap.keySet( ).stream( ).anyMatch( prefix -> attributeName.startsWith( prefix ) );
    }

    public boolean isMapping(String attributeName) {
        return isStringMapping( attributeName ) || isIntMapping( attributeName ) || isBooleanMapping( attributeName );
    }

    public void addIntMapping( String attributeName, Function<T, Integer> mapping) {
        this.intFunctionMap.put( attributeName, mapping );
    }

    public int getInt( String attributeName, T instance) {
        return this.intFunctionMap.get( attributeName ).apply( instance );
    }

    public void addBooleanMapping( String attributeName, Function<T, Boolean> mapping) {
        this.booleanFunctionMap.put( attributeName, mapping );
    }

    public boolean getBoolean( String attributeName, T instance) {
        return this.booleanFunctionMap.get( attributeName ).apply( instance );
    }

    public List<String> getStringAttributes() {
        return new ArrayList<>( stringFunctionMap.keySet( ) );
    }

    public List<String> getIntAttributes() {
        return new ArrayList<>( intFunctionMap.keySet( ) );
    }

    public List<String> getBooleanAttributes() {
        return new ArrayList<>( booleanFunctionMap.keySet( ) );
    }

    public List<String> getAllAttributes() {
        return Arrays.asList( stringFunctionMap,intFunctionMap, booleanFunctionMap).stream()
            .flatMap( map->map.keySet().stream() ).collect( Collectors.toList());
    }

}
