package org.apache.archiva.rest.api.model.v2;/*
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

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="beanInformation")
public class BeanInformation implements Serializable
{
    private static final long serialVersionUID = -432385743277355987L;
    String id;
    String displayName;
    String descriptionKey;
    String defaultDescription;
    boolean readonly;

    @Schema(description = "The identifier")
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema(description = "The display name")
    public String getDisplayName( )
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @Schema(description = "The translation key for the description")
    public String getDescriptionKey( )
    {
        return descriptionKey;
    }

    public void setDescriptionKey( String descriptionKey )
    {
        this.descriptionKey = descriptionKey;
    }

    @Schema(description = "The description translated in the default language")
    public String getDefaultDescription( )
    {
        return defaultDescription;
    }

    public void setDefaultDescription( String defaultDescription )
    {
        this.defaultDescription = defaultDescription;
    }

    @Schema(description = "True, if this bean cannot be removed")
    public boolean isReadonly( )
    {
        return readonly;
    }

    public void setReadonly( boolean readonly )
    {
        this.readonly = readonly;
    }
}
