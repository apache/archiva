package org.apache.archiva.metadata.repository.stats.model;

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

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class DefaultRepositoryStatisticsTest
{
    @Test
    public void toProperties( ) throws Exception
    {
        DefaultRepositoryStatistics stats = new DefaultRepositoryStatistics();
        Date startTime = new Date();
        Date endTime = new Date();
        stats.setScanStartTime( startTime );
        stats.setScanEndTime( endTime );
        stats.setTotalFileCount( 500 );
        stats.setNewFileCount( 10 );
        stats.setRepositoryId( "test-repo" );
        stats.setTotalArtifactCount( 300 );
        stats.setTotalArtifactFileSize( 4848484 );
        stats.setTotalGroupCount( 4 );
        stats.setTotalProjectCount( 6 );
        stats.setCustomValue( "test.value.1", 55 );
        stats.setCustomValue( "test.value.2", 44);
        stats.setTotalCountForType( "java-source",13 );
        stats.setTotalCountForType( "pom", 5 );

        Map<String, String> props = stats.toProperties( );

        assertEquals( "500", props.get("totalFileCount") );
        assertEquals( "10", props.get("newFileCount"));
        assertEquals( "300", props.get("totalArtifactCount"));
        assertEquals( "4848484", props.get("totalArtifactFileSize"));
        assertEquals("4", props.get("totalGroupCount"));
        assertEquals("6", props.get("totalProjectCount"));
        assertEquals("55",props.get("count-custom-test.value.1" ));
        assertEquals("44", props.get("count-custom-test.value.2"));
        assertEquals("13", props.get("count-type-java-source"));
        assertEquals("5", props.get("count-type-pom"));
        assertEquals( String.valueOf(startTime.getTime()), props.get("scanStartTime"));
        assertEquals( String.valueOf(endTime.getTime()), props.get("scanEndTime"));
    }

    @Test
    public void fromProperties( ) throws Exception
    {
        DefaultRepositoryStatistics stats = new DefaultRepositoryStatistics( );
        Date startTime = new Date();
        Date endTime = new Date();
        Map<String,String> props = new HashMap<>(  );
        props.put("totalFileCount","501");
        props.put("newFileCount","11");
        props.put("totalArtifactCount","301");
        props.put("totalArtifactFileSize","473565557");
        props.put("totalGroupCount","5");
        props.put("totalProjectCount","7");
        props.put("count-custom-test.value.1","56");
        props.put("count-custom-test.value.2","45");
        props.put("count-type-java-source","14");
        props.put("count-type-pom","6");
        props.put("scanStartTime", String.valueOf(startTime.getTime()));
        props.put("scanEndTime", String.valueOf(endTime.getTime()));

        stats.fromProperties( props );

        assertEquals(501,stats.getTotalFileCount());
        assertEquals(11,stats.getNewFileCount());
        assertEquals(301, stats.getTotalArtifactCount());
        assertEquals(473565557, stats.getTotalArtifactFileSize());
        assertEquals(5, stats.getTotalGroupCount());
        assertEquals(7, stats.getTotalProjectCount());
        assertEquals(56, stats.getCustomValue( "test.value.1" ));
        assertEquals(45, stats.getCustomValue( "test.value.2" ));
        assertEquals(14, stats.getTotalCountForType( "java-source" ));
        assertEquals(6, stats.getTotalCountForType( "pom" ));
        assertEquals(startTime, stats.getScanStartTime());
        assertEquals( endTime, stats.getScanEndTime() );
    }



}