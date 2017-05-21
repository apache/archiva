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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "scm")
public class Scm
    implements Serializable
{
    private String connection;

    private String developerConnection;

    private String url;

    public Scm()
    {
        // no op
    }

    public Scm( String connection, String developerConnection, String url )
    {
        this.connection = connection;
        this.developerConnection = developerConnection;
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getDeveloperConnection()
    {
        return developerConnection;
    }

    public void setDeveloperConnection( String developerConnection )
    {
        this.developerConnection = developerConnection;
    }

    public String getConnection()
    {
        return connection;
    }

    public void setConnection( String connection )
    {
        this.connection = connection;
    }

    @Override
    public String toString()
    {
        return "Scm{" +
            "connection='" + connection + '\'' +
            ", developerConnection='" + developerConnection + '\'' +
            ", url='" + url + '\'' +
            '}';
    }
}
