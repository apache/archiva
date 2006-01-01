package org.apache.maven.repository.digest;

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

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;

/**
 * Create a digest for a file.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface Digester
{
    String ROLE = Digester.class.getName();

    String SHA1 = "SHA-1";

    String MD5 = "MD5";

    String createChecksum( File file, String algorithm )
        throws FileNotFoundException, IOException, NoSuchAlgorithmException;

    boolean verifyChecksum( File file, String checksum, String algorithm )
        throws NoSuchAlgorithmException, IOException;
}
