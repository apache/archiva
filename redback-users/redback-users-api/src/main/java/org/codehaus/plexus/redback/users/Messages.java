package org.codehaus.plexus.redback.users;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Localized Message Handling.
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class Messages
{
    private static final String BUNDLE_NAME = "org.codehaus.plexus.redback.users.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME );

    /**
     * Get a Message as-is from the Resource Bundle.
     * 
     * @param key the key for the message to get.
     * @return the value of the key, or "!key!" if the key is not found.
     */
    public static String getString( String key )
    {
        try
        {
            return RESOURCE_BUNDLE.getString( key );
        }
        catch ( MissingResourceException e )
        {
            return '!' + key + '!';
        }
    }

    /**
     * Gets a Message from the Resource Bundle, with {1} and {2} style arguments.
     * 
     * @param key the key for the message to get.
     * @param arg the argument to pass in.
     * @return the value of the key, or "!key!" if the key is not found.
     */
    public static String getString( String key, Object arg )
    {
        return getString( key, new Object[] { arg } );
    }

    /**
     * Gets a Message from the Resource Bundle, with {1} and {2} style arguments.
     * 
     * @param key the key for the message to get.
     * @param args the arguments to pass in.
     * @return the value of the key, or "!key!" if the key is not found.
     */
    public static String getString( String key, Object args[] )
    {
        try
        {
            String pattern = RESOURCE_BUNDLE.getString( key );
            return MessageFormat.format( pattern, args );
        }
        catch ( MissingResourceException e )
        {
            return '!' + key + '!';
        }
    }

    /**
     * Prevent Instantiation.
     */
    private Messages()
    {
    }
}
