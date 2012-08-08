package org.apache.archiva.common.utils;

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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * DateUtil - some (not-so) common date utility methods.
 *
 *
 */
public class DateUtil
{
    public static String getDuration( long duration )
    {
        return getDuration( new Date( 0 ), new Date( duration ) );
    }

    public static String getDuration( long ms1, long ms2 )
    {
        return getDuration( new Date( ms1 ), new Date( ms2 ) );
    }

    public static String getDuration( Date d1, Date d2 )
    {
        Calendar cal1 = new GregorianCalendar();
        cal1.setTime( d1 );

        Calendar cal2 = new GregorianCalendar();
        cal2.setTime( d2 );

        return getDuration( cal1, cal2 );
    }

    public static String getDuration( Calendar cal1, Calendar cal2 )
    {
        int year1 = cal1.get( Calendar.YEAR );
        int day1 = cal1.get( Calendar.DAY_OF_YEAR );
        int hour1 = cal1.get( Calendar.HOUR_OF_DAY );
        int min1 = cal1.get( Calendar.MINUTE );
        int sec1 = cal1.get( Calendar.SECOND );
        int ms1 = cal1.get( Calendar.MILLISECOND );

        int year2 = cal2.get( Calendar.YEAR );
        int day2 = cal2.get( Calendar.DAY_OF_YEAR );
        int hour2 = cal2.get( Calendar.HOUR_OF_DAY );
        int min2 = cal2.get( Calendar.MINUTE );
        int sec2 = cal2.get( Calendar.SECOND );
        int ms2 = cal2.get( Calendar.MILLISECOND );

        int leftDays = ( day1 - day2 ) + ( year1 - year2 ) * 365;
        int leftHours = hour2 - hour1;
        int leftMins = min2 - min1;
        int leftSeconds = sec2 - sec1;
        int leftMilliSeconds = ms2 - ms1;

        if ( leftMilliSeconds < 0 )
        {
            leftMilliSeconds += 1000;
            --leftSeconds;
        }

        if ( leftSeconds < 0 )
        {
            leftSeconds += 60;
            --leftMins;
        }

        if ( leftMins < 0 )
        {
            leftMins += 60;
            --leftHours;
        }

        if ( leftHours < 0 )
        {
            leftHours += 24;
            --leftDays;
        }

        StringBuilder interval = new StringBuilder();

        appendInterval( interval, leftDays, "Day" );
        appendInterval( interval, leftHours, "Hour" );
        appendInterval( interval, leftMins, "Minute" );
        appendInterval( interval, leftSeconds, "Second" );
        appendInterval( interval, leftMilliSeconds, "Millisecond" );

        return interval.toString();
    }

    private static void appendInterval( StringBuilder interval, int count, String type )
    {
        if ( count > 0 )
        {
            if ( interval.length() > 0 )
            {
                interval.append( " " );
            }

            interval.append( count );
            interval.append( " " ).append( type );
            if ( count > 1 )
            {
                interval.append( "s" );
            }
        }
    }

}
