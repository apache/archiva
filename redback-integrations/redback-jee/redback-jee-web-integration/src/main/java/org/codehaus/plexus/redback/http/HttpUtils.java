package org.codehaus.plexus.redback.http;

/*
 * Copyright 2005-2006 The Codehaus.
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

import java.util.Properties;

import org.codehaus.plexus.util.StringUtils;

/**
 * Collection of Utility methods useful in an Http environment.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @todo should move this to plexus-utils or plexus-utils-web
 */
public class HttpUtils
{
    /**
     * Convert typical complex header into properties.
     * <p/>
     * <p/>
     * Example:
     * </p>
     * <p/>
     * <code>
     * realm="Somewhere Over The Rainbow", domain="kansas.co.us", nonce="65743ABCF"
     * </code>
     * <p/>
     * <p>becomes</p>
     * <p/>
     * <code>
     * Map ( "realm",  "Somewhere Over The Rainbox" )
     * Map ( "domain", "kansas.co.us" )
     * Map ( "nonce",  "65743ABCF" )
     * </code>
     *
     * @param rawheader
     * @param majorDelim
     * @param subDelim
     * @return
     */
    public static Properties complexHeaderToProperties( String rawheader, String majorDelim, String subDelim )
    {
        Properties ret = new Properties();

        if ( StringUtils.isEmpty( rawheader ) )
        {
            return ret;
        }

        String array[] = StringUtils.split( rawheader, majorDelim );
        for ( int i = 0; i < array.length; i++ )
        {
            // String quotes.
            String rawelem = StringUtils.replace( array[i], "\"", "" );
            String parts[] = StringUtils.split( rawelem, subDelim, 2 );

            ret.setProperty( StringUtils.trim( parts[0] ), StringUtils.trim( parts[1] ) );
        }

        return ret;
    }
}
