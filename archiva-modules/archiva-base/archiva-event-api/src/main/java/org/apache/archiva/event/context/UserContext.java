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
import java.util.Map;

/**
 * This context provides user information.
 *
 * @author Martin Schreier <martin_s@apache.org>
 */
public class UserContext implements EventContext, Serializable
{
    private static final long serialVersionUID = -3499164111736559781L;

    private static final String ID = "user";

    private final String userId;
    private final String remoteAddress;

    public UserContext( String user, String remoteAddress )
    {
        this.userId = user == null ? "" : user;
        this.remoteAddress = remoteAddress == null ? "" : remoteAddress;

    }

    public String getUserId( )
    {
        return userId;
    }

    public String getRemoteAddress( )
    {
        return remoteAddress;
    }

    @Override
    public Map<String, String> getData( )
    {
        Map<String, String> values = new HashMap<>( );
        values.put( ID +".user_id", userId );
        values.put( ID +".remote_address", remoteAddress );
        return values;
    }

    @Override
    public String getId( )
    {
        return ID;
    }
}
