package org.codehaus.redback.integration.util;

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * DateUtils
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DateUtils
{
    /**
     * Provided a date you will get a timestamp and the age/old that you want
     *
     * @param date   the date to compare to now.
     * @param suffix the suffix in the age string.  using "ago" here would result in "2006 Aug 23, 11:43 pm - 12 days ago"
     * @return the formated string.
     */
    public static String formatWithAge( Date date, String suffix )
    {
        return formatWithAge( date, "EEE, d MMM yyyy HH:mm:ss Z", suffix );
    }

    /**
     * Provided a date you will get a timestamp and the age/old that you want.
     *
     * @param date       the date to compare to now.
     * @param dateFormat the {@link SimpleDateFormat} format string to use for the date.
     * @param suffix     the suffix in the age string.  using "ago" here would result in "2006 Aug 23, 11:43 pm - 12 days ago"
     * @return the formated string.
     */
    public static String formatWithAge( Date date, String dateFormat, String suffix )
    {
        if ( date == null )
        {
            return null;
        }

        SimpleDateFormat format = new SimpleDateFormat( dateFormat );

        StringBuffer out = new StringBuffer();
        out.append( format.format( date ) );
        out.append( " - " );

        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTime( date );

        long diffMillis = now.getTimeInMillis() - then.getTimeInMillis();

        long days = diffMillis / ( 24 * 60 * 60 * 1000 );
        long hours = diffMillis / ( 60 * 60 * 1000 );
        long minutes = diffMillis / ( 60 * 1000 );
        long seconds = diffMillis / ( 1000 );

        if ( days > 0 )
        {
            out.append( String.valueOf( days ) ).append( " day" );
            if ( days > 1 )
            {
                out.append( 's' );
            }
        }
        else if ( hours > 0 )
        {
            out.append( String.valueOf( hours ) ).append( " hour" );
            if ( hours > 1 )
            {
                out.append( 's' );
            }
        }
        else if ( minutes > 0 )
        {
            out.append( String.valueOf( minutes ) ).append( " minute" );
            if ( minutes > 1 )
            {
                out.append( 's' );
            }
        }
        else if ( seconds > 0 )
        {
            out.append( String.valueOf( seconds ) ).append( " second" );
            if ( seconds > 1 )
            {
                out.append( 's' );
            }
        }

        out.append( ' ' ).append( suffix );

        return out.toString();
    }
}
