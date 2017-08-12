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

import java.util.Properties;

/**
 * Metadata on project level.
 * Namely the namespace and the project id. But different repository types may
 * add additional metadata information.
 *
 */
public class ProjectMetadata
{
    private Properties customProperties;

    private String namespace;

    private String id;

    /**
     * Sets the project id.
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;        
    }

    /**
     * Returns the project id.
     * @return
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns the namespace where the project resides.
     * @return The namespace.
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Sets the namespace. Namespaces are strings that may contain '.' characters to separate
     * the hierarchy levels.
     * @return
     */
    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    /**
     * Adds a custom property. Repository storage implementations may add custom properties
     * on the project level.
     * @param key
     * @param value
     */
    public void addProperty(String key, String value) {
        Properties props = getProperties();
        props.setProperty( key, value );
    }

    /**
     * Replaces all custom properties with the given properties object.
     * The given object is stored by reference and not copied.
     * @param properties
     */
    public void setProperties(Properties properties) {
        this.customProperties = properties;
    }


    /**
     * Returns the object with all custom properties.
     * If there are no custom properties set, a empty object will be returned.
     *
     * @return The custom properties.
     */
    public Properties getProperties() {
        if (customProperties==null)
        {
            Properties props = new Properties( );
            this.customProperties = props;
            return props;
        } else {
            return this.customProperties;
        }
    }

    /**
     * Returns true, if there are custom properties set.
     * @return True, if there exist custom properties.
     */
    public boolean hasProperties() {
        return this.customProperties != null && this.customProperties.size()>0;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "ProjectMetadata{" );
        sb.append( "namespace='" ).append( namespace ).append( '\'' );
        sb.append( ", id='" ).append( id ).append( '\'' );
        if (customProperties!=null) {
            sb.append(", custom: '").append(customProperties.toString()).append('\'');
        }
        sb.append( '}' );
        return sb.toString();
    }


}
