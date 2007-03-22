package org.apache.maven.archiva.common.utils;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;

/**
 * DateUtilTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DateUtilTest extends TestCase
{
    private void assertDuration( String expectedDuration, String startTimestamp, String endTimestamp )
        throws ParseException
    {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss SSS" );
        Date startDate = sdf.parse( startTimestamp );
        Date endDate = sdf.parse( endTimestamp );
        
//        System.out.println( "Date: " + endTimestamp + " - " + startTimestamp + " = "
//                        + ( endDate.getTime() - startDate.getTime() ) + " ms" );

        assertEquals( expectedDuration, DateUtil.getDuration( startDate, endDate ) );
    }

    public void testGetDurationDifference() throws ParseException
    {
        assertDuration( "2 Seconds", "2006-08-22 13:00:02 0000", 
                                     "2006-08-22 13:00:04 0000" );

        assertDuration( "12 Minutes 12 Seconds 234 Milliseconds", "2006-08-22 13:12:02 0000",
                                                                  "2006-08-22 13:24:14 0234" );
        
        assertDuration( "12 Minutes 501 Milliseconds", "2006-08-22 13:12:01 0500",
                                                       "2006-08-22 13:24:02 0001" );
    }
    
    public void testGetDurationDirect() throws ParseException
    {
        assertEquals( "2 Seconds", DateUtil.getDuration( 2000 ) );

        assertEquals( "12 Minutes 12 Seconds 234 Milliseconds", DateUtil.getDuration( 732234 ) );
        
        assertEquals( "12 Minutes 501 Milliseconds", DateUtil.getDuration( 720501 ) );
    }
}
