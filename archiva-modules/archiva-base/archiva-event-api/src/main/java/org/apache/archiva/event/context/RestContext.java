package org.apache.archiva.event.context;
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

import org.apache.archiva.event.EventContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides information about a REST call.
 *
 * @author Martin Schreier <martin_s@apache.org>
 */
public class RestContext implements EventContext, Serializable
{
    private static final long serialVersionUID = -4109505194250928317L;

    public static final String ID = "rest";

    private final String service;
    private final String path;
    private final String operation;
    private final String requestMethod;
    private final int resultCode;
    private final Map<String, List<String>> pathParameter;


    public RestContext( String path, String service, String operation, String requestMethod, int resultCode,
                        Map<String, List<String>> pathParameter)
    {
        this.service = service;
        this.path = path;
        this.operation = operation;
        this.resultCode = resultCode;
        this.requestMethod = requestMethod;
        this.pathParameter = pathParameter;
    }

    public String getService( )
    {
        return service;
    }

    public String getPath( )
    {
        return path;
    }

    public String getOperation( )
    {
        return operation;
    }

    public String getRequestMethod( )
    {
        return requestMethod;
    }

    public int getResultCode( )
    {
        return resultCode;
    }

    public Map<String, List<String>> getPathParameter() {
        return pathParameter;
    }

    @Override
    public Map<String, String> getData( )
    {
        Map<String, String> values = new HashMap<>( );
        values.put( ID +".service", service );
        values.put( ID +".path", path );
        values.put( ID +".operation", operation );
        values.put( ID +".requestMethod", requestMethod );
        values.put( ID + ".pathParameter", getParamString( ) );
        return values;
    }

    @Override
    public String getId( )
    {
        return ID;
    }

    private String getParamString() {
        StringBuilder sb = new StringBuilder( );
        for(Map.Entry<String, List<String>> entry : pathParameter.entrySet()) {
            sb.append( entry.getKey( ) ).append( String.join( ",", entry.getValue( ) ) );
        }
        return sb.toString( );
    }
}
