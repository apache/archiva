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

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@XmlRootElement ( name = "userManagerImplementationInformation" )
public class UserManagerImplementationInformation
    implements Serializable
{
    private String beanId;

    private String descriptionKey;

    public UserManagerImplementationInformation()
    {
        // no op
    }

    public UserManagerImplementationInformation( String beanId, String descriptionKey )
    {
        this.beanId = beanId;
        this.descriptionKey = descriptionKey;
    }

    public String getBeanId()
    {
        return beanId;
    }

    public void setBeanId( String beanId )
    {
        this.beanId = beanId;
    }

    public String getDescriptionKey()
    {
        return descriptionKey;
    }

    public void setDescriptionKey( String descriptionKey )
    {
        this.descriptionKey = descriptionKey;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "UserManagerImplementationInformation" );
        sb.append( "{beanId='" ).append( beanId ).append( '\'' );
        sb.append( ", descriptionKey='" ).append( descriptionKey ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof UserManagerImplementationInformation ) )
        {
            return false;
        }

        UserManagerImplementationInformation that = (UserManagerImplementationInformation) o;

        if ( !beanId.equals( that.beanId ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return beanId.hashCode();
    }
}
