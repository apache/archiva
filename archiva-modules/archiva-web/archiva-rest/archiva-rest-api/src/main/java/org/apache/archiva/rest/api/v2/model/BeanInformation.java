package org.apache.archiva.rest.api.v2.model;/*
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

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="beanInformation")
@Schema(name="BeanInformation",description = "Information about a bean instance.")
public class BeanInformation implements Serializable, RestModel
{
    private static final long serialVersionUID = -432385743277355987L;
    private String id;
    private String displayName;
    private String descriptionKey;
    private String defaultDescription;
    private boolean readonly;

    public BeanInformation( )
    {
    }

    public BeanInformation( String id, String displayName, String descriptionKey, String defaultDescription, boolean readonly )
    {
        this.id = id;
        this.displayName = displayName;
        this.descriptionKey = descriptionKey;
        this.defaultDescription = defaultDescription;
        this.readonly = readonly;
    }

    @Schema(description = "The identifier")
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema(name="display_name", description = "The display name")
    public String getDisplayName( )
    {
        return displayName;
    }

    public void setDisplayName( String displayName )
    {
        this.displayName = displayName;
    }

    @Schema(name="description_key", description = "The translation key for the description")
    public String getDescriptionKey( )
    {
        return descriptionKey;
    }

    public void setDescriptionKey( String descriptionKey )
    {
        this.descriptionKey = descriptionKey;
    }

    @Schema(name="default_description", description = "The description translated in the default language")
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

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        BeanInformation that = (BeanInformation) o;
        return readonly == that.readonly && id.equals( that.id ) && Objects.equals( displayName, that.displayName ) && Objects.equals( descriptionKey, that.descriptionKey ) && Objects.equals( defaultDescription, that.defaultDescription );
    }

    @Override
    public int hashCode( )
    {
        return Objects.hash( id, displayName, descriptionKey, defaultDescription, readonly );
    }

    @Override
    public String toString( )
    {
        return "BeanInformation{" +
            "id='" + id + '\'' +
            ", display_name='" + displayName + '\'' +
            ", description_key='" + descriptionKey + '\'' +
            ", default_description='" + defaultDescription + '\'' +
            ", readonly=" + readonly +
            '}';
    }
}
