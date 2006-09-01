package org.apache.maven.archiva.digest;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import junit.framework.TestCase;

public class DigestUtilsTest
    extends TestCase
{
    public void testCleanChecksum()
        throws DigesterException
    {
        // SHA1 checksum from www.ibiblio.org/maven2, incuding file path
        DigestUtils.cleanChecksum(
            "bcc82975c0f9c681fcb01cc38504c992553e93ba  /home/projects/maven/repository-staging/to-ibiblio/maven2/servletapi/servletapi/2.4/servletapi-2.4.pom",
            "SHA1", "servletapi/servletapi/2.4/servletapi-2.4.pom" );

        DigestUtils.cleanChecksum(
            "SHA1(/home/projects/maven/repository-staging/to-ibiblio/maven2/servletapi/servletapi/2.4/servletapi-2.4.pom)=bcc82975c0f9c681fcb01cc38504c992553e93ba",
            "SHA1", "servletapi/servletapi/2.4/servletapi-2.4.pom" );
    }
}
