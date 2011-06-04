package org.apache.maven.archiva.webdav.util;

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

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

/**
 * MimeTypesTest
 *
 * @version $Id: MimeTypesTest.java 6556 2007-06-20 20:44:46Z joakime $
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class MimeTypesTest
    extends TestCase
{
    @Inject
    MimeTypes mime;

    @Test
    public void testGetMimeType()
        throws Exception
    {
        assertNotNull( "MimeTypes should not be null.", mime );

        assertEquals( "application/pdf", mime.getMimeType( "big-book.pdf" ) );
        assertEquals( "application/octet-stream", mime.getMimeType( "BookMaker.class" ) );
        assertEquals( "application/vnd.ms-powerpoint", mime.getMimeType( "TypeSetting.ppt" ) );
        assertEquals( "application/java-archive", mime.getMimeType( "BookViewer.jar" ) );
    }
}
