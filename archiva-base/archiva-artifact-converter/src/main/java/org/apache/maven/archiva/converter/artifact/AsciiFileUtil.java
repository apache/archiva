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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * AsciiFileUtil - conveinence utility for reading / writing ascii files.
 * 
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @todo switch to commons-lang and use their high-performance versions of these utility methods.
 */
public class AsciiFileUtil
{
    /**
     * Read a file into a {@link String} and return it.
     * 
     * @param file the file to read
     * @return the {@link String} contents of the file.
     * @throws IOException if there was a problem performing this operation.
     */
    public static String readFile( File file )
        throws IOException
    {
        return FileUtils.readFileToString( file, null );
    }

    /**
     * Write the contents of a {@link String} to a file.
     *  
     * @param file the file to write to
     * @param content the {@link String} contents to write.
     * @throws IOException if there was a problem performing this operation.
     */
    public static void writeFile( File file, String content )
        throws IOException
    {
        FileUtils.writeStringToFile( file, content, null );
    }
}
