package org.apache.archiva.configuration.provider;

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

/**
 * ConfigurationEvent
 *
 *
 */
public class ConfigurationEvent
{
    public static final int SAVED = 1;

    public static final int CHANGED = 2;

    private int type;

    private String tag;

    public ConfigurationEvent( int type )
    {
        this.type = type;
        tag = "";
    }

    public ConfigurationEvent(int type, String tag) {
        this.type = type;
        this.tag = tag;
    }

    public int getType()
    {
        return type;
    }

    public String getTag( )
    {
        return tag;
    }

    public void setTag( String tag )
    {
        this.tag = tag;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        ConfigurationEvent that = (ConfigurationEvent) o;

        if ( type != that.type ) return false;
        return tag.equals( that.tag );
    }

    @Override
    public int hashCode( )
    {
        int result = type;
        result = 31 * result + tag.hashCode( );
        return result;
    }
}
