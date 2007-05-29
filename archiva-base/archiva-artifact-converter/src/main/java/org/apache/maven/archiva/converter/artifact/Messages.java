package org.apache.maven.archiva.converter.artifact;

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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Messages 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class Messages
{
    private static final String BUNDLE_NAME = "org.apache.maven.archiva.converter.artifact.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME );

    private Messages()
    {
    }

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
    
    public static String getString( String key, Object argument )
    {
        return getString( key, new Object[] { argument } );
    }
    
    public static String getString( String key, Object arguments[] )
    {
        try
        {
            String pattern = RESOURCE_BUNDLE.getString( key );
            return MessageFormat.format( pattern, arguments );
        }
        catch ( MissingResourceException e )
        {
            return '!' + key + '!';
        }
    }
}
