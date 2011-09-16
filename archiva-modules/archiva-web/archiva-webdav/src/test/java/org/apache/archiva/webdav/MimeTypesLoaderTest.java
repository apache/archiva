package org.apache.archiva.webdav;

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

import junit.framework.TestCase;
import org.apache.archiva.webdav.util.MimeTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

/**
 * ArchivaMimeTypesTest 
 *
 * @version $Id$
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class MimeTypesLoaderTest
    extends TestCase
{

    @Inject
    MimeTypes mimeTypes;

    @Test
    public void testArchivaTypes()
        throws Exception
    {
        assertNotNull( mimeTypes );

        // Test for some added types.
        assertEquals( "sha1", "text/plain", mimeTypes.getMimeType( "foo.sha1" ) );
        assertEquals( "md5", "text/plain", mimeTypes.getMimeType( "foo.md5" ) );
        assertEquals( "pgp", "application/pgp-encrypted", mimeTypes.getMimeType( "foo.pgp" ) );
        assertEquals( "jar", "application/java-archive", mimeTypes.getMimeType( "foo.jar" ) );
        assertEquals( "Default", "application/octet-stream", mimeTypes.getMimeType(".SomeUnknownExtension"));
    }
}
