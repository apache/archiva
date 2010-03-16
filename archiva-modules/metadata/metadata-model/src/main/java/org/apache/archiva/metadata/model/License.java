package org.apache.archiva.metadata.model;

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

/**
 * A description of a particular license used by a project.
 */
public class License
{
    /**
     * The name of the license.
     */
    private String name;

    /**
     * The URL of the license text.
     */
    private String url;

    public License( String name, String url )
    {
        this.name = name;
        this.url = url;
    }

    public License()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        License license = (License) o;

        if ( name != null ? !name.equals( license.name ) : license.name != null )
        {
            return false;
        }
        if ( url != null ? !url.equals( license.url ) : license.url != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + ( url != null ? url.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "License{" + "name='" + name + '\'' + ", url='" + url + '\'' + '}';
    }
}
