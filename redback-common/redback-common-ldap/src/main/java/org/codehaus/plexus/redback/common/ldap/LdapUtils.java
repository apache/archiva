package org.codehaus.plexus.redback.common.ldap;

/*
 * Copyright 2001-2007 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 * 
 * @version $Id$
 */
public final class LdapUtils
{
    private LdapUtils()
    {
        // no op
    }

    @SuppressWarnings("unchecked")
    public static String getLabeledUriValue( Attributes attributes, String attrName, String label,
                                             String attributeDescription )
        throws MappingException
    {
        if ( attrName == null )
        {
            return null;
        }

        Attribute attribute = attributes.get( attrName );
        if ( attribute != null )
        {
            NamingEnumeration attrs;
            try
            {
                attrs = attribute.getAll();
            }
            catch ( NamingException e )
            {
                throw new MappingException(
                    "Failed to retrieve " + attributeDescription + " (attribute: \'" + attrName + "\').", e );
            }

            while ( attrs.hasMoreElements() )
            {
                Object value = attrs.nextElement();

                String val = String.valueOf( value );

                if ( val.endsWith( " " + label ) )
                {
                    return val.substring( 0, val.length() - ( label.length() + 1 ) );
                }
            }
        }

        return null;
    }

    public static String getAttributeValue( Attributes attributes, String attrName, String attributeDescription )
        throws MappingException
    {
        if ( attrName == null )
        {
            return null;
        }

        Attribute attribute = attributes.get( attrName );
        if ( attribute != null )
        {
            try
            {
                Object value = attribute.get();

                return String.valueOf( value );
            }
            catch ( NamingException e )
            {
                throw new MappingException(
                    "Failed to retrieve " + attributeDescription + " (attribute: \'" + attrName + "\').", e );
            }
        }

        return null;
    }

    public static String getAttributeValueFromByteArray( Attributes attributes, String attrName,
                                                         String attributeDescription )
        throws MappingException
    {
        if ( attrName == null )
        {
            return null;
        }

        Attribute attribute = attributes.get( attrName );
        if ( attribute != null )
        {
            try
            {
                byte[] value = (byte[]) attribute.get();

                return new String( value );
            }
            catch ( NamingException e )
            {
                throw new MappingException(
                    "Failed to retrieve " + attributeDescription + " (attribute: \'" + attrName + "\').", e );
            }
        }

        return null;
    }
}
