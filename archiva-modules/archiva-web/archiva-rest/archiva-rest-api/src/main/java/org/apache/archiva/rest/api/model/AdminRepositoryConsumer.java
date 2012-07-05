package org.apache.archiva.rest.api.model;

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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * AdminRepositoryConsumer
 *
 *
 */
@XmlRootElement( name = "adminRepositoryConsumer" )
public class AdminRepositoryConsumer
    implements Serializable
{
    private boolean enabled = false;

    private String id;

    private String description;

    public AdminRepositoryConsumer()
    {
        // no op
    }

    public AdminRepositoryConsumer( boolean enabled, String id, String description )
    {
        this.enabled = enabled;
        this.id = id;
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public void setId( String id )
    {
        this.id = id;
    }
}
